package com.cwtcn.leshanandroidlib.utils.interfaces;

/**
 * Created by leizhiheng on 2018/1/19.
 *
 *
 */
public interface OnWriteReadListener {
    /**当服务器端通过Write操作将Object的IntervalInSec传递过来时，通知外部*/
    void writedPeriod(int objectId, int period);
    /**当服务器端想要通过Read操作读取某个Object的IntervalInSec的值时，通知外部*/
    int readPeriod(int objectId);
    /**存储数据*/
    void setStringToPreference(String key, String value);
    String getStringFromPreferemce(String key);

}
