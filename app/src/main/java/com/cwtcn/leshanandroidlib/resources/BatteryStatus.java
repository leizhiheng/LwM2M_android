package com.cwtcn.leshanandroidlib.resources;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.widget.Toast;

import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnWriteReadListener;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

import java.util.Date;

/**

 */
public class BatteryStatus extends ExtendBaseInstanceEnabler {
    private static final String KEY_BATTERY_SHRESHOLD = "battery.shreshold";
    private static final String KEY_RATE_OF_REDUCE = "battery.rate.of.reduce";
    public static final int BATTERY_SHRESHOLD = 5601;
    public static final int RATE_OF_REDUCE = 5524;
    public static final int CURRENT_BATTERY_LEVEL = 5527;
    public static final int TIME_STAMP = 5;

    /**
     * 上次提交到服务器的电量百分比
     */
    private float percentageFiredBefore = 100;
    /**
     * 当前电量值
     */
    private float batteryLevel;
    /**
     * 低电量报警阈值,数据类型是证整数
     */
    private int shreshold;
    /**
     * 每低于报警阈值一个rateOfReduce的电量百分比，则报警一次。
     * 默认为0,表示只在电量达到阈值的时候报警一次。
     */
    private int rateOfReduce;

    @Override
    public void onCreate(Context context, int objectId, OnWriteReadListener onWriteReadListener) {
        super.onCreate(context, objectId, onWriteReadListener);
        registerBatteryStatusReceiver();
        shreshold = getBatteryThreshold();
        rateOfReduce = getReduceRate();
    }

    @Override
    public void onDestory() {
        mContext.unregisterReceiver(mReciever);
    }

    private BroadcastReceiver mReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (shreshold < 0) return;

            //当前剩余电量
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            //电量最大值
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            DebugLog.d("battery level = " + batteryLevel + ", scale = " + scale  + ", shreshold:" + shreshold + ", rateOfReduce:" + rateOfReduce);

            if (batteryLevel == shreshold && percentageFiredBefore > shreshold) {
                fireResourcesChange(CURRENT_BATTERY_LEVEL, TIME_STAMP);
                if (rateOfReduce == 0) {
                    shreshold = -1;
                } else {
                    percentageFiredBefore = shreshold;
                }
            } else if (batteryLevel < shreshold) {
                if ((percentageFiredBefore - batteryLevel) == shreshold) {
                    percentageFiredBefore = batteryLevel;
                    //如果上次提交的电量值减去当前
                    fireResourcesChange(CURRENT_BATTERY_LEVEL, TIME_STAMP);
                }
            }
        }
    };

    /**
     * 注册电量改变广播接收器
     */
    private void registerBatteryStatusReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mReciever, filter);
    }

    @Override
    public synchronized ReadResponse read(int resourceId) {
        switch (resourceId) {
            case CURRENT_BATTERY_LEVEL:
                //读取剩余电量值
                return ReadResponse.success(resourceId, getBatteryLevel());
            case BATTERY_SHRESHOLD:
                //读取设置的报警阈值
                return ReadResponse.success(resourceId, getBatteryThreshold());
            case RATE_OF_REDUCE:
                //设置每次报警的电量百分比间隔
                return ReadResponse.success(resourceId, getReduceRate());
            case TIME_STAMP:
                //设置时间戳
                return ReadResponse.success(resourceId, getTimeStamp());
            default:
                return super.read(resourceId);
        }
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        DebugLog.d("resource id = " + resourceid + ", value = " + value.toString());
        switch (resourceid) {
            case BATTERY_SHRESHOLD:
                //设置低电量的阈值
                String shreshold = (String) value.getValue();
                setBatteryShreshold(shreshold);
                return WriteResponse.success();
            case RATE_OF_REDUCE:
                //设置上报电量的间隔
                String rate = (String) value.getValue();
                setRateOfReduce(rate);
                return WriteResponse.success();
            default:
                return super.write(resourceid, value);
        }
    }

    private void setBatteryShreshold(String value) {
        mOnWriteReadListener.setStringToPreference(KEY_BATTERY_SHRESHOLD, value);
    }

    private void setRateOfReduce(String value) {
        mOnWriteReadListener.setStringToPreference(KEY_RATE_OF_REDUCE, value);
    }

    private int getBatteryThreshold() {
        //低电量报警的阈值默认设置为20%
        return Integer.valueOf(mOnWriteReadListener.getStringFromPreferemce(KEY_BATTERY_SHRESHOLD, "20"));
    }

    private int getReduceRate() {
        //报警的电量百分比间隔如果设置为0，就表示只在电量达到阈值的时候报警一次，之后不再报警
        return Integer.valueOf(mOnWriteReadListener.getStringFromPreferemce(KEY_BATTERY_SHRESHOLD, "0"));
    }

    private String getBatteryLevel() {
        return batteryLevel + "";
    }

    private Date getTimeStamp() {
        return new Date();
    }
}
