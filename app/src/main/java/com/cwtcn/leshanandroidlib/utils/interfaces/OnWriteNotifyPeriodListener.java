package com.cwtcn.leshanandroidlib.utils.interfaces;

/**
 * Created by leizhiheng on 2018/1/19.
 */

public interface OnWriteNotifyPeriodListener {
    void writedPeriod(int objectId, int period);
    int readPeriod(int objectId);
}
