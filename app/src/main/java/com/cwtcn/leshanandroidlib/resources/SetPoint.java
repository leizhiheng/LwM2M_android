package com.cwtcn.leshanandroidlib.resources;

import com.cwtcn.leshanandroidlib.utils.DebugLog;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

import static org.eclipse.leshan.LwM2mId.LOCATION;

public class SetPoint extends ExtendBaseInstanceEnabler {


    public static final int OBJECT_ID_SET_POINT = 3308;
    public static final int SET_POINT_VALUE = 5900;

    private String mText;


    @Override
    public synchronized ReadResponse read(int resourceId) {
        switch (resourceId) {
        case SET_POINT_VALUE:
            DebugLog.d("read Thread:" + Thread.currentThread().getId());
            int period = -1;
            if (mOnWriteReadListener != null) {
                period = mOnWriteReadListener.readPeriod(LOCATION);
            }
            return ReadResponse.success(resourceId, period);
        default:
            return super.read(resourceId);
        }
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        switch (resourceid) {
            case SET_POINT_VALUE://用于写Location的IntervalInSec
                double period = Double.valueOf(value.getValue().toString());
                if (mOnWriteReadListener != null) {
                    mOnWriteReadListener.writedPeriod(LOCATION, (int) period);
                }
                DebugLog.d("write resource id  = " + resourceid + ", value = " + value.toString() + ", period = " + period);
                return WriteResponse.success();
            default:
                return super.write(resourceid, value);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(int resourceId, String params) {
        switch (resourceId) {

        default:
            return super.execute(resourceId, params);
        }
    }
}
