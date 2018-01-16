package com.cwtcn.leshanandroidlib.view;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cwtcn.leshanandroidlib.R;
import com.cwtcn.leshanandroidlib.model.ResourceBean;
import com.cwtcn.leshanandroidlib.dialog.ResourceOperateDialog;
import com.cwtcn.leshanandroidlib.resources.RandomTemperatureSensor;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.LocationUtil;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServerInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import static org.eclipse.leshan.LwM2mId.LOCATION;

public class MainActivity extends Activity implements View.OnClickListener, LwM2mClientObserver, AdapterView.OnItemClickListener, LocationUtil.ILocationListener, IMainView {
    public static final String TAG = "MainActivity";

    private static final int PERMISSIONS_REQUEST_LOCATION = 1;
    private Button mStartButton, mStopButton;
    private ListView mListDevice, mListLocation, mListTemperature;
    private TextView mClientStatus;
    private static ArrayList<ResourceBean> mDeviceResources, mLocationResources, mTemperatureResources;
    private ClientService mService;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            hideProgressDialog();
            int what = msg.what;
            Bundle data = msg.getData();
            showTost("Requst result code : " + what);
            switch (what) {
                case ClientService.REQUEST_RESULT_BOOTSTRAP_SUCCESS:
                    break;
                case ClientService.REQUEST_RESULT_BOOTSTRAP_FAILURE:
                    break;
                case ClientService.REQUEST_RESULT_BOOTSTRAP_TIMEOUT:
                    break;
                case ClientService.REQUEST_RESULT_REGISTRATION_SUCCESS:
                    updateClientDevice(true, data.getString("registrationId"));
                    break;
                case ClientService.REQUEST_RESULT_REGISTRATION_FAILURE:
                    break;
                case ClientService.REQUEST_RESULT_REGISTRATION_TIMEOUT:
                    break;
                case ClientService.REQUEST_RESULT_UPDATE_SUCCESS:

                    break;
                case ClientService.REQUEST_RESULT_UPDATE_FAILURE:
                    break;
                case ClientService.REQUEST_RESULT_UPDATE_TIMEOUT:
                    break;
                case ClientService.REQUEST_RESULT_DEREGISTRATION_SUCCUSS:
                    updateClientDevice(false, null);
                    break;
                case ClientService.REQUEST_RESULT_DEREGISTRATION_FAILURE:
                    break;
                case ClientService.REQUEST_RESULT_DEREGISTRATION_TIMEOUT:
                    break;
            }
            return false;
        }
    });

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ClientService.LeshanBinder binder = (ClientService.LeshanBinder) service;
            mService = ((ClientService.LeshanBinder) service).getService();
            mService.setContext(MainActivity.this);
            mService.setObserver(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    static {
        //加载Device的资源
        mDeviceResources = new ArrayList<ResourceBean>();
        mDeviceResources.add(new ResourceBean(21, "Total Memory", null, ResourceBean.ValueType.LONG));

        //加载Location的资源
        mLocationResources = new ArrayList<ResourceBean>();
        mLocationResources.add(new ResourceBean(0, "Latitude", new Random().nextInt(180), ResourceBean.ValueType.DOUBLE));
        mLocationResources.add(new ResourceBean(1, "Longitude", new Random().nextInt(360), ResourceBean.ValueType.DOUBLE));
        mLocationResources.add(new ResourceBean(5, "TimeStamp", new Date(), ResourceBean.ValueType.DATE));

        //加载TemperatureSensor的资源
        mTemperatureResources = new ArrayList<ResourceBean>();
        mTemperatureResources.add(new ResourceBean(RandomTemperatureSensor.MIN_MEASURED_VALUE, "Min Measured Value", -70, ResourceBean.ValueType.DOUBLE));
        mTemperatureResources.add(new ResourceBean(RandomTemperatureSensor.MAX_MEASURED_VALUE, "Max Measured Value", 30, ResourceBean.ValueType.DOUBLE));
        mTemperatureResources.add(new ResourceBean(RandomTemperatureSensor.SENSOR_VALUE, "Sensor Value", -70, ResourceBean.ValueType.DOUBLE));
        mTemperatureResources.add(new ResourceBean(RandomTemperatureSensor.UNITS, "Units", "cel", ResourceBean.ValueType.STRING));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initView();

        //updateClientDevice(false, null);
        startService();
        bindService();
    }

    private void initView() {
        mStartButton = (Button) findViewById(R.id.start_button);
        mStopButton = (Button) findViewById(R.id.stop_button);
        mStartButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);

        mListDevice = findViewById(R.id.resource_list_device);
        mListDevice.setVisibility(View.GONE);
        mListLocation = findViewById(R.id.resource_list_location);
        mListTemperature = findViewById(R.id.resource_list_temperature);
        mListDevice.setAdapter(new ResourceAdapter(mDeviceResources));
        mListLocation.setAdapter(new ResourceAdapter(mLocationResources));
        mListTemperature.setAdapter(new ResourceAdapter(mTemperatureResources));
        mListDevice.setOnItemClickListener(this);
        mListLocation.setOnItemClickListener(this);
        mListTemperature.setOnItemClickListener(this);

        mClientStatus = findViewById(R.id.client_status);
    }

    private void startService() {
        startService(new Intent(this, ClientService.class));
    }

    private void stopService() {
        if (mService != null) mService.stopSelf();
    }

    private void bindService() {
        bindService(new Intent(this, ClientService.class), mConn, Context.BIND_AUTO_CREATE);
    }
    private void unbindService() {
        unbindService(mConn);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_button:
                if (mService != null) {
                    mService.startClient();
                    showProgressDialog();
                } else {
                    showTost("The service has not existed !");
                }
                break;
            case R.id.stop_button:
                if (mService != null) {
                    mService.stopClient();
                    showProgressDialog();
                } else {
                    Toast.makeText(this, "客户端已注销", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!mService.isClientStarted()) {
            Toast.makeText(this, "请先注册客户端", Toast.LENGTH_SHORT).show();
        }
        ResourceBean bean = null;
        int objectId = 0;
        switch (parent.getId()) {
            case R.id.resource_list_device:
                bean = mDeviceResources.get(position);
                break;
            case R.id.resource_list_location:
                getLocation();
                return;
            case R.id.resource_list_temperature:
                bean = mTemperatureResources.get(position);
                objectId = 3303;
                break;
        }
        if (bean != null) {
            ResourceOperateDialog dialog = ResourceOperateDialog.newInstance(bean);
            final ResourceBean finalBean = bean;
            final int finalObjectId = objectId;
            dialog.setOnOkClickListener(new ResourceOperateDialog.OnOkClickListener() {
                @Override
                public void onOkClick(String value) {
                    mService.updateResource(finalObjectId, finalBean, value);
                }
            });
            dialog.show(getFragmentManager(), "resource");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        //The call to request update for a location is not blocking, hence it wont wait there. Also the provider in emulator may not have been started.
        //也就是说，我们一开始去获取位置的时候，Provider可能还没有被唤醒，而获取位置的方法又是非阻塞方法，所以第一次获取位置时很可能返回null。所以现在这里调用这个方法唤醒Provider.
        new UpdateLocationTask().execute();
    }

    /**
     * 获取地理位置
     */
    private void getLocation() {
        DebugLog.d("getLocation ==>");
        //高版本的权限检查
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        } else {
            Location loc = LocationUtil.getBestLocation(this, null);
            if (loc == null) {
                showTost("Get location failed!");
            } else {
                showTost("latitude: " + loc.getLatitude() + ", longitude:" + loc.getLongitude());
                DebugLog.d("getLocation==> latitude: " + loc.getLatitude() + ", longitude:" + loc.getLongitude());

                double latitude = loc.getLatitude();
                double longitude = loc.getLongitude();
                ResourceBean latBean = new ResourceBean(0, "Latitude", latitude, ResourceBean.ValueType.DOUBLE);
                ResourceBean longBean = new ResourceBean(1, "Longitude", longitude, ResourceBean.ValueType.DOUBLE);
                mService.updateResource(LOCATION, latBean, String.valueOf(latitude));
                mService.updateResource(LOCATION, longBean, String.valueOf(longitude));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        DebugLog.d("onRequestPermissionsResult ==>");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();
                }
                break;
        }
    }

    private void setLocationListener() {
        LocationUtil.addLocationListener(this, null, this);
    }

    private void updateClientDevice(boolean isRegistered, String registrationId) {
        if (isRegistered) {
            mClientStatus.setText("客户端注册成功\n注册Id为：" + registrationId);
        } else {
            mClientStatus.setText("客户端已注销");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService();
        stopService();
    }

    private void sendMessage(int what, Bundle data) {
        Message message = mHandler.obtainMessage();
        message.what = what;
        message.setData(data);
        mHandler.sendMessage(message);
    }

    @Override
    public void onSuccessLocation(Location location) {
        DebugLog.d("onSuccessLocation==>");
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Toast.makeText(this, "Latitude:" + latitude + ", Longitude:" + longitude, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBootstrapSuccess(ServerInfo serverInfo) {
        sendMessage(ClientService.REQUEST_RESULT_BOOTSTRAP_SUCCESS, null);
    }

    @Override
    public void onBootstrapFailure(ServerInfo serverInfo, ResponseCode responseCode, String s) {
        sendMessage(ClientService.REQUEST_RESULT_BOOTSTRAP_FAILURE, null);
    }

    @Override
    public void onBootstrapTimeout(ServerInfo serverInfo) {
        sendMessage(ClientService.REQUEST_RESULT_BOOTSTRAP_TIMEOUT, null);
    }

    @Override
    public void onRegistrationSuccess(DmServerInfo dmServerInfo, String s) {
        Bundle bundle = new Bundle();
        bundle.putString("registrationId", s);
        sendMessage(ClientService.REQUEST_RESULT_REGISTRATION_SUCCESS, bundle);
    }

    @Override
    public void onRegistrationFailure(DmServerInfo dmServerInfo, ResponseCode responseCode, String s) {
        sendMessage(ClientService.REQUEST_RESULT_REGISTRATION_FAILURE, null);
    }

    @Override
    public void onRegistrationTimeout(DmServerInfo dmServerInfo) {
        sendMessage(ClientService.REQUEST_RESULT_REGISTRATION_TIMEOUT, null);
    }

    @Override
    public void onUpdateSuccess(DmServerInfo dmServerInfo, String s) {
        sendMessage(ClientService.REQUEST_RESULT_UPDATE_SUCCESS, null);
    }

    @Override
    public void onUpdateFailure(DmServerInfo dmServerInfo, ResponseCode responseCode, String s) {
        sendMessage(ClientService.REQUEST_RESULT_UPDATE_FAILURE, null);
    }

    @Override
    public void onUpdateTimeout(DmServerInfo dmServerInfo) {
        sendMessage(ClientService.REQUEST_RESULT_UPDATE_TIMEOUT, null);
    }

    @Override
    public void onDeregistrationSuccess(DmServerInfo dmServerInfo, String s) {
        sendMessage(ClientService.REQUEST_RESULT_DEREGISTRATION_SUCCUSS, null);

    }

    @Override
    public void onDeregistrationFailure(DmServerInfo dmServerInfo, ResponseCode responseCode, String s) {
        sendMessage(ClientService.REQUEST_RESULT_DEREGISTRATION_FAILURE, null);
    }

    @Override
    public void onDeregistrationTimeout(DmServerInfo dmServerInfo) {
        sendMessage(ClientService.REQUEST_RESULT_DEREGISTRATION_TIMEOUT, null);
    }

    @Override
    public void register() {

    }

    @Override
    public void destroyClient() {

    }

    @Override
    public void updateClientStatus(boolean registered, String registrationId) {

    }

    @Override
    public void showProgress() {

    }

    @Override
    public void hideProgress() {

    }

    @Override
    public void updateLocation() {

    }

    @Override
    public void updateTemperature() {

    }

    public class ResourceAdapter extends BaseAdapter {

        private ArrayList<ResourceBean> mLists;
        private LayoutInflater mInflater;
        public ResourceAdapter(ArrayList<ResourceBean> lists) {
            this.mLists = lists;
            mInflater = MainActivity.this.getLayoutInflater();
        }

        @Override
        public int getCount() {
            return mLists == null ? 0 : mLists.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.adapter_resource_list_item, null, false);
            }
            TextView name = convertView.findViewById(R.id.resource_name);
            name.setText(mLists.get(position).name);
            return convertView;
        }
    }

    class UpdateLocationTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            LocationUtil.updateLocation(MainActivity.this);
            return null;
        }
    }
    private AlertDialog mProgressDialog;
    private void showProgressDialog() {
        if (mProgressDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(R.layout.view_progress_dialog);
            mProgressDialog = builder.create();
        }
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void showTost(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
