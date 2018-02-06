package com.cwtcn.leshanandroidlib.resources;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnWriteReadListener;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**

 */
public class SosAlert extends ExtendBaseInstanceEnabler {
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

    @Override
    public void onCreate(Context context, int objectId, OnWriteReadListener onWriteReadListener) {
        super.onCreate(context, objectId, onWriteReadListener);
        registerSosReceiver();
        //先获取一次位置信息。
        mOnWriteReadListener.requestLocate(objectId, this);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mOnWriteReadListener.requestLocate(objectId, SosAlert.this);
        }
    };
    private void registerSosReceiver() {
        IntentFilter filter = new IntentFilter("com.abardeen.action.VolDownLongPress");
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestory() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public ReadResponse read(int resourceid) {
        DebugLog.d("leshan.MyLocation.read() resourceId = " + resourceid);
        switch (resourceid) {
            case 0://读取经度
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
    public void setLocateResult(double lat, double lon, float accuracy) {
        this.latitude = (float) lat;
        this.longitude = (float) lon;
        this.accuracy = accuracy;
        fireResourcesChange(0, 1, 3, 5);
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
