package com.cwtcn.leshanandroidlib.constant;

/**
 * Created by leizhiheng on 2018/1/16.
 */

public class ServerConfig {

    public static final String NOTIFY_PERIOD_PREFERENCES = "nofity.period.preferences";

    /**Server 配置信息*/
    public static final String END_POINT = "18010902WithPsk";//一定要用这个endpoint name,否则会被服务器禁止连接
    public static final String SERVER_URI = "coap://baok180901-00.westeurope.cloudapp.azure.com:5684";
    public static final String PSK_IDENTITY = "18010901id";
    public static final String PSK_KEY = "38383838";

    /**服务器observe后，Client notify的时间间隔*/
    public static final int MIN_PERIOD = 5;
    public static final int MAX_PERIOD = 30;


    /**Client与Server通信结果码*/
    public static final int REQUEST_RESULT_BOOTSTRAP_SUCCESS = 1;
    public static final int REQUEST_RESULT_BOOTSTRAP_FAILURE = 2;
    public static final int REQUEST_RESULT_BOOTSTRAP_TIMEOUT = 3;
    public static final int REQUEST_RESULT_REGISTRATION_SUCCESS = 4;
    public static final int REQUEST_RESULT_REGISTRATION_FAILURE = 5;
    public static final int REQUEST_RESULT_REGISTRATION_TIMEOUT = 6;
    public static final int REQUEST_RESULT_UPDATE_SUCCESS = 7;
    public static final int REQUEST_RESULT_UPDATE_FAILURE = 8;
    public static final int REQUEST_RESULT_UPDATE_TIMEOUT = 9;
    public static final int REQUEST_RESULT_DEREGISTRATION_SUCCUSS = 10;
    public static final int REQUEST_RESULT_DEREGISTRATION_FAILURE = 11;
    public static final int REQUEST_RESULT_DEREGISTRATION_TIMEOUT = 12;
}
