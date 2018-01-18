package com.cwtcn.leshanandroidlib.presenter;

/**
 * Created by leizhiheng on 2018/1/16.
 */

public interface IMainPresenter {
    void register(int serverId);
    void destroy();
    void updateLocation();
    void updateTemperature();
    void unbindService();
}
