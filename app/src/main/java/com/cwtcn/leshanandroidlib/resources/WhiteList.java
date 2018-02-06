package com.cwtcn.leshanandroidlib.resources;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.cwtcn.leshanandroidlib.utils.ContactListUtils;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnWriteReadListener;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 白名单功能。这个功能和联系人功能(ContactList)是不一样的：只有白名单中的号码可以呼进；白名单和联系人中的号码都可以呼出。
 */
public class WhiteList extends ExtendBaseInstanceEnabler {
    public static final int EVENT_IDENTIFIER = 5823;
    public static final int TEXT = 5527;
    private ContactListUtils mUtils;

    @Override
    public void onCreate(Context context, int objectId, OnWriteReadListener onWriteReadListener) {
        super.onCreate(context, objectId, onWriteReadListener);
        mUtils = new ContactListUtils(context);
    }

    @Override
    public void onDestory() {

    }

    @Override
    public synchronized ReadResponse read(int resourceId) {
        switch (resourceId) {
            case TEXT:
                //读联系人信息，提交到服务器
                return ReadResponse.success(resourceId, getAllContacts());
            default:
                return super.read(resourceId);
        }
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        switch (resourceid) {
            case EVENT_IDENTIFIER:
                //{"cmd":"delete","numbers":["15112301111","15112301112","15112301113"]}
                String contacts = (String) value.getValue();//createCmdJsonString();//
                storeWhiteList(contacts);

                DebugLog.d("write resource id  = " + resourceid + ", value = " + value.toString());
                return WriteResponse.success();
            default:
                return super.write(resourceid, value);
        }
    }

    /**
     * 存储白名单
     * @param value 格式如下：
     {
    "count": 1,
    "result": [{
    "name": "string",

    "mobile": "string"
    }]
    }
     */
    private void storeWhiteList(String value) {

        //第一步：删除之前的白名单数据
        int countOld = mUtils.deleteContacts(ContactsContract.Data.CONTENT_URI, ContactsContract.CommonDataKinds.Phone.DATA4 + "=?", new String[] {"1"});

        /*
         * 第二步：插入新的白名单数据
         */
        List<ContactListUtils.ContactBean> beans = null;
        try {
            JSONObject object = new JSONObject(value);
            int count = object.optInt("count");
            JSONArray array = object.getJSONArray("result");
            beans = ContactListUtils.array2contacts(array);

            //将白名单插入到数据库
            for (int i = 0; i < beans.size(); i++) {
                ContactListUtils.ContactBean bean = beans.get(i);
                mUtils.addContact(bean.name, bean.mobile, true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取所有白名单联系人
     *
     * @return
     */
    protected String getAllContacts() {
        List<ContactListUtils.ContactBean> contacts = mUtils.getContacts(ContactsContract.Data.CONTENT_URI,
                ContactsContract.CommonDataKinds.Phone.DATA4 + "=? and " + ContactsContract.CommonDataKinds.Phone.DATA2 + "=?", new String[] {"1", "2"});
        JSONObject object = new JSONObject();
        JSONArray array = mUtils.contacts2array(contacts);
        try {
            object.put("count", array.length() + "");
            object.put("result", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }
}
