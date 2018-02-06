package com.cwtcn.leshanandroidlib.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.cwtcn.leshanandroidlib.resources.ContactList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by leizhiheng on 2018/2/5.
 */

public class ContactListUtils {
    private Context mContext;

    public ContactListUtils(Context context) {
        this.mContext = context;
    }

    /**
     * 根据电话号码和姓名新增一个联系人
     * @param name
     * @param phoneNumber
     * @param isWhiteList 改号码是否是属于白名单。
     */
    public void addContact(String name, String phoneNumber, boolean isWhiteList) {
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
        //是否是白名单
//        values.put(ContactsContract.CommonDataKinds.Phone.DATA4, isWhiteList ? "1" : "0");
        // 向联系人URI添加联系人名字
        mContext.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
        values.clear();

        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        // 联系人的电话号码
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
        // 联系人名字
        values.put(ContactsContract.CommonDataKinds.Phone.DATA3, name);
        //是否是白名单
        values.put(ContactsContract.CommonDataKinds.Phone.DATA4, isWhiteList ? "1" : "0");
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
    public List<ContactBean> getContacts(Uri uri, String selection, String []selectionArgs) {
        ArrayList<ContactBean> contacts = new ArrayList<ContactBean>();
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = cr.query(uri, new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DATA3,//data3是联系人姓名
                        ContactsContract.CommonDataKinds.Phone.DATA4}, selection, selectionArgs,  null);
        while (cursor.moveToNext()) {
            ContactBean bean = new ContactBean(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA3)), cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
            contacts.add(bean);
        }
        cursor.close();
        return contacts;
    }

    /**
     * 删除联系人
     * @return
     */
    public int deleteContacts(Uri uri, String selection, String[] selectionArgs) {
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = cr.query(uri, new String[] {ContactsContract.Data.RAW_CONTACT_ID}, selection, selectionArgs,  null);
        int count = cursor.getCount();
        while (cursor.moveToNext()) {
            int rawContactIdIndex = cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID);
            String rawContactId = cursor.getString(rawContactIdIndex);
            //第一步先删除Contacts表中的数据
            int lineId = cr.delete(ContactsContract.Contacts.CONTENT_URI, ContactsContract.Contacts.NAME_RAW_CONTACT_ID + " =?", new String[]{rawContactId});
            //第二步再删除RawContacts表的数据
            int lineContactId = cr.delete(ContactsContract.RawContacts.CONTENT_URI, ContactsContract.Contacts._ID + " =?", new String[]{rawContactId});
            //第三部删除data表中的数据
            int dataId = cr.delete(ContactsContract.Data.CONTENT_URI, ContactsContract.Data.RAW_CONTACT_ID + " =?", new String[]{rawContactId});
        }
        cursor.close();
        return count;
    }

    /**
     * 根据电话号码删除联系人
     *
     * @param number
     */
    public boolean deleteContact(String number) {
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
                new String[]{ContactsContract.Data.RAW_CONTACT_ID},
                ContactsContract.CommonDataKinds.Phone.NUMBER +"=?", new String[] {number}, null);
//                ContactsContract.CommonDataKinds.Phone.NUMBER +
//                        "=?", new String[]{number}, null);

        DebugLog.d("getContactId cursor count = " + cursor.getCount());
        while (cursor.moveToNext()) {
            id = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
        }
        return id;
    }

    public static class ContactBean {
        public String name;
        public String mobile;
        public boolean isWhiteList;

        public ContactBean(String name, String mobile, boolean isWhiteList) {
            this.name = name;
            this.mobile = mobile;
            this.isWhiteList = isWhiteList;
        }

        public ContactBean(String name, String mobile) {
            this(name, mobile, false);
        }
    }

    public static JSONArray contacts2array (List<ContactBean> list) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            JSONObject object = new JSONObject();
            ContactBean bean = list.get(i);
            try {
                object.put("name", bean.name);
                object.put("mobile", bean.mobile);
                array.put(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    public static List<ContactBean> array2contacts(JSONArray array) {
        List<ContactBean> list  = new ArrayList<ContactBean>();
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject object = array.getJSONObject(i);
                list.add(new ContactBean(object.getString("name"), object.getString("mobile")));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return list;
    }
}
