package com.cwtcn.leshanandroidlib.model;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.cwtcn.leshanandroidlib.constant.ServerConfig;
import com.cwtcn.leshanandroidlib.resources.BatteryStatus;
import com.cwtcn.leshanandroidlib.resources.ContactList;
import com.cwtcn.leshanandroidlib.resources.ExtendBaseInstanceEnabler;
import com.cwtcn.leshanandroidlib.resources.ExtendObjectsInitializer;
import com.cwtcn.leshanandroidlib.resources.MyDevice;
import com.cwtcn.leshanandroidlib.resources.MyLocation;
import com.cwtcn.leshanandroidlib.resources.NoDisturbMode;
import com.cwtcn.leshanandroidlib.resources.RandomTemperatureSensor;
import com.cwtcn.leshanandroidlib.resources.SetPoint;
import com.cwtcn.leshanandroidlib.resources.SosAlert;
import com.cwtcn.leshanandroidlib.resources.WhiteList;
import com.cwtcn.leshanandroidlib.utils.ContactListUtils;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnLocateResultListener;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnOperationResultListener;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnWriteReadListener;
import com.cwtcn.leshanandroidlib.utils.locationutils.GaodeLBSUtils;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.util.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
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

public class ClientService extends Service implements IClientModel, OnWriteReadListener, LwM2mClientObserver, OnLocateResultListener {
    public static final String TAG = "ClientService";

    /*添加一个Object第1步：添加model文件*/
    private final static String[] modelPaths = new String[]{
            "3301.xml", "3303.xml", "3308.xml",
            "3341.xml", "9000.xml", "9001.xml",
            "9002.xml", "9003.xml", "9004.xml"};
    /*添加一个Object第2步：添加Object对应的类*/
    private final static Map<Integer, Class> objectClasses;

    static {
        //cus-表示自定义Object;oma-表示官方定义的Object
        objectClasses = new HashMap<Integer, Class>();
        //objectClasses.put(3301, IlluminanceSensor.class);//Illuminance-oma
        objectClasses.put(3303, RandomTemperatureSensor.class);//Temperature-oma
        objectClasses.put(3308, SetPoint.class);//Set Point-oma
        //objectClasses.put(3341, AddressableTextDisplay.class);//Addressable Text Display-oma
        objectClasses.put(9000, ContactList.class);//ContactList-cus
        objectClasses.put(9001, NoDisturbMode.class);//NoDisturbMode-cus
        objectClasses.put(9002, BatteryStatus.class);//BatteryStatus-cus
        objectClasses.put(9003, SosAlert.class);//SosAlert-cus
        objectClasses.put(9004, WhiteList.class);//WhiteList-cus
    }

    private final static Map<Integer, ExtendBaseInstanceEnabler> baseInstances = new HashMap<Integer, ExtendBaseInstanceEnabler>();

    private Context mContext;
    private ObjectsInitializer initializer;
    private LeshanClient mClient;
    private MyLocation mLocation;
    private MyDevice mDevice;

    private String mRegistrationId;
    /**
     * 客户端与服务器是否是异常断开连接，比如断网后导致连接断开。
     */
    private boolean mIsDisconnectedAbnormal = false;

    public static SharedPreferences mPreferences;
    /**
     * 高德定位工具类实例
     */
    public GaodeLBSUtils mGaodeLbsUtils;
    /**
     * 要求获取定位信息的ExtendBaseInstanceEnabler实例。可能有多个ExtendBaseInstanceEnabler的实例请求定位信息，所以使用List
     */
    public Map<Integer, ExtendBaseInstanceEnabler> mRequestLocationEablers;

    private OnOperationResultListener mOnOperationResultListener;

    public void setOnOperationResultListener(OnOperationResultListener listener) {
        this.mOnOperationResultListener = listener;
    }

    public static final int MSG_WHAT_START_OPTERATE = 1000;
    public static final int MSG_WHAT_REQUEST_LOCATION = 1001;
    public static final int MSG_WHAT_NETWORK_IS_AVAILABLE = 1002;
    public static final int MSG_WHAT_NETWORK_IS_NOT_AVAILABLE = 1003;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int what = msg.what;
            Bundle data = msg.getData();

            String resultMsg = "";
            DebugLog.d("handler message what:" + what);
            switch (what) {
                case MSG_WHAT_REQUEST_LOCATION:
                    //收到消息开始定位
                    mGaodeLbsUtils.startLoc();
                    return true;
                case MSG_WHAT_START_OPTERATE:
                    //Client向Server提交了一次请求
                    mOnOperationResultListener.onStartOperate();
                    return true;
                case MSG_WHAT_NETWORK_IS_AVAILABLE:
                    //网络改变：网络可用
                    //如果之前是非正常断开连接，则尝试重新连接
                    //showToast("Network status changed, isAvailable:" + true + ", mRegistrationId = " + mRegistrationId);
                    DebugLog.d("Network status changed, isAvailable:" + true + ", mRegistrationId = " + mRegistrationId);
                    if (mIsDisconnectedAbnormal) {
                        register();
                    }
                    return true;
                case MSG_WHAT_NETWORK_IS_NOT_AVAILABLE:
                    //网络改变：网络不可用
                    //如果之前已经注册，则直接将mRegistrationId设置为null,表示连接已断开
                    //showToast("Network status changed, isAvailable:" + false + ", mRegistrationId = " + mRegistrationId);
                    DebugLog.d("Network status changed, isAvailable:" + false + ", mRegistrationId = " + mRegistrationId);
                    if (!TextUtils.isEmpty(mRegistrationId)) {
                        mIsDisconnectedAbnormal = true;
                        mRegistrationId = null;
                        resultMsg = "Disconnected from server abnormally!";
                    }
                    break;

                /*
                 * 引导服务器
                 */
                case ServerConfig.REQUEST_RESULT_BOOTSTRAP_SUCCESS:
                    break;
                case ServerConfig.REQUEST_RESULT_BOOTSTRAP_FAILURE:
                    break;
                case ServerConfig.REQUEST_RESULT_BOOTSTRAP_TIMEOUT:
                    break;
                /*
                 * 注册
                 */
                case ServerConfig.REQUEST_RESULT_REGISTRATION_SUCCESS:
                    mRegistrationId = data.getString("registrationId");
                    resultMsg = mRegistrationId;
                    mIsDisconnectedAbnormal = false;
                    break;
                case ServerConfig.REQUEST_RESULT_REGISTRATION_FAILURE:
                case ServerConfig.REQUEST_RESULT_REGISTRATION_TIMEOUT:
                    break;
                /*
                 * 更新
                 */
                case ServerConfig.REQUEST_RESULT_UPDATE_SUCCESS:
                    mIsDisconnectedAbnormal = false;
                    break;
                case ServerConfig.REQUEST_RESULT_UPDATE_FAILURE:
                case ServerConfig.REQUEST_RESULT_UPDATE_TIMEOUT:
                    resultMsg = "Disconnected from server abnormally!";
                    mRegistrationId = null;
                    break;
                /*
                 * 注销
                 */
                case ServerConfig.REQUEST_RESULT_DEREGISTRATION_SUCCUSS:
                case ServerConfig.REQUEST_RESULT_DEREGISTRATION_FAILURE:
                case ServerConfig.REQUEST_RESULT_DEREGISTRATION_TIMEOUT:
                    mRegistrationId = null;
                    mClient = null;
                    break;
            }
            mOnOperationResultListener.onOperateResult(what, resultMsg);
            return false;
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        DebugLog.d("ClientService.onCreate ==>");
        mPreferences = getSharedPreferences(ServerConfig.NOTIFY_PERIOD_PREFERENCES, Context.MODE_PRIVATE);
        registerReceiver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        DebugLog.d("ClientService.onBind ==>");
        return new LeshanBinder();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.net.conn.CONNECTIVITY_CHANGE") || action.equals("android.net.wifi.WIFI_STATE_CHANGED") || action.equals("android.net.wifi.STATE_CHANGE")) {
                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkinfo = connMgr.getActiveNetworkInfo();

                if (networkinfo == null || !networkinfo.isAvailable()) {
                    //如果是短暂地断开网络，则不处理这个消息，所以延迟5秒发送
                    mHandler.removeMessages(MSG_WHAT_NETWORK_IS_AVAILABLE);
                    mHandler.sendEmptyMessageDelayed(MSG_WHAT_NETWORK_IS_NOT_AVAILABLE, 2000);
                } else {
                    mHandler.removeMessages(MSG_WHAT_NETWORK_IS_NOT_AVAILABLE);
                    mHandler.sendEmptyMessageDelayed(MSG_WHAT_NETWORK_IS_AVAILABLE, 2000);
                }
            } else if (action.equals("android.intent.action.PHONE_STATE")) {
                //获取白名单列表
                ContactListUtils utils = new ContactListUtils(mContext);
                List<ContactListUtils.ContactBean> whiteLists = utils.getContacts(ContactsContract.Data.CONTENT_URI,
                        ContactsContract.CommonDataKinds.Phone.DATA4 + "=? and " + ContactsContract.CommonDataKinds.Phone.DATA2 + "=?", new String[] {"1", "2"});
                DebugLog.d("whitelist：" + utils.contacts2array(whiteLists).toString());
                //判断来电号码是否属于白名单
                String phone = intent.getStringExtra("incoming_number");
                boolean isContain = false;
                for (ContactListUtils.ContactBean bean: whiteLists) {
                    if (phone.equals(bean.mobile)) {
                        isContain = true;
                        break;
                    }
                }
                DebugLog.d("incall phone number:" + phone + ", is white list:" + isContain);
                //Toast.makeText(mContext, "Call state ringing, number:" + phone, Toast.LENGTH_LONG).show();
                //如果来电号码不属于白名单，则拦截来电
                if (!isContain) {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
                    int state = telephonyManager.getCallState();
                    switch (state) {
                        //来电状态
                        case TelephonyManager.CALL_STATE_IDLE:
                        case TelephonyManager.CALL_STATE_RINGING:
                            //得到TelephonyManager的Class对象
                            Class<TelephonyManager> telephonyManagerClass = TelephonyManager.class;
                            try {
                                //得到TelephonyManager.getITelephony方法的Method对象
                                Method method = telephonyManagerClass.getDeclaredMethod("getITelephony", new Class[0]);
                                //允许访问私有方法
                                method.setAccessible(true);
                                //调用getITelephony方法发挥ITelephony对象
                                ITelephony telephony = (ITelephony) method.invoke(telephonyManager, new Object[0]);
                                //挂断电话
                                telephony.endCall();
                            } catch (Exception e) {
                                //Toast.makeText(mContext, "end call failed, e:" + e.getMessage(), Toast.LENGTH_LONG).show();
                                DebugLog.d("end call failed, error message:" + e.getMessage());
                                e.printStackTrace();
                            }
                            break;
                        //通话状态
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            break;
                        //挂断状态
//                        case TelephonyManager.CALL_STATE_IDLE:
//                            break;
                    }
                }
            }
        }
    };

    private IntentFilter mFilter;
    private void registerReceiver() {
        mFilter = new IntentFilter();
        mFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        mFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        mFilter.addAction("android.net.wifi.STATE_CHANGE");
        //监听电话状态
        mFilter.addAction("android.intent.action.PHONE_STATE");
        mContext.registerReceiver(mReceiver, mFilter);
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
        unregisterReceiver(mReceiver);
    }

    public class LeshanBinder extends Binder {
        public ClientService getService() {
            return ClientService.this;
        }
    }

    @Override
    public void register() {
        if (mGaodeLbsUtils == null) {
            mGaodeLbsUtils = new GaodeLBSUtils(this, this);
        }
        if (!TextUtils.isEmpty(mRegistrationId)) {
            mOnOperationResultListener.onOperateReject("Client has already registed!");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(MSG_WHAT_START_OPTERATE);
                checkParams();
            }
        }).start();
    }

    @Override
    public void destroy() {
        if (mGaodeLbsUtils != null) {
            mGaodeLbsUtils.stopLoc();
            mGaodeLbsUtils = null;
        }
        if (TextUtils.isEmpty(mRegistrationId)) {
            mOnOperationResultListener.onOperateReject("Client has already been destoryed");
            return;
        }
        mOnOperationResultListener.onStartOperate();
        new StopClientTask().execute();
    }

    public void checkParams() {
        /**---------------------本地服务器设置----------------*/
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

        mLocation = new MyLocation();
        mLocation.onCreate(mContext.getApplicationContext(), LOCATION, this);

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
        initializer.setInstancesForObject(LOCATION, mLocation);
        List<LwM2mObjectEnabler> enablers = initializer.create(SECURITY, SERVER, DEVICE, LOCATION);
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
        mClient.addObserver(this);

        // Start the client
        mClient.start();
    }

    private List<LwM2mObjectEnabler> setInstancesForObject() {
        List<LwM2mObjectEnabler> enablers = new ArrayList<LwM2mObjectEnabler>();
        for (int objectId : objectClasses.keySet()) {
            try {
                ExtendBaseInstanceEnabler baseInstance = (ExtendBaseInstanceEnabler) objectClasses.get(objectId).newInstance();
                baseInstance.onCreate(mContext.getApplicationContext(), objectId, this);
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

    /**
     * 当服务端传递过来数据上报周期信息时，在这处理
     *
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
            for (LwM2mInstanceEnabler enabler : enablers) {
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
    public void requestLocate(int objectId, ExtendBaseInstanceEnabler requestLocationEnabler) {
        if (mRequestLocationEablers == null) {
            mRequestLocationEablers = new HashMap<Integer, ExtendBaseInstanceEnabler>();
        }
        if (!mRequestLocationEablers.containsKey(objectId)) {
            mRequestLocationEablers.put(objectId, requestLocationEnabler);
        }
        mHandler.sendEmptyMessage(MSG_WHAT_REQUEST_LOCATION);
    }

    /**
     * 定位结果
     *
     * @param isSuccessful 定位是否成功
     * @param lat          经度
     * @param lon          维度
     * @param accuracy     精度。可查看GaodeLBSUtils.java中的方法调用
     */
    @Override
    public void onLocateResult(boolean isSuccessful, double lat, double lon, float accuracy) {
        if (isSuccessful) {
            for (Integer objectId : mRequestLocationEablers.keySet()) {
                mRequestLocationEablers.get(objectId).setLocateResult(lat, lon, accuracy);
            }
            mRequestLocationEablers.clear();
        } else {
            //showToast("定位失败！");
        }
    }

    @Override
    public void setStringToPreference(String key, String value) {
        mPreferences.edit().putString(key, value).commit();
    }

    @Override
    public String getStringFromPreferemce(String key, String defaultValue) {
        return mPreferences.getString(NoDisturbMode.KEY_NO_DISTURB_MODE_MSG, defaultValue);
    }

    public void setIntervalInSec(int objectId, int period) {
        mPreferences.edit().putInt(String.valueOf(objectId), period).commit();
    }

    public int getIntervalInSec(int objectId) {
        return mPreferences.getInt(String.valueOf(objectId), -1);
    }

    /**
     * 停止Object的Instance的周期上报线程.并且调用onDestroy()方法，用于销毁一些不需要的资源
     */
    private void stopInstanceNotifyThread() {
        Map<Integer, LwM2mInstanceEnabler[]> instances = initializer.getInstances();
        for (Integer objectId : instances.keySet()) {
            LwM2mInstanceEnabler[] enablers = instances.get(objectId);
            for (LwM2mInstanceEnabler enabler : enablers) {
//                DebugLog.d("stopInstanceNotifyThread objectId = " + objectId + ", enabler = " + enabler);
                if (enabler instanceof ExtendBaseInstanceEnabler) {
                    ExtendBaseInstanceEnabler e = (ExtendBaseInstanceEnabler) enabler;
                    e.setStartedObseve(false);
                    e.onDestory();
                }
            }
        }
    }

    private void sendMessage(int what, Bundle data) {
        Message message = mHandler.obtainMessage();
        message.what = what;
        message.setData(data);
        mHandler.sendMessage(message);
    }

    @Override
    public void onBootstrapSuccess(ServerInfo serverInfo) {
        sendMessage(ServerConfig.REQUEST_RESULT_BOOTSTRAP_SUCCESS, null);
    }

    @Override
    public void onBootstrapFailure(ServerInfo serverInfo, ResponseCode responseCode, String s) {
        sendMessage(ServerConfig.REQUEST_RESULT_BOOTSTRAP_FAILURE, null);
    }

    @Override
    public void onBootstrapTimeout(ServerInfo serverInfo) {
        sendMessage(ServerConfig.REQUEST_RESULT_BOOTSTRAP_TIMEOUT, null);
    }

    @Override
    public void onRegistrationSuccess(DmServerInfo dmServerInfo, String s) {
        DebugLog.d("onRegistrationSuccess");
        Bundle bundle = new Bundle();
        bundle.putString("registrationId", s);
        sendMessage(ServerConfig.REQUEST_RESULT_REGISTRATION_SUCCESS, bundle);
    }

    @Override
    public void onRegistrationFailure(DmServerInfo dmServerInfo, ResponseCode responseCode, String s) {
        Bundle bundle = new Bundle();
        bundle.putString("responseCode", responseCode.getCode() + "");
        bundle.putString("responseName", responseCode.getName() + "");
        sendMessage(ServerConfig.REQUEST_RESULT_REGISTRATION_FAILURE, bundle);
        DebugLog.d("onRegistrationFailure");
    }

    @Override
    public void onRegistrationTimeout(DmServerInfo dmServerInfo) {
        sendMessage(ServerConfig.REQUEST_RESULT_REGISTRATION_TIMEOUT, null);
    }

    @Override
    public void onUpdateSuccess(DmServerInfo dmServerInfo, String s) {
        sendMessage(ServerConfig.REQUEST_RESULT_UPDATE_SUCCESS, null);
    }

    @Override
    public void onUpdateFailure(DmServerInfo dmServerInfo, ResponseCode responseCode, String s) {
        sendMessage(ServerConfig.REQUEST_RESULT_UPDATE_FAILURE, null);
    }

    @Override
    public void onUpdateTimeout(DmServerInfo dmServerInfo) {
        sendMessage(ServerConfig.REQUEST_RESULT_UPDATE_TIMEOUT, null);
    }

    @Override
    public void onDeregistrationSuccess(DmServerInfo dmServerInfo, String s) {
        sendMessage(ServerConfig.REQUEST_RESULT_DEREGISTRATION_SUCCUSS, null);

    }

    @Override
    public void onDeregistrationFailure(DmServerInfo dmServerInfo, ResponseCode responseCode, String s) {
        sendMessage(ServerConfig.REQUEST_RESULT_DEREGISTRATION_FAILURE, null);
    }

    @Override
    public void onDeregistrationTimeout(DmServerInfo dmServerInfo) {
        sendMessage(ServerConfig.REQUEST_RESULT_DEREGISTRATION_TIMEOUT, null);
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

    private void showToast(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }
}
