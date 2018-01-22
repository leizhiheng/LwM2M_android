package com.cwtcn.leshanandroidlib.resources;

import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;

/**
 * Created by leizhiheng on 2018/1/19.
 */

public class ExtendObjectsInitializer extends ObjectsInitializer {
    public ExtendObjectsInitializer(LwM2mModel lwM2mModel) {
        super(lwM2mModel);
    }

    @Override
    public void setInstancesForObject(int objectId, LwM2mInstanceEnabler... instances) {
        /*
         * 原先的ObjectsInitializer中记录LwM2mInstanceEnabler的实例时没有将ObjectId写入到实例中。
         * 导致在实例中无法获取所属Object的id，也就无法执行一些操作，比如：在实例中根据ObjectId去获取IntervalInSec的值
         */
        for (LwM2mInstanceEnabler enabler: instances) {
            if (enabler instanceof  ExtendBaseInstanceEnabler) {
                ((ExtendBaseInstanceEnabler) enabler).setObjectId(objectId);
            }
        }
        super.setInstancesForObject(objectId, instances);
    }
}
