package com.cwtcn.leshanandroidlib.resources;

import android.content.Context;
import android.widget.Toast;

import com.cwtcn.leshanandroidlib.utils.DebugLog;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

/**

 */
public class BatteryStatus extends ExtendBaseInstanceEnabler {
    public static final int BATTERY_SHRESHOLD = 5601;
    public static final int RATE_OF_REDUCE = 5524;
    public static final int CURRENT_BATTERY_LEVEL = 5527;

    @Override
    public void onCreate(Context context) {

    }

    @Override
    public void onDestory() {

    }

    @Override
    public synchronized ReadResponse read(int resourceId) {
        switch (resourceId) {
            case CURRENT_BATTERY_LEVEL:
                //提交剩余电量值
                return ReadResponse.success(resourceId, getBatteryLevel());
            case BATTERY_SHRESHOLD:
                return ReadResponse.success(resourceId, getBatteryThreshold());
            case RATE_OF_REDUCE:
                return ReadResponse.success(resourceId, getReduceRate());
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
//                String shreshold = (String) value.getValue();
//                DebugLog.d("shreshold:" + shreshold);
//                setBatteryShreshold(shreshold);
//                Toast.makeText(mContext, "settingMsg:" + shreshold, Toast.LENGTH_LONG).show();
                return WriteResponse.success();
            case RATE_OF_REDUCE:
                //设置上报电量的间隔
//                String rate = (String) value.getValue();
//                setRateOfReduce(rate);
                return WriteResponse.success();
            default:
                return super.write(resourceid, value);
        }
    }

    private void setBatteryShreshold(String value) {

    }

    private void setRateOfReduce(String value) {

    }

    private float getBatteryThreshold() {

        return 20;
    }

    private float getReduceRate() {
        return 5;
    }

    private String getBatteryLevel() {
        return 30 + "";
    }
}
