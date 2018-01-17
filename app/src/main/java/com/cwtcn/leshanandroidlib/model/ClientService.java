package com.cwtcn.leshanandroidlib.model;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

import com.cwtcn.leshanandroidlib.constant.ServerConfig;
import com.cwtcn.leshanandroidlib.resources.IlluminanceSensor;
import com.cwtcn.leshanandroidlib.resources.MyDevice;
import com.cwtcn.leshanandroidlib.resources.MyLocation;
import com.cwtcn.leshanandroidlib.resources.RandomTemperatureSensor;
import com.cwtcn.leshanandroidlib.utils.DebugLog;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.util.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.eclipse.leshan.LwM2mId.DEVICE;
import static org.eclipse.leshan.LwM2mId.LOCATION;
import static org.eclipse.leshan.LwM2mId.SECURITY;
import static org.eclipse.leshan.LwM2mId.SERVER;
import static org.eclipse.leshan.client.object.Security.noSec;
import static org.eclipse.leshan.client.object.Security.noSecBootstap;
import static org.eclipse.leshan.client.object.Security.psk;
import static org.eclipse.leshan.client.object.Security.pskBootstrap;

public class ClientService extends Service implements IClientModel{
    public static final String TAG = "ClientService";

    private final static String[] modelPaths = new String[] { "3301.xml", "3303.xml" };
    private static final int OBJECT_ID_TEMPERATURE_SENSOR = 3303;
    private final static String DEFAULT_ENDPOINT = "LeshanClientDemo";
    private final static String USAGE = "java -jar leshan-client-demo.jar [OPTION]";

    private Context mContext;
    private MyLocation locationInstance;
    private LeshanClient mClient;
    private LwM2mClientObserver mObserver;
    private MyDevice mDevice;
    private MyLocation mLocation, mLocationTemp;
    private RandomTemperatureSensor mTemperature;
    private IlluminanceSensor mIllumunance;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LeshanBinder();
    }

    @Override
    public void register() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                checkParams();
            }
        }).start();
    }

    @Override
    public void destroy() {
        new StopClientTask().execute();
    }

    @Override
    public void updateResource(int objectId, ResourceBean bean, String newValue) {
        switch (objectId) {
            case LOCATION:
                mLocation.updateLocation(bean.id, Float.valueOf(newValue));
                break;
            case OBJECT_ID_TEMPERATURE_SENSOR:
                mTemperature.adjustTemperature(Double.valueOf(newValue));
                break;
        }
    }

    @Override
    public boolean isClientStarted() {
        return mClient != null;
    }

    public class LeshanBinder extends Binder {
        public ClientService getService() {
            return new ClientService();
        }
    }

    public void checkParams() {

        /**---------------------本地服务器设置----------------*/
//        String endpoint = "Phone-Blue-Client";
//
//        // Get server URI
//        String serverURI  = "coap://10.0.2.2:5484"; //+ LwM2m.DEFAULT_COAP_PORT;
//
//        // get security info
//        byte[] pskIdentity = null;
//        byte[] pskKey = null;
//
//        // get local address
//        String localAddress = null;
//        int localPort = 0;
//
//        // get secure local address
//        String secureLocalAddress = null;
//        int secureLocalPort = 0;
//
//        Float latitude = null;
//        Float longitude = null;
//        Float scaleFactor = 1.0f;

        /**--------------爱立信服务器设置-------------*/
        String endpoint = ServerConfig.END_POINT;

        // Get server URI
        String serverURI = ServerConfig.SERVER_URI;

        // get security info
        byte[] pskIdentity = ServerConfig.PSK_IDENTITY.getBytes();
        byte[] pskKey = Hex.decodeHex(ServerConfig.PSK_KEY.toCharArray());

        // get local address
        String localAddress = null;
        int localPort = 0;

        // get secure local address
        String secureLocalAddress = null;
        int secureLocalPort = 0;

        Float latitude = null;
        Float longitude = null;
        Float scaleFactor = 1.0f;

        createAndStartClient(endpoint, localAddress, localPort, secureLocalAddress, secureLocalPort, false,
                serverURI, pskIdentity, pskKey, latitude, longitude, scaleFactor);
    }

    public void createAndStartClient(String endpoint, String localAddress, int localPort,
                                     String secureLocalAddress, int secureLocalPort, boolean needBootstrap, String serverURI, byte[] pskIdentity,
                                     byte[] pskKey, Float latitude, Float longitude, float scaleFactor) {
        mLocation = new MyLocation(latitude, longitude, scaleFactor);
        mLocationTemp = new MyLocation(latitude, longitude, scaleFactor);
        mIllumunance = new IlluminanceSensor();
        mTemperature = new RandomTemperatureSensor();

        // Initialize model
        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/assets", modelPaths));

        // Initialize object list
        ObjectsInitializer initializer = new ObjectsInitializer(new LwM2mModel(models));
        if (needBootstrap) {
            if (pskIdentity == null)
                initializer.setInstancesForObject(SECURITY, noSecBootstap(serverURI));
            else
                initializer.setInstancesForObject(SECURITY, pskBootstrap(serverURI, pskIdentity, pskKey));
        } else {
            if (pskIdentity == null) {
                initializer.setInstancesForObject(SECURITY, noSec(serverURI, 123));
                initializer.setInstancesForObject(SERVER, new Server(123, 30, BindingMode.U, false));
            } else {
                initializer.setInstancesForObject(SECURITY, psk(serverURI, 123, pskIdentity, pskKey));
                initializer.setInstancesForObject(SERVER, new Server(123, 30, BindingMode.U, false));
            }
        }
        initializer.setClassForObject(DEVICE, MyDevice.class);
        //LOCATION属于单实例对象，所以只能设置一个实例，否则报错
        initializer.setInstancesForObject(LOCATION, mLocation/*, mLocationTemp*/);
        initializer.setInstancesForObject(OBJECT_ID_TEMPERATURE_SENSOR, mTemperature);
        initializer.setInstancesForObject(IlluminanceSensor.OJBECTS_ID_ILLUMINANCE, mIllumunance);
        List<LwM2mObjectEnabler> enablers = initializer.create(SECURITY, SERVER, DEVICE, LOCATION, IlluminanceSensor.OJBECTS_ID_ILLUMINANCE,
                OBJECT_ID_TEMPERATURE_SENSOR);

        // Create CoAP Config
        NetworkConfig coapConfig = null;
        try {
            InputStream inputStream = mContext.getResources().getAssets().open(NetworkConfig.DEFAULT_FILE_NAME);
            System.out.println("leshan.LeshanClientDemo.createAndStartClient inputStream = " + inputStream);
            //readProperties(inputStream);
//            System.out.println("leshan.LeshanClientDemo.createAndStartClient configFile.isFile(): " + configFile.isFile());
//            if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(inputStream);
//            } else {
//                coapConfig = LeshanClientBuilder.createDefaultNetworkConfig();
//                coapConfig.store(configFile);
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setLocalAddress(localAddress, localPort);
        builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
        builder.setObjects(enablers);
        builder.setCoapConfig(coapConfig);
        // if we don't use bootstrap, client will always use the same unique endpoint
        // so we can disable the other one.
        if (!needBootstrap) {
            if (pskIdentity == null)
                builder.disableSecuredEndpoint();
            else
                builder.disableUnsecuredEndpoint();
        }
        mClient = builder.build();
        mClient.addObserver(mObserver);

        // Start the client
        mClient.start();
    }

    /**
     * 用于测试读取califonium.properties文件是否成功
     * @param inputStream
     */
    private void readProperties(InputStream inputStream) {
        int count = 0;
        InputStreamReader reader = new InputStreamReader(inputStream);
        char[] buf = new char[2048];
        try {
            int n = reader.read(buf, count, 1024);
            if (n > 0) {
                count += n;
            }
            DebugLog.d(new String(buf));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setObserver(LwM2mClientObserver observer) {
        this.mObserver = observer;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    class StopClientTask extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... params) {
            if (mClient != null) {
                mClient.destroy(true);
                mClient = null;
            }
            return null;
        }
    }
}
