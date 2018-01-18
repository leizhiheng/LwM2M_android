package com.cwtcn.leshanandroidlib.resources;

import com.cwtcn.leshanandroidlib.utils.DebugLog;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.util.NamedThreadFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AddressableTextDisplay extends ExtendBaseInstanceEnabler {


    public static final int OBJECT_ID_ADDRESSABLE_TEXT_DISPLAY = 3341;
    public static final int TEXT = 5527;

    private String mText;

    public AddressableTextDisplay() {
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
        switch (resourceId) {

        default:
            return super.execute(resourceId, params);
        }
    }
}
