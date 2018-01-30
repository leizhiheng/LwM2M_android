package com.cwtcn.leshanandroidlib.resources;

import android.content.Context;

import com.cwtcn.leshanandroidlib.model.ClientService;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnWriteReadListener;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.WriteResponse;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by leizhiheng on 2018/1/18.
 */

public class ExtendBaseInstanceEnabler extends BaseInstanceEnabler {

    protected Context mContext;
    private int objectId;
    /*服务器observe后，Notify间隔时间*/
    private int intervalInSec = -1;
    private boolean startedObseve = false;
    private HashMap<Integer, Long> observedResource;

    protected OnWriteReadListener mOnWriteReadListener;
    public void setOnWriteNotifyPeriodListener(OnWriteReadListener listener) {
        this.mOnWriteReadListener = listener;
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        return super.write(resourceid, value);
    }

    /**
     * 服务器端observe一个Resource后，就调用该方法
     * @param resourceId
     */
    @Override
    public void notifyObserve(int resourceId) {
        DebugLog.d("notifyObserve resourceId = " + resourceId);
        if (intervalInSec < 0) {
            intervalInSec = mOnWriteReadListener.readPeriod(objectId);
        }
        if (observedResource == null) {
            startedObseve = true;
            observedResource = new HashMap<Integer, Long>();
            startPeriodicNotify();
        }
        observedResource.put(resourceId, getNowMilliSecs());
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }

    public int getObjectId() {
        return this.objectId;
    }

    public void setIntervalInSec(int intervalInSec) {
        DebugLog.d("setIntervalInSec intervalInSec = " + intervalInSec);
        this.intervalInSec = intervalInSec;
    }
    /**
     * 通过将startedObseve设置为false，来中断startPeriodicNotify()中的循环线程
     * @param startedObseve
     */
    public void setStartedObseve(boolean startedObseve) {
        this.startedObseve = startedObseve;
    }

    /**
     * 如果服务器observe了Resource，则开启一个Thread，用于计时和上报数据
     */
    private void startPeriodicNotify() {
        if (intervalInSec < 0) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (startedObseve) {
                    try {
                        //每一秒检查一次，距离上次Notify的时间是否已达到minPeriod
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    long milliSecs = getNowMilliSecs();
                    for (int resourceid : observedResource.keySet()) {
//                        DebugLog.d("startPeriodicNotify resourceid = " + resourceid);
                        if (intervalInSec * 1000 < milliSecs - observedResource.get(resourceid)) {
                            //如果时间间隔大于minPeriod，则上报数据
                            fireResourcesChange(resourceid);
                            //记录本次上报时间
                            observedResource.put(resourceid, milliSecs);
                        }
                    }
                }
            }
        }).start();
    }

    private long getNowMilliSecs() {
        Date date = new Date();
        return date.getTime();
    }
}
