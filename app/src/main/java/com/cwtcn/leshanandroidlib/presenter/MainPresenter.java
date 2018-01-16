package com.cwtcn.leshanandroidlib.presenter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.cwtcn.leshanandroidlib.constant.ServerConfig;
import com.cwtcn.leshanandroidlib.model.ClientService;
import com.cwtcn.leshanandroidlib.model.IClientModel;
import com.cwtcn.leshanandroidlib.model.ResourceBean;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.LocationUtil;
import com.cwtcn.leshanandroidlib.view.IMainView;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServerInfo;

import static org.eclipse.leshan.LwM2mId.LOCATION;

/**
 * Created by leizhiheng on 2018/1/16.
 */
public class MainPresenter implements IMainPresenter, LwM2mClientObserver{
    private IMainView mView;
    private IClientModel mModel;
    private Context mContext;

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ClientService.LeshanBinder binder = (ClientService.LeshanBinder) service;
            ClientService ser = ((ClientService.LeshanBinder) service).getService();
            ser.setContext(mContext);
            ser.setObserver(MainPresenter.this);
            mModel = ser;
            mModel.register();
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
            mView.showToast("Requst result code : " + what);
            switch (what) {
                case ServerConfig.REQUEST_RESULT_BOOTSTRAP_SUCCESS:
                    break;
                case ServerConfig.REQUEST_RESULT_BOOTSTRAP_FAILURE:
                    break;
                case ServerConfig.REQUEST_RESULT_BOOTSTRAP_TIMEOUT:
                    break;
                case ServerConfig.REQUEST_RESULT_REGISTRATION_SUCCESS:
                    mView.updateClientStatus(true, data.getString("registrationId"));
                    break;
                case ServerConfig.REQUEST_RESULT_REGISTRATION_FAILURE:
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
        mContext.stopService(intent);
        mContext.unbindService(mConn);
    }

    @Override
    public void register() {
        if (mModel == null) {
            mView.showProgress();
            startService();
        } else {
            if (mModel.isClientStarted()) {
                mView.showToast("The service has not existed !");
            } else {
                mView.showProgress();
                mModel.register();
            }
        }
    }

    @Override
    public void destroy() {
        if (mModel != null) {
            mModel.destroy();
            mView.showProgress();
            stopService();
        } else {
            mView.showToast("客户端已注销");
        }
    }

    @Override
    public void updateLocation() {
        Location loc = LocationUtil.getBestLocation(mContext, null);
        if (loc == null) {
            mView.showToast("Get location failed!");
        } else {
            mView.showToast("latitude: " + loc.getLatitude() + ", longitude:" + loc.getLongitude());
            DebugLog.d("getLocation==> latitude: " + loc.getLatitude() + ", longitude:" + loc.getLongitude());
            double latitude = loc.getLatitude();
            double longitude = loc.getLongitude();
            ResourceBean latBean = new ResourceBean(0, "Latitude", latitude, ResourceBean.ValueType.DOUBLE);
            ResourceBean longBean = new ResourceBean(1, "Longitude", longitude, ResourceBean.ValueType.DOUBLE);
            mModel.updateResource(LOCATION, latBean, String.valueOf(latitude));
            mModel.updateResource(LOCATION, longBean, String.valueOf(longitude));
        }
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
        sendMessage(ServerConfig.REQUEST_RESULT_REGISTRATION_FAILURE, null);
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
