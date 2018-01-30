package com.cwtcn.leshanandroidlib.resources;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.cwtcn.leshanandroidlib.constant.ServerConfig;
import com.cwtcn.leshanandroidlib.utils.DebugLog;

import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MyLocation extends ExtendBaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(MyLocation.class);

    private static final Random RANDOM = new Random();

    private float latitude;
    private float longitude;
    private float scaleFactor;
    private Date timestamp;
    private Map<Integer, Long> observedResource = new HashMap<Integer, Long>();
    public void setContext(Context context) {
        this.mContext = context;
    }

    public MyLocation() {
        this(null, null, 1.0f);
    }

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
    public ReadResponse read(int resourceid) {
        LOG.info("Read on Location Resource " + resourceid);
        System.out.print("leshan.MyLocation.read() resourceId = " + resourceid);
        switch (resourceid) {
            case 0:
                return ReadResponse.success(resourceid, getLatitude());
            case 1:
                return ReadResponse.success(resourceid, getLongitude());
            case 5:
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

    public void moveLocation(String nextMove) {
        switch (nextMove.charAt(0)) {
            case 'w':
                moveLatitude(1.0f);
                break;
            case 'a':
                moveLongitude(-1.0f);
                break;
            case 's':
                moveLatitude(-1.0f);
                break;
            case 'd':
                moveLongitude(1.0f);
                break;
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

    private void moveLatitude(float delta) {
        latitude = latitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourcesChange(0, 5);
    }

    private void moveLocation(float delta) {

    }

    private void moveLongitude(float delta) {
        longitude = longitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourcesChange(1, 5);
    }

    public float getLatitude() {
        latitude += 1;
        DebugLog.d("MyLocation.getLatitude==> latitude:" + latitude);
        return latitude;
    }

    public float getLongitude() {
        longitude += 1;
        DebugLog.d("MyLocation.getLongitude==> longitude:" + longitude);
        return longitude;
    }

    public Date getTimestamp() {
        //return timestamp;
        return new Date();
    }
}