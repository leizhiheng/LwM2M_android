package com.cwtcn.leshanandroidlib.clientmanage;

/**
 * Created by leizhiheng on 2018/1/16.
 */
public interface IMainView {
    void registeButtonClicked();
    void destroyButtonClicked();
    void updateClientStatus(boolean registered, String registrationId);
    void showProgress();
    void hideProgress();
    void showToast(String msg);
}
