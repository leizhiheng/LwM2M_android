package com.cwtcn.leshanandroidlib.view;

/**
 * Created by leizhiheng on 2018/1/16.
 */

public interface IMainView {
    void register(int serverId);
    void destroyClient();
    void updateClientStatus(boolean registered, String registrationId);
    void showProgress();
    void hideProgress();
    void updateLocation();
    void updateTemperature();
    void showToast(String msg);
}
