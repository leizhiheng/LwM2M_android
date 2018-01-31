package com.cwtcn.leshanandroidlib.model;

/**
 * Created by leizhiheng on 2018/1/16.
 */

public interface IClientModel {
    void register();
    void destroy();
    void updateResource(int objectId, ResourceBean bean, String newValue);
    void setOnOperationResultListener(ClientService.OnOperationResultListener listener);
}
