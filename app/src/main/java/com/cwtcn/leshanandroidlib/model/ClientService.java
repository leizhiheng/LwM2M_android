package com.cwtcn.leshanandroidlib.model;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

import com.cwtcn.leshanandroidlib.constant.ServerConfig;
import com.cwtcn.leshanandroidlib.resources.AddressableTextDisplay;
import com.cwtcn.leshanandroidlib.resources.ContactList;
import com.cwtcn.leshanandroidlib.resources.ExtendBaseInstanceEnabler;
import com.cwtcn.leshanandroidlib.resources.ExtendObjectsInitializer;
import com.cwtcn.leshanandroidlib.resources.IlluminanceSensor;
import com.cwtcn.leshanandroidlib.resources.MyDevice;
import com.cwtcn.leshanandroidlib.resources.MyLocation;
import com.cwtcn.leshanandroidlib.resources.NoDisturbMode;
import com.cwtcn.leshanandroidlib.resources.RandomTemperatureSensor;
import com.cwtcn.leshanandroidlib.resources.SetPoint;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnWriteReadListener;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.leshan.LwM2mId.DEVICE;
import static org.eclipse.leshan.LwM2mId.LOCATION;
import static org.eclipse.leshan.LwM2mId.SECURITY;
import static org.eclipse.leshan.LwM2mId.SERVER;
import static org.eclipse.leshan.client.object.Security.noSec;
import static org.eclipse.leshan.client.object.Security.noSecBootstap;
import static org.eclipse.leshan.client.object.Security.psk;
import static org.eclipse.leshan.client.object.Security.pskBootstrap;

public class ClientService extends Service implements IClientModel, OnWriteReadListener {
    public static final String TAG = "ClientService";

    public static final int SERVER_ID_LOCAL = 0;
    public static final int SERVER_ID_REMOTE = 1;

    /*添加一个Object第1步：添加model文件*/
    private final static String[] modelPaths = new String[]{
            "3301.xml", "3303.xml", "3308.xml",
            "3341.xml", "9000.xml", "9001.xml"};
    /*添加一个Object第2步：添加Object对应的类*/
    private final static Map<Integer, Class> objectClasses;
    static {
        //cus-表示自定义Object;oma-表示官方定义的Object
        objectClasses = new HashMap<Integer, Class>();
        objectClasses.put(LOCATION, MyLocation.class);//Location-oma
        objectClasses.put(3301, IlluminanceSensor.class);//Illuminance-oma
        objectClasses.put(3303, RandomTemperatureSensor.class);//Temperature-oma
        objectClasses.put(3308, SetPoint.class);//Set Point-oma
        objectClasses.put(3341, AddressableTextDisplay.class);//Addressable Text Display-oma
        objectClasses.put(9000, ContactList.class);//ContactList-cus
        objectClasses.put(9001, NoDisturbMode.class);//NoDisturbMode-cus
    }
    private final static Map<Integer, ExtendBaseInstanceEnabler> baseInstances = new HashMap<Integer, ExtendBaseInstanceEnabler>();

    private Context mContext;
    private ObjectsInitializer initializer;
    private LeshanClient mClient;
    private LwM2mClientObserver mObserver;
    private MyDevice mDevice;

    private String mRegistrationId;

    public static SharedPreferences mPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        DebugLog.d("ClientService.onCreate ==>");
        mPreferences = getSharedPreferences(ServerConfig.NOTIFY_PERIOD_PREFERENCES, Context.MODE_PRIVATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        DebugLog.d("ClientService.onBind ==>");
        return new LeshanBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        DebugLog.d("ClientService.onUnbind ==>");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        DebugLog.d("ClientService.onDestroy ==>");
        super.onDestroy();
    }

    public class LeshanBinder extends Binder {
        public ClientService getService() {
            return ClientService.this;
        }
    }

    @Override
    public void register(final int serverId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                checkParams(serverId);
            }
        }).start();
    }

    @Override
    public void destroy() {
        new StopClientTask().execute();
    }

    public void checkParams(int serverId) {
        /**---------------------本地服务器设置----------------*/
//        if (serverId == SERVER_ID_LOCAL) {
            String endpoint = "Phone-Blue-Client";

            // Get server URI
            String serverURI = "coap://10.0.2.2:5484"; //+ LwM2m.DEFAULT_COAP_PORT;

            // get security info
            byte[] pskIdentity = null;
            byte[] pskKey = null;

            // get local address
            String localAddress = null;
            int localPort = 0;

            // get secure local address
            String secureLocalAddress = null;
            int secureLocalPort = 0;

            Float latitude = null;
            Float longitude = null;
            Float scaleFactor = 1.0f;
//
//            createAndStartClient(endpoint, localAddress, localPort, secureLocalAddress, secureLocalPort, false,
//                    serverURI, pskIdentity, pskKey, latitude, longitude, scaleFactor);
//        } else if (serverId == SERVER_ID_REMOTE) {

            /**--------------爱立信服务器设置-------------*/
//            String endpoint = ServerConfig.END_POINT;
//
//            // Get server URI
//            String serverURI = ServerConfig.SERVER_URI;
//
//            // get security info
//            byte[] pskIdentity = ServerConfig.PSK_IDENTITY.getBytes();
//            byte[] pskKey = Hex.decodeHex(ServerConfig.PSK_KEY.toCharArray());
//
//            // get local address
//            String localAddress = null;
//            int localPort = 0;
//
//            // get secure local address
//            String secureLocalAddress = null;
//            int secureLocalPort = 0;
//
//            Float latitude = null;
//            Float longitude = null;
//            Float scaleFactor = 1.0f;
            createAndStartClient(endpoint, localAddress, localPort, secureLocalAddress, secureLocalPort, false,
                    serverURI, pskIdentity, pskKey, latitude, longitude, scaleFactor);
//        }
    }

    public void createAndStartClient(String endpoint, String localAddress, int localPort,
                                     String secureLocalAddress, int secureLocalPort, boolean needBootstrap, String serverURI, byte[] pskIdentity,
                                     byte[] pskKey, Float latitude, Float longitude, float scaleFactor) {

        // Initialize model
        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/assets", modelPaths));

        // Initialize object list
        initializer = new ExtendObjectsInitializer(new LwM2mModel(models));
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
        List<LwM2mObjectEnabler> enablers = initializer.create(SECURITY, SERVER, DEVICE);
        //设置其他的Object
        List<LwM2mObjectEnabler> enablersMore = setInstancesForObject();
        enablers.addAll(enablersMore);


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

    private List<LwM2mObjectEnabler> setInstancesForObject() {
        List<LwM2mObjectEnabler> enablers = new ArrayList<LwM2mObjectEnabler>();
        for (int objectId:objectClasses.keySet()) {
            try {
                ExtendBaseInstanceEnabler baseInstance = (ExtendBaseInstanceEnabler) objectClasses.get(objectId).newInstance();
                baseInstance.setContext(mContext);
                baseInstance.setObjectId(objectId);
                baseInstance.setOnWriteNotifyPeriodListener(this);
                baseInstances.put(objectId, baseInstance);
                /*添加一个Object第3步：创建实例，并添加到initializer中*/
                initializer.setInstancesForObject(objectId, baseInstance);
                enablers.add(initializer.create(objectId));
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return enablers;
    }

    @Override
    public void updateResource(int objectId, ResourceBean bean, String newValue) {
        switch (objectId) {
            case LOCATION:
                MyLocation location = (MyLocation) baseInstances.get(objectId);
                location.updateLocation(bean.id, Float.valueOf(newValue));
                break;
            case RandomTemperatureSensor.OBJECT_ID_TEMPERATURE_SENSOR:
                RandomTemperatureSensor sensor = (RandomTemperatureSensor) baseInstances.get(objectId);
                sensor.adjustTemperature(Double.valueOf(newValue));
                break;
        }
    }

    public void setObserver(LwM2mClientObserver observer) {
        this.mObserver = observer;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    @Override
    public boolean isClientStarted() {
        return mClient != null;
    }

    @Override
    public void setRegistrationId(String registrationId) {
        this.mRegistrationId = registrationId;
    }

    @Override
    public String getRegistrationId() {
        return mRegistrationId;
    }


    /**
     * 当服务端传递过来数据上报周期信息时，在这处理
     * @param objectId
     * @param period
     */
    @Override
    public void writedPeriod(int objectId, int period) {
        Map<Integer, LwM2mInstanceEnabler[]> instances = initializer.getInstances();
        if (instances.containsKey(objectId)) {
            //保存period
            setIntervalInSec(objectId, (int) period);
            LwM2mInstanceEnabler[] enablers = instances.get(objectId);
            for (LwM2mInstanceEnabler enabler: enablers) {
                if (enabler instanceof ExtendBaseInstanceEnabler) {
                    //将period的值设置给所有Instance,当Server observe这个实例的某个资源后，
                    // 这个实例会每隔period秒，上报一次resource的值
                    ((ExtendBaseInstanceEnabler) enabler).setIntervalInSec((int) period);
                }
            }
        }

        setIntervalInSec(objectId, period);
    }

    @Override
    public int readPeriod(int objectId) {
        return getIntervalInSec(objectId);
    }

    @Override
    public void setStringToPreference(String key, String value) {
        mPreferences.edit().putString(key, value).commit();
    }

    @Override
    public String getStringFromPreferemce(String key) {
        return mPreferences.getString(NoDisturbMode.KEY_NO_DISTURB_MODE_MSG, null);
    }

    public void setIntervalInSec(int objectId, int period) {
        mPreferences.edit().putInt(String.valueOf(objectId), period).commit();
    }

    public int getIntervalInSec(int objectId) {
        return mPreferences.getInt(String.valueOf(objectId), -1);
    }

    /**
     * 停止Object的Instance的周期上报线程
     */
    private void stopInstanceNotifyThread() {
        Map<Integer, LwM2mInstanceEnabler[]> instances = initializer.getInstances();
        for (Integer objectId: instances.keySet()) {
            LwM2mInstanceEnabler[] enablers = instances.get(objectId);
            for (LwM2mInstanceEnabler enabler: enablers) {
//                DebugLog.d("stopInstanceNotifyThread objectId = " + objectId + ", enabler = " + enabler);
                if (enabler instanceof ExtendBaseInstanceEnabler) {
                    ExtendBaseInstanceEnabler e = (ExtendBaseInstanceEnabler) enabler;
                    e.setStartedObseve(false);
                }
            }
        }
    }

    class StopClientTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            stopInstanceNotifyThread();

            if (mClient != null) {
                mClient.destroy(true);
                mClient = null;
            }
            return null;
        }
    }

    /**
     * 用于测试读取califonium.properties文件是否成功
     *
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
}
