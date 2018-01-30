package com.cwtcn.leshanandroidlib.resources;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.cwtcn.leshanandroidlib.utils.DebugLog;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ContactList extends ExtendBaseInstanceEnabler {
    public static final int EVENT_IDENTIFIER = 5823;
    public static final int TEXT = 5527;

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
                try {
                    //{"cmd":"delete","numbers":["15112301111","15112301112","15112301113"]}
                    String contacts = (String) value.getValue();//createCmdJsonString();//
                    if(contacts != null && contacts.startsWith("\ufeff")) {
                        contacts =  contacts.substring(1);
                    }
                    JSONObject object = new JSONObject(contacts);
                    String cmd = object.getString("cmd");
                    JSONArray numbers = object.getJSONArray("numbers");
                    if ("add".equals(cmd)) {
                        insetContacts(numbers);
                    } else if ("delete".equals(cmd)) {
                        numbers = deleteContacts(numbers);
                        DebugLog.d("deleteContacts result resest number count:" + numbers.length());
                        if (numbers.length() > 0) {
                            return WriteResponse.badRequest("Can't delete numbers have not existed!");
                        } else {
                            return WriteResponse.success();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                DebugLog.d("write resource id  = " + resourceid + ", value = " + value.toString());
                return WriteResponse.success();
            default:
                return super.write(resourceid, value);
        }
    }

    private String createCmdJsonString() {
        //外层object
        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd", "add");
            //Contact数组
            JSONArray contactArray = new JSONArray();
            contactArray.put("15112301111");
            contactArray.put("15112301112");
            contactArray.put("15112301113");
            obj.put("numbers", contactArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        DebugLog.d("createCmdJsonString str:" + obj.toString());
        return obj.toString();
    }

    /**
     * 将服务器端的电话插入到数据库
     * @param numbers
     * @return
     */
    private boolean insetContacts(JSONArray numbers) {
        //contacts的格式如下：
//        {
//            "cmd": "add",   # or ‘delete’; ‘get’ and ‘update’ are not needed
//            "numbers": ["number1","number2"]
//        }
        try {
            //插入联系人
            for (int i = 0; i < numbers.length(); i++) {
                addContact("", numbers.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 根据电话号码和姓名新增一个联系人
     * @param name
     * @param phoneNumber
     */
    public void addContact(String name, String phoneNumber) {
        // 创建一个空的ContentValues
        ContentValues values = new ContentValues();

        // 向RawContacts.CONTENT_URI空值插入，
        // 先获取Android系统返回的rawContactId
        // 后面要基于此id插入值
        Uri rawContactUri = mContext.getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
        values.clear();

        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        // 内容类型
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        // 联系人名字
        values.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name);
        // 向联系人URI添加联系人名字
        mContext.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
        values.clear();

        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        // 联系人的电话号码
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
        // 电话类型
        values.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        // 向联系人电话号码URI添加电话号码
        mContext.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
        values.clear();

//        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
//        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
//        // 联系人的Email地址
//        values.put(ContactsContract.CommonDataKinds.Email.DATA, "zhangphil@xxx.com");
//        // 电子邮件的类型
//        values.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
//        // 向联系人Email URI添加Email数据
//        mContext.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);

    }


    /**
     * 获取所有联系人的电话
     * @return
     */
    protected String getAllContacts() {
        ArrayList<String> contacts = new ArrayList<String>();
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = cr.query(
                ContactsContract.Data.CONTENT_URI, new String[] {"data1"}, "data2=?", new String[] {"2"},
                ContactsContract.Contacts._ID + " DESC");
        while (cursor.moveToNext()) {
            contacts.add(cursor.getString(cursor.getColumnIndex("data1")));
        }
        cursor.close();
        return contacts2json(contacts);
    }


    private String contacts2json(ArrayList<String> contacts) {
        //外层obj对象
        JSONObject objWrite = new JSONObject();

        //Contact数组
        JSONArray contactArray = new JSONArray();
        for (String number:contacts) {
            contactArray.put(number);
        }
        try {
            objWrite.put("numbers", contactArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println(objWrite);
        return objWrite.toString();
    }

    /**
     * 删除联系人
     *
     * @param numbers
     */
    private JSONArray deleteContacts(JSONArray numbers) {
//        String json = "{" + "\"cmd\": \"add\"," + "\"numbers\": [\"15112301829\", \"15112301230\"]" + "}";
        //contacts的格式如下：
//        {
//
//            "cmd": "add",   # or ‘delete’; ‘get’ and ‘update’ are not needed
//
//            "numbers": ["number1","number2"]
//
//        }
        try {
            //删除联系人
            for (int i = numbers.length() - 1; i >= 0; i--) {
                boolean result = deleteContact(numbers.getString(i));
                if (result) {
                    //如果数据库中存在这个号码，并且删除成功，则删除对应的JSONObject
                    numbers.remove(i);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            DebugLog.d("delete exception e:" + e.getMessage());
        }
        return numbers;
    }


    /**
     * 根据电话号码删除联系人
     *
     * @param number
     */
    private boolean deleteContact(String number) {
        //getContentResolver()是Activity的方法，若不在Activity需获得Activity的上下文。例如Context.getContentResolver()
        ContentResolver cr = mContext.getContentResolver();
        String contactId = getContactID(number);
        if ("-1".equals(contactId)) return false;

        //第一步先删除Contacts表中的数据
        int lineId = cr.delete(ContactsContract.Contacts.CONTENT_URI, ContactsContract.Contacts._ID + " =?", new String[]{contactId + ""});
        //第二步再删除RawContacts表的数据
        int lineContactId = cr.delete(ContactsContract.RawContacts.CONTENT_URI, ContactsContract.RawContacts.CONTACT_ID + " =?", new String[]{contactId + ""});

        return (lineId + lineContactId) != 0;
    }

    /**
     * 获取ContactId。删除联系人时需要这个ID
     */
    public String getContactID(String number) {
        String id = "-1";
        Cursor cursor = mContext.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                //查询数据表中的字段：data1字段可以是电话号码和名字；data2为2时，data1为电话号码
                new String[]{ContactsContract.Data.RAW_CONTACT_ID, "data1", "data2"},
                "data2=?", new String[] {"2"}, null);
//                ContactsContract.CommonDataKinds.Phone.NUMBER +
//                        "=?", new String[]{number}, null);

        DebugLog.d("getContactId cursor count = " + cursor.getCount());
        while (cursor.moveToNext()) {
            String data1 = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            if (number.equals(data1)) {
                id = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
                return id;
            }
        }
        return id;
    }
}
