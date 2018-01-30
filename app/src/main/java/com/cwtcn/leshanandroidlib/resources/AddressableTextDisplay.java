package com.cwtcn.leshanandroidlib.resources;

import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnWriteReadListener;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class AddressableTextDisplay extends ExtendBaseInstanceEnabler {


    public static final int OBJECT_ID_ADDRESSABLE_TEXT_DISPLAY = 3341;
    public static final int TEXT = 5527;
    public static final int CLEAR_DISPLAY = 5530;

    private String mText;

    private OnWriteReadListener mOnWriteNotifyPeriodListener;

    public void setOnWriteNotifyPeriodListener(OnWriteReadListener listener) {
        this.mOnWriteNotifyPeriodListener = listener;
    }

    @Override
    public synchronized ReadResponse read(int resourceId) {
        switch (resourceId) {
            case TEXT:
                return ReadResponse.success(resourceId, mText);
            default:
                return super.read(resourceId);
        }
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        switch (resourceid) {
            case TEXT:
                DebugLog.d("write resource id  = " + resourceid + ", value = " + value.toString());
                return WriteResponse.success();
            default:
                return super.write(resourceid, value);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(int resourceId, String params) {
        DebugLog.d("execute params ==> " + params);
        switch (resourceId) {
            case CLEAR_DISPLAY:
                DebugLog.d("execute params = " + params);
                return ExecuteResponse.success() ;
            default:
                return super.execute(resourceId, params);
        }
    }

}
