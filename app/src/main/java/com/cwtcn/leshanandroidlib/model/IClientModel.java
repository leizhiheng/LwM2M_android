package com.cwtcn.leshanandroidlib.model;

/**
 * Created by leizhiheng on 2018/1/16.
 */

public interface IClientModel {
    void register(int serverId);
    void destroy();
    void updateResource(int objectId, ResourceBean bean, String newValue);
    boolean isClientStarted();
    void setRegistrationId(String registrationId);
    String getRegistrationId();
}
