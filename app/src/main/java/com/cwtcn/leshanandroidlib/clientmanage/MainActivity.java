package com.cwtcn.leshanandroidlib.clientmanage;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cwtcn.leshanandroidlib.R;
import com.cwtcn.leshanandroidlib.constant.ServerConfig;
import com.cwtcn.leshanandroidlib.dialog.ResourceOperateDialog;
import com.cwtcn.leshanandroidlib.bean.ResourceBean;
import com.cwtcn.leshanandroidlib.resources.RandomTemperatureSensor;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener, IMainView {
    public static final String TAG = "MainActivity";

    private static final int PERMISSIONS_REQUEST_LOCATION = 1;
    private static final int PERMISSIONS_REQUEST_CONTACT = 2;
    private static final int REQ_CODE_PERMISSION = 0x1111;
    private Button mStartButton, mStartLoc, mStopButton;
    private ListView mListDevice, mListLocation, mListTemperature;
    private TextView mClientStatus;
    private ImageView mImageView;
    private AlertDialog mProgressDialog;
    private static ArrayList<ResourceBean> mDeviceResources, mLocationResources, mTemperatureResources;
    private static IMainPresenter mPresenter;

    static {
        //加载Device的资源
        mDeviceResources = new ArrayList<ResourceBean>();
        mDeviceResources.add(new ResourceBean(2, "Serials Num", null, ResourceBean.ValueType.STRING));
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

        if (savedInstanceState != null) {
            String registrationId = savedInstanceState.getString("registrationId");
            updateClientStatus(!TextUtils.isEmpty(registrationId), registrationId);
        }

        encodeQRCode();
    }

    private void initView() {
        mStartButton = (Button) findViewById(R.id.start_button);
        mStartLoc = (Button) findViewById(R.id.start_local_button);
        mStopButton = (Button) findViewById(R.id.stop_button);
        mStartButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mStartLoc.setOnClickListener(this);
        mStartLoc.setVisibility(View.GONE);

        mImageView = (ImageView) findViewById(R.id.imageview_qrcode);

        mListDevice = (ListView) findViewById(R.id.resource_list_device);
        mListLocation = (ListView) findViewById(R.id.resource_list_location);
        mListTemperature = (ListView) findViewById(R.id.resource_list_temperature);
        mListDevice.setAdapter(new ResourceAdapter(mDeviceResources));
        mListLocation.setAdapter(new ResourceAdapter(mLocationResources));
        mListTemperature.setAdapter(new ResourceAdapter(mTemperatureResources));
        mListDevice.setOnItemClickListener(this);
        mListLocation.setOnItemClickListener(this);
        mListTemperature.setOnItemClickListener(this);

        mListDevice.setVisibility(View.GONE);
        mListLocation.setVisibility(View.GONE);
        mListTemperature.setVisibility(View.GONE);

        mClientStatus = (TextView) findViewById(R.id.client_status);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_button:
                //注册到服务器
                registeButtonClicked();
                break;
            case R.id.start_local_button:

                break;
            case R.id.stop_button:
                //注销客户端
                destroyButtonClicked();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ResourceBean bean = null;
        int objectId = 0;
        switch (parent.getId()) {
            case R.id.resource_list_device:
//                bean = mDeviceResources.get(position);
                if (position == 0) {
//                    scanQRCode();
                } else if (position == 1) {

                }

                return;
            case R.id.resource_list_location:
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("registrationId", mClientStatus.getText().toString());
    }

    /**
     * 生成二维码
     */
    private void encodeQRCode() {
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        int width = manager.getDefaultDisplay().getWidth() - 100;
        Bitmap qrBm = mPresenter.encodeQRCode(ServerConfig.END_POINT, width, width);
//        showToast("QRCode image encode succussful ? " + (qrBm == null) + ", width = " + width);
        mImageView.setBackground(new BitmapDrawable(qrBm));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            //扫描二维码时，请求Camera权限的结果
//            case REQ_CODE_PERMISSION: {
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // User agree the permission
//                    startCaptureActivityForResult();
//                } else {
//                    // User disagree the permission
//                    Toast.makeText(this, "You must agree the camera permission request before you use the code scan function", Toast.LENGTH_LONG).show();
//                }
//            }
//            break;
            case PERMISSIONS_REQUEST_LOCATION:
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    mPresenter.updateLocation();
//                }
                break;
            case PERMISSIONS_REQUEST_CONTACT:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPresenter.register();
                }
                break;
        }
    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        switch (requestCode) {
//            case CaptureActivity.REQ_CODE:
//                String codeString = "";
//                switch (resultCode) {
//                    case RESULT_OK:
//                        codeString = data.getStringExtra(CaptureActivity.EXTRA_SCAN_RESULT);
//                        showToast("QRCode string:" + codeString);
//                        break;
//                    case RESULT_CANCELED:
//                        if (data != null) {
//                            // for some reason camera is not working correctly
//                            codeString = data.getStringExtra(CaptureActivity.EXTRA_SCAN_RESULT);
//                            showToast("QRCode string:" + codeString);
//                        }
//                        break;
//                }
//                break;
//        }
//    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void registeButtonClicked() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS,
                            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_CONTACT);
        } else {
            mPresenter.register();
        }
    }

    @Override
    public void destroyButtonClicked() {
        mPresenter.destroyClient();
    }

    @Override
    public void updateClientStatus(boolean registered, String registrationId) {
        if (registered) {
            mClientStatus.setText("客户端注册成功\n注册Id为：" + registrationId);
        } else {
            mClientStatus.setText("客户端已注销");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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
            TextView name = (TextView) convertView.findViewById(R.id.resource_name);
            name.setText(mLists.get(position).name);
            return convertView;
        }
    }
}
