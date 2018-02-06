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

public abstract class ExtendBaseInstanceEnabler extends BaseInstanceEnabler {

    protected Context mContext;
    protected int objectId;
    /*服务器observe后，Notify间隔时间*/
    protected int intervalInSec = -1;
    protected boolean startedObseve = false;
    protected HashMap<Integer, Long> observedResource;

    protected OnWriteReadListener mOnWriteReadListener;

    /**
     * 当InstanceEnabler被创建时，ClientService会调用这个方法
     */
    public void onCreate(Context context, int objectId, OnWriteReadListener onWriteReadListener) {
        this.mContext = context;
        this.objectId = objectId;
        this.mOnWriteReadListener = onWriteReadListener;
    }


    /**
     * 当InstanceEnabler被之前，ClientService会调用这个方法
     */
    public void onDestory() {

    }

    /**
     * 当ClientService定位成功后，调用该方法，定位结果发送给InstanceEnabler。
     * 具体的操作可以在子类中实现
     * @param lat 经度
     * @param lon 维度
     * @param accuracy 精度
     */
    public void setLocateResult(double lat, double lon, float accuracy) {
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
//        if (intervalInSec < 0) {
//            intervalInSec = mOnWriteReadListener.readPeriod(objectId);
//        }
        intervalInSec = 10;
        if (observedResource == null) {
            startedObseve = true;
            observedResource = new HashMap<Integer, Long>();
            startPeriodicNotify();
        }
        observedResource.put(resourceId, getNowMilliSecs());
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
                            //在通知服务器更新Resource的值之前，如果需要做一些其他的工作，就执行beforeFireResourceChange(resourceId)方法。
                            //这个方法在不同的子类中有不同的具体实现
                            beforeFireResourceChange(resourceid);
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

    /**
     * 如果子类在周期提交资源值之前需要做一些其他的工作，则复写这个方法。
     * @param resourceId
     */
    protected void beforeFireResourceChange(int resourceId) {
    }

    private long getNowMilliSecs() {
        Date date = new Date();
        return date.getTime();
    }
}
