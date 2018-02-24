package com.cwtcn.leshanandroidlib.resources;

import android.content.Context;
import android.content.SharedPreferences;

import com.cwtcn.leshanandroidlib.constant.ServerConfig;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnWriteReadListener;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class MyLocation extends ExtendBaseInstanceEnabler {

    /**
     * 经度
     */
    private float latitude;
    /**
     * 纬度
     */
    private float longitude;
    /**
     * 精度：定位的误差范围，单位为米
     */
    private float accuracy;
    private Date timestamp;

    private CountDownLatch mCountDownLatch;

    @Override
    public void onCreate(Context context, int objectId, OnWriteReadListener onWriteReadListener) {
        super.onCreate(context, objectId, onWriteReadListener);
    }

    @Override
    public void onDestory() {
    }

    public MyLocation() {}

    @Override
    public ReadResponse read(int resourceid) {
        DebugLog.d("leshan.MyLocation.read() resourceId = " + resourceid);
        if (mCountDownLatch == null) {
            mCountDownLatch = new CountDownLatch(1);
        }
        switch (resourceid) {
            case 0://读取经度
                mOnWriteReadListener.requestLocate(objectId, this);
                try {
                    DebugLog.d("mCountDownLatch.aweit getLongitude begin");
                    //在主线程中获取定位，所以这里要等主线程定位完成，然后再返回read结果。
                    mCountDownLatch.await();
                    DebugLog.d("mCountDownLatch.aweit getLongitude end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return ReadResponse.success(resourceid, getLatitude());
            case 1://读取纬度
                return ReadResponse.success(resourceid, getLongitude());
            case 3://读取精度
                return ReadResponse.success(resourceid, getAccuracy());
            case 5://读取时间戳
                return ReadResponse.success(resourceid, getTimestamp());
            default:
                return super.read(resourceid);
        }
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        DebugLog.d("MyLocation.write resource id  = " + resourceid + ", value = " + value.toString());
        return WriteResponse.success();
    }

    @Override
    public void notifyObserve(int resourceId) {
        super.notifyObserve(resourceId);

    }

    @Override
    public void setLocateResult(double lat, double lon, float accuracy) {
        latitude = (float) lat;
        longitude = (float) lon;
        this.accuracy = accuracy;
        DebugLog.d("mCountDownLatch.countDown");
        mCountDownLatch.countDown();
    }

    public void updateLocation(int resourceId, float newValue) {
        timestamp = new Date();
        if (resourceId == 0) {
            latitude = newValue;
            fireResourcesChange(0, 5);
        } else if (resourceId == 1) {
            longitude = newValue;
            fireResourcesChange(1, 5);
        }
    }

    private void moveLongitude() {
        timestamp = new Date();
        fireResourcesChange(1, 5);
    }

    private void moveLatitude() {
        timestamp = new Date();
        fireResourcesChange(0, 5);
    }

    public float getLatitude() {
        DebugLog.d("MyLocation.getLatitude==> latitude:" + latitude);
        return latitude;
    }

    public float getLongitude() {
        DebugLog.d("MyLocation.getLongitude==> longitude:" + longitude);
        return longitude;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public Date getTimestamp() {
        //return timestamp;
        return new Date();
    }
}