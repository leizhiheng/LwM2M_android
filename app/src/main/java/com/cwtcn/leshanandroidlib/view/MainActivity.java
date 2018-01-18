package com.cwtcn.leshanandroidlib.view;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.cwtcn.leshanandroidlib.dialog.ResourceOperateDialog;
import com.cwtcn.leshanandroidlib.model.ResourceBean;
import com.cwtcn.leshanandroidlib.presenter.IMainPresenter;
import com.cwtcn.leshanandroidlib.presenter.MainPresenter;
import com.cwtcn.leshanandroidlib.resources.RandomTemperatureSensor;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.LocationUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener, IMainView {
    public static final String TAG = "MainActivity";

    private static final int PERMISSIONS_REQUEST_LOCATION = 1;
    private Button mStartButton, mStartLoc, mStopButton;
    private ListView mListDevice, mListLocation, mListTemperature;
    private TextView mClientStatus;
    private AlertDialog mProgressDialog;
    private static ArrayList<ResourceBean> mDeviceResources, mLocationResources, mTemperatureResources;
    private static IMainPresenter mPresenter;

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

        mPresenter = new MainPresenter(this, this.getApplicationContext());
    }

    private void initView() {
        mStartButton = (Button) findViewById(R.id.start_button);
        mStartLoc = findViewById(R.id.start_local_button);
        mStopButton = (Button) findViewById(R.id.stop_button);
        mStartButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mStartLoc.setOnClickListener(this);
        mStartLoc.setVisibility(View.GONE);

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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_button:
                //注册到服务器
                register(1);
                break;
            case R.id.start_local_button:
                register(0);
                break;
            case R.id.stop_button:
                //注销客户端
                destroyClient();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ResourceBean bean = null;
        int objectId = 0;
        switch (parent.getId()) {
            case R.id.resource_list_device:
                bean = mDeviceResources.get(position);
                break;
            case R.id.resource_list_location:
                updateLocation();
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
//                    mService.updateResource(finalObjectId, finalBean, value);
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
        LocationUtil.updateLocation(MainActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.unbindService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        DebugLog.d("onRequestPermissionsResult ==>");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPresenter.updateLocation();
                }
                break;
        }
    }

    @Override
    public void register(int serverId) {
        mPresenter.register(serverId);
    }

    @Override
    public void destroyClient() {
        mPresenter.destroy();
    }

    @Override
    public void updateClientStatus(boolean registered, String registrationId) {
        if (registered) {
            mClientStatus.setText("客户端注册成功\n注册Id为：" + registrationId);
        } else {
            mClientStatus.setText("客户端已注销");
        }
    }

    @Override
    public void showProgress() {
        if (mProgressDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(R.layout.view_progress_dialog);
            mProgressDialog = builder.create();
        }
        mProgressDialog.show();
    }

    @Override
    public void hideProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void updateLocation() {
        DebugLog.d("updateLocation ==>");
        //高版本的权限检查
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        } else {
            mPresenter.updateLocation();
        }
    }

    @Override
    public void updateTemperature() {

    }

    @Override
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
}
