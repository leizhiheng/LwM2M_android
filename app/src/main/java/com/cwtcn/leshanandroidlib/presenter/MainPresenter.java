package com.cwtcn.leshanandroidlib.presenter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;

import com.cwtcn.leshanandroidlib.constant.ServerConfig;
import com.cwtcn.leshanandroidlib.model.ClientService;
import com.cwtcn.leshanandroidlib.model.IClientModel;
import com.cwtcn.leshanandroidlib.model.ResourceBean;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.LocationUtil;
import com.cwtcn.leshanandroidlib.view.IMainView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServerInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.eclipse.leshan.LwM2mId.LOCATION;

/**
 * Created by leizhiheng on 2018/1/16.
 */
public class MainPresenter implements IMainPresenter, LwM2mClientObserver {
    private IMainView mView;
    private IClientModel mModel;
    private Context mContext;

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DebugLog.d("onServiceConnected");
            ClientService.LeshanBinder binder = (ClientService.LeshanBinder) service;
            ClientService ser = ((ClientService.LeshanBinder) service).getService();
            ser.setContext(mContext);
            ser.setObserver(MainPresenter.this);
            mModel = ser;
            if (!TextUtils.isEmpty(mModel.getRegistrationId())) {
                mView.updateClientStatus(true, mModel.getRegistrationId());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            mView.hideProgress();
            int what = msg.what;
            Bundle data = msg.getData();

            switch (what) {
                case ServerConfig.REQUEST_RESULT_BOOTSTRAP_SUCCESS:
                    break;
                case ServerConfig.REQUEST_RESULT_BOOTSTRAP_FAILURE:
                    break;
                case ServerConfig.REQUEST_RESULT_BOOTSTRAP_TIMEOUT:
                    break;
                case ServerConfig.REQUEST_RESULT_REGISTRATION_SUCCESS:
                    String registrationId = data.getString("registrationId");
                    mView.updateClientStatus(true, registrationId);
                    mModel.setRegistrationId(registrationId);
                    break;
                case ServerConfig.REQUEST_RESULT_REGISTRATION_FAILURE:
                    String responseCode = data.getString("responseCode");
                    String responseName = data.getString("responseName");
                    mModel.setRegistrationId(null);
                    mView.showToast("Requst result code: " + what + ", responseCode:" + responseCode + ", responseName:" + responseName);
                    break;
                case ServerConfig.REQUEST_RESULT_REGISTRATION_TIMEOUT:
                    break;
                case ServerConfig.REQUEST_RESULT_UPDATE_SUCCESS:

                    break;
                case ServerConfig.REQUEST_RESULT_UPDATE_FAILURE:
                    break;
                case ServerConfig.REQUEST_RESULT_UPDATE_TIMEOUT:
                    break;
                case ServerConfig.REQUEST_RESULT_DEREGISTRATION_SUCCUSS:
                    mModel.setRegistrationId(null);
                    mView.updateClientStatus(false, null);
                    break;
                case ServerConfig.REQUEST_RESULT_DEREGISTRATION_FAILURE:
                    break;
                case ServerConfig.REQUEST_RESULT_DEREGISTRATION_TIMEOUT:
                    break;
            }
            return false;
        }
    });

    public MainPresenter(IMainView view, Context context) {
        this.mView = view;
        mContext = context;
        startService();
    }

    /**
     * 开启服务
     */
    private void startService() {
        Intent intent = new Intent(mContext, ClientService.class);
        mContext.startService(intent);
        mContext.bindService(intent, mConn, Context.BIND_AUTO_CREATE);
    }

    /**
     * 结束服务
     */
    private void stopService() {
        Intent intent = new Intent(mContext, ClientService.class);
        //mContext.stopService(intent);
    }

    public void unbindService() {
        mContext.unbindService(mConn);
    }

    @Override
    public void scanQRCode(Context context) {
    }

    /**
     * 生成二维码
     * @param content 二维码内容
     * @param width 二维码图片宽度
     * @param height 二维码图片高度
     * @return
     */
    @Override
    public Bitmap encodeQRCode(String content, int width, int height) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, String> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        try {
            BitMatrix encode = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            int[] pixels = new int[width * height];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (encode.get(j, i)) {
                        pixels[i * width + j] = 0x00000000;
                    } else {
                        pixels[i * width + j] = 0xffffffff;
                    }
                }
            }
            return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int serverId;

    @Override
    public void register(int serverId) {
        this.serverId = serverId;

        if (!TextUtils.isEmpty(mModel.getRegistrationId())) {
            mView.showToast("Client has registered!");
        } else {
            mView.showProgress();
            mModel.register(serverId);
        }
    }

    @Override
    public void destroy() {
        if (mModel != null && mModel.isClientStarted()) {
            mModel.destroy();
            mView.showProgress();
            //stopService();
        } else {
            mView.showToast("客户端已注销");
        }
    }

    @Override
    public void updateLocation() {
        Location loc = LocationUtil.getBestLocation(mContext, null);
        double latitude = 0.0;//loc.getLatitude();
        double longitude = 0.0;//loc.getLongitude();
        boolean isRandom = false;
        if (loc == null) {
            //mView.showToast("Get location failed!");
            latitude = nextDouble(50.0, 70.0);
            longitude = nextDouble(-200, -230);
            isRandom = true;
        } else {
            DebugLog.d("getLocation==> latitude: " + loc.getLatitude() + ", longitude:" + loc.getLongitude());
            latitude = loc.getLatitude();
            longitude = loc.getLongitude();
            isRandom = true;
        }
        mView.showToast("latitude: " + latitude + ", longitude:" + longitude + ", isRandom:" + isRandom);
        ResourceBean latBean = new ResourceBean(0, "Latitude", latitude, ResourceBean.ValueType.DOUBLE);
        ResourceBean longBean = new ResourceBean(1, "Longitude", longitude, ResourceBean.ValueType.DOUBLE);
        mModel.updateResource(LOCATION, latBean, String.valueOf(latitude));
        mModel.updateResource(LOCATION, longBean, String.valueOf(longitude));
    }

    public double nextDouble(final double min, final double max) {
        if (max < min) {
            return 0;
        }
        if (min == max) {
            return min;
        }
        return min + ((max - min) * new Random().nextDouble());
    }

    @Override
    public void updateTemperature() {

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
}
