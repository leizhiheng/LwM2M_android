package com.cwtcn.leshanandroidlib.utils.interfaces;

import com.cwtcn.leshanandroidlib.resources.ExtendBaseInstanceEnabler;

/**
 * Created by leizhiheng on 2018/1/19.
 *
 *
 */
public interface OnWriteReadListener {
    /**
     * 当服务器端通过Write操作将Object的IntervalInSec传递过来时，通知外部
     * @param objectId
     * @param period
     */
    void writedPeriod(int objectId, int period);

    /**
     * 当服务器端想要通过Read操作读取某个Object的IntervalInSec的值时，通知外部
     * @param objectId
     * @return
     */
    int readPeriod(int objectId);

    /**
     * 存储数据到SharedPreferences
     * @param key
     * @param value
     */
    void setStringToPreference(String key, String value);

    /**
     * 获取数据SharedPreferences中的数据
     * @param key
     * @return
     */
    String getStringFromPreferemce(String key, String defaultValue);

    /**
     * 要求获取定位信息
     * @param requestLocationEnabler 要求获取定位信息的ExtendBaseInstanceEnabler实例
     */
    void requestLocate(int objectId, ExtendBaseInstanceEnabler requestLocationEnabler);
}
