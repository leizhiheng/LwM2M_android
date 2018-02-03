package com.cwtcn.leshanandroidlib.resources;

import android.content.Context;

import com.cwtcn.leshanandroidlib.constant.ServerConfig;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.locationutils.WifiLocateUtils;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class MyLocation extends ExtendBaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(MyLocation.class);

    private static final Random RANDOM = new Random();

    private float latitude;
    private float longitude;
    private float scaleFactor;
    private WifiLocateUtils mWifiLocUtils;
    private Date timestamp;
    private Map<Integer, Long> observedResource = new HashMap<Integer, Long>();

    private CountDownLatch mCountDownLatch;

    @Override
    public void onCreate(Context context) {
        mContext = context;
    }

    @Override
    public void onDestory() {
    }

    public MyLocation() {}

    public MyLocation(Float latitude, Float longitude, float scaleFactor) {
        if (latitude != null) {
            this.latitude = latitude + 90f;
        } else {
            this.latitude = RANDOM.nextInt(180);
        }
        if (longitude != null) {
            this.longitude = longitude + 180f;
        } else {
            this.longitude = RANDOM.nextInt(360);
        }
        this.scaleFactor = scaleFactor;
        timestamp = new Date();
    }

    @Override
    public ObserveResponse observe(int resourceid) {
        notifyObserve(resourceid);
        return super.observe(resourceid);
    }

    @Override
    public ReadResponse read(int resourceid) {
        DebugLog.d("leshan.MyLocation.read() resourceId = " + resourceid);
        if (mCountDownLatch == null) {
            mCountDownLatch = new CountDownLatch(1);
        }
        switch (resourceid) {
            case 0:
                mOnWriteReadListener.requestLocate(objectId, this);
                try {
                    DebugLog.d("mCountDownLatch.aweit getLatitude begin");
                    //在主线程中获取定位，所以这里要等主线程定位完成，然后在返回read结果。
                    mCountDownLatch.await();
                    DebugLog.d("mCountDownLatch.aweit getLatitude end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return ReadResponse.success(resourceid, getLatitude());
            case 1:
                try {
                    DebugLog.d("mCountDownLatch.aweit getLongitude begin");
                    //在主线程中获取定位，所以这里要等主线程定位完成，然后在返回read结果。
                    mCountDownLatch.await();
                    DebugLog.d("mCountDownLatch.aweit getLongitude end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return ReadResponse.success(resourceid, getLongitude());
            case 5:
                return ReadResponse.success(resourceid, getTimestamp());
            default:
                return super.read(resourceid);
        }
    }

    @Override
    public void setLocateResult(double lat, double lon, String msg) {
        latitude = (float) lat;
        longitude = (float) lon;
        DebugLog.d("mCountDownLatch.countDown");
        mCountDownLatch.countDown();
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        DebugLog.d("MyLocation.write resource id  = " + resourceid + ", value = " + value.toString());
        return WriteResponse.success();
    }

    private void notifyResource(long secs) {
        for (int resourceid : observedResource.keySet()) {
            if (ServerConfig.MIN_PERIOD < secs - observedResource.get(resourceid)
                    && secs - observedResource.get(resourceid) < ServerConfig.MAX_PERIOD) {
                observedResource.put(resourceid, secs);
                switch (resourceid) {
                    case 0:
                        latitude += 1;
                        break;
                    case 1:
                        longitude += 1;
                        break;
                    case 5:
                        timestamp = new Date();
                        break;
                }
                fireResourcesChange(resourceid);
            }
        }
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

    public Date getTimestamp() {
        //return timestamp;
        return new Date();
    }
}