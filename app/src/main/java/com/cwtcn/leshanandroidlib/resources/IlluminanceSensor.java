package com.cwtcn.leshanandroidlib.resources;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.response.ReadResponse;

/**
 * Created by leizhiheng on 2017/12/29.
 */

public class IlluminanceSensor extends BaseInstanceEnabler {
    public static int OJBECTS_ID_ILLUMINANCE = 3301;

    @Override
    public ReadResponse read(int resourceid) {
        return super.read(resourceid);
    }
}
