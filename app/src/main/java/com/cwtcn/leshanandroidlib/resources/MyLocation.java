package com.cwtcn.leshanandroidlib.resources;

import android.util.Log;

import com.cwtcn.leshanandroidlib.utils.DebugLog;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.response.ReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Random;

public class MyLocation extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(MyLocation.class);

    private static final Random RANDOM = new Random();

    private float latitude;
    private float longitude;
    private float scaleFactor;
    private Date timestamp;

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

    public void updateLocation() {

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


    private void moveLongitude(float delta) {
        longitude = longitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourcesChange(1, 5);
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
        return timestamp;
    }
}