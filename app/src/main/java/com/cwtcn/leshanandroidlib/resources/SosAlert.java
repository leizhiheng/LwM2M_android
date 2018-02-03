package com.cwtcn.leshanandroidlib.resources;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.cwtcn.leshanandroidlib.utils.DebugLog;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**

 */
public class SosAlert extends ExtendBaseInstanceEnabler {
    public static final int TEXT = 5527;
    private String mSosAlertMsg;

    @Override
    public void onCreate(Context context) {
        mContext = context;
        registerSosReceiver();
    }

    @Override
    public void onDestory() {
        mContext.unregisterReceiver(mSosReceiver);
    }

    private BroadcastReceiver mSosReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.abardeen.action.VolDownLongPress".equals(intent.getAction())) {
                //收到Sos广播后先获取地理位置
                mOnWriteReadListener.requestLocate(objectId, SosAlert.this);
            }
        }
    };

    private void registerSosReceiver() {
        //如果长按音量键5秒，PhoneWindowManager.java会发送一个广播，Action:com.abardeen.action.VolDownLongPress,用于触发SOS报警。
        //这里监听这个广播
        IntentFilter filter = new IntentFilter("com.abardeen.action.VolDownLongPress");
        mContext.registerReceiver(mSosReceiver, filter);

    }

    @Override
    public synchronized ReadResponse read(int resourceId) {
        switch (resourceId) {
            case TEXT:
                //提交紧急求救信息
                return ReadResponse.success(resourceId, getSosAlertMsg());
            default:
                return super.read(resourceId);
        }
    }

    @Override
    public void setLocateResult(double lat, double lon, String accuracy) {
        super.setLocateResult(lat, lon, accuracy);
        //定位成功后提交Sos信息到服务器
        produceSosAlertMsg(lat, lon, accuracy);
    }

    private void produceSosAlertMsg(double lat, double lon, String accuracy) {
        JSONObject object = new JSONObject();
        try {
            object.put("latitude", String.valueOf(lat));
            object.put("longitude", String.valueOf(lon));
            object.put("accuracy", accuracy);
            object.put("timestamp", String.valueOf(new Date().getTime()));
            mSosAlertMsg = object.toString();
            DebugLog.d("sos alert msg:" + mSosAlertMsg);
            fireResourcesChange(TEXT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * SOS信息应该包含两部分：位置信息和当前时间，格式如下：
     *
     {
        "latitude": "23.82893",
        "longitude": "-178.34348"
        "timestap": "2018-1-31 13:10:10"
     }
     * @return
     */
    private String getSosAlertMsg() {

        return mSosAlertMsg == null ? "Waiting to get sos alert!" : mSosAlertMsg;
    }
}
