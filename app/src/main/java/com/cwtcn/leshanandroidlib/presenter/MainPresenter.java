package com.cwtcn.leshanandroidlib.presenter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.IBinder;
import android.text.TextUtils;

import com.cwtcn.leshanandroidlib.constant.ServerConfig;
import com.cwtcn.leshanandroidlib.model.ClientService;
import com.cwtcn.leshanandroidlib.model.IClientModel;
import com.cwtcn.leshanandroidlib.model.ResourceBean;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnOperationResultListener;
import com.cwtcn.leshanandroidlib.utils.locationutils.LocationUtil;
import com.cwtcn.leshanandroidlib.view.IMainView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.eclipse.leshan.LwM2mId.LOCATION;

/**
 * Created by leizhiheng on 2018/1/16.
 */
public class MainPresenter implements IMainPresenter, OnOperationResultListener {
    private IMainView mView;
    private IClientModel mModel;
    private Context mContext;

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DebugLog.d("onServiceConnected");
            ClientService.LeshanBinder binder = (ClientService.LeshanBinder) service;
            ClientService ser = ((ClientService.LeshanBinder) service).getService();

            mModel = ser;
            mModel.setOnOperationResultListener(MainPresenter.this);
            mModel.register();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mModel = null;
        }
    };

    public MainPresenter(IMainView view, Context context) {
        this.mView = view;
        mContext = context;
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

    @Override
    public void checkRegistrationId() {
        if (mModel != null) {
            if (TextUtils.isEmpty(mModel.getRegistrationId())) {
                mView.updateClientStatus(true, mModel.getRegistrationId());
            }
        }
    }

    @Override
    public void register() {
        Intent intent = new Intent(mContext, ClientService.class);
        mContext.startService(intent);
        if (mModel == null) {
            mContext.bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        } else {
            mModel.register();
        }
    }

    @Override
    public void destroyClient() {
        if (mModel != null) {
            mModel.destroy();//取消对服务器的注册
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
    public void onStartOperate() {
        mView.showProgress();
    }

    @Override
    public void onOperateReject(String rejectReason) {
        mView.showToast(rejectReason);
    }

    @Override
    public void onOperateResult(int resultCode, String msg) {
        mView.hideProgress();
        switch (resultCode) {
            case ClientService.MSG_WHAT_NETWORK_IS_NOT_AVAILABLE:
                mView.updateClientStatus(false, null);
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
                mView.updateClientStatus(true, msg);
                break;
            case ServerConfig.REQUEST_RESULT_REGISTRATION_FAILURE:
                msg = "Registration Failed!";
                break;
            case ServerConfig.REQUEST_RESULT_REGISTRATION_TIMEOUT:
                msg = "Registration Timeout!";
                break;

                /*
                 * 更新
                 */
            case ServerConfig.REQUEST_RESULT_UPDATE_SUCCESS:
                break;
            case ServerConfig.REQUEST_RESULT_UPDATE_FAILURE:
                break;
            case ServerConfig.REQUEST_RESULT_UPDATE_TIMEOUT:
                break;

                /*
                 * 注销
                 */
            case ServerConfig.REQUEST_RESULT_DEREGISTRATION_SUCCUSS:
                mView.updateClientStatus(false, null);
                break;
            case ServerConfig.REQUEST_RESULT_DEREGISTRATION_FAILURE:
            case ServerConfig.REQUEST_RESULT_DEREGISTRATION_TIMEOUT:
                msg = "Client Disconnected!";
                mView.updateClientStatus(false, null);
                break;
        }
        if (!TextUtils.isEmpty(msg)) {
            mView.showToast(msg);
        }
    }
}
