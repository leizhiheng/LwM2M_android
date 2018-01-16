package com.cwtcn.leshanandroidlib.model;

import java.io.Serializable;

/**
 * Created by leizhiheng on 2017/12/27.
 */
public class ResourceBean implements Serializable {
    public int id;
    public String name;
    public Object value = 10;
    public int objectId;
    public ValueType valueType;

    public ResourceBean(int id, String name, Object value, ValueType valueType) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.valueType = valueType;
    }

    public static enum ValueType{STRING, LONG, DOUBLE, LINK, DATE, BYTE, MAP};
}
