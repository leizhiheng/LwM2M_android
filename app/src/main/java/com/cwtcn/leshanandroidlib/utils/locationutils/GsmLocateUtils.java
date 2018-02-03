package com.cwtcn.leshanandroidlib.utils.locationutils;

import android.content.Context;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.Toast;

import com.cwtcn.leshanandroidlib.utils.DebugLog;

import java.util.List;

/**
 * Created by leizhiheng on 2018/2/2.
 */
public class GsmLocateUtils {
    public static final String TAG = "GsmLocateUtils";
    private Context mContext;
    TelephonyManager mTelephonyManager;

    public GsmLocateUtils(Context context){
        this.mContext = context;
    }
    /**
     * 获取 基站 信息
     * @return
     */
    public String getBaseStationInformation(){
        if(mTelephonyManager==null){
            mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }
        // 返回值MCC + MNC （注意：电信的mnc 对应的是 sid）
        String operator = mTelephonyManager.getNetworkOperator();
        int mcc = -1;
        int mnc = -1;
        if(operator!=null&&operator.length()>3){
            mcc = Integer.parseInt(operator.substring(0, 3));
            mnc = Integer.parseInt(operator.substring(3));
        }

        // 获取邻区基站信息
        List<NeighboringCellInfo> infos = mTelephonyManager.getNeighboringCellInfo();
        StringBuffer sb = new StringBuffer("总数 : " + infos.size() + "\n");

        for (NeighboringCellInfo info1 : infos) { // 根据邻区总数进行循环
            sb.append(" LAC : " + info1.getLac()); // 取出当前邻区的LAC
            sb.append("\n CID : " + info1.getCid()); // 取出当前邻区的CID
            sb.append("\n BSSS : " + (-113 + 2 * info1.getRssi()) + "\n"); // 获取邻区基站信号强度
        }


        int type = mTelephonyManager.getNetworkType();

        Toast.makeText(mContext,"type:= "+type,Toast.LENGTH_LONG).show();
        //需要判断网络类型，因为获取数据的方法不一样
        if(type == TelephonyManager.NETWORK_TYPE_CDMA        // 电信cdma网
                || type == TelephonyManager.NETWORK_TYPE_1xRTT
                || type == TelephonyManager.NETWORK_TYPE_EVDO_0
                || type == TelephonyManager.NETWORK_TYPE_EVDO_A
                || type == TelephonyManager.NETWORK_TYPE_EVDO_B
                || type == TelephonyManager.NETWORK_TYPE_LTE){
            CdmaCellLocation cdma = (CdmaCellLocation) mTelephonyManager.getCellLocation();
            if(cdma!=null){
                sb.append(" MCC = " + mcc );
                sb.append("\n cdma.getBaseStationLatitude()"+cdma.getBaseStationLatitude()/14400 +"\n"
                        +"cdma.getBaseStationLongitude() "+cdma.getBaseStationLongitude()/14400 +"\n"
                        +"cdma.getBaseStationId()(cid)  "+cdma.getBaseStationId()
                        +"\n  cdma.getNetworkId()(lac)   "+cdma.getNetworkId()
                        +"\n  cdma.getSystemId()(mnc)   "+cdma.getSystemId());
            }else{
                sb.append("can not get the CdmaCellLocation");
            }

        }else if(type == TelephonyManager.NETWORK_TYPE_GPRS         // 移动和联通GSM网
                || type == TelephonyManager.NETWORK_TYPE_EDGE
                || type == TelephonyManager.NETWORK_TYPE_HSDPA
                || type == TelephonyManager.NETWORK_TYPE_UMTS
                || type == TelephonyManager.NETWORK_TYPE_LTE){
            GsmCellLocation gsm = (GsmCellLocation) mTelephonyManager.getCellLocation();
            if(gsm!=null){
                sb.append("  gsm.getCid()(cid)   "+gsm.getCid()+"  \n "//移动联通 cid
                        +"gsm.getLac()(lac) "+gsm.getLac()+"  \n "             //移动联通 lac
                        +"gsm.getPsc()  "+gsm.getPsc());
            }else{
                sb.append("can not get the GsmCellLocation");
            }
        }else if(type == TelephonyManager.NETWORK_TYPE_UNKNOWN){
            Toast.makeText(mContext,"电话卡不可用！",Toast.LENGTH_LONG).show();
        }

        logD("mTelephonyManager.getNetworkType(); "+mTelephonyManager.getNetworkType());
        logD(" 获取邻区基站信息:" + sb.toString());
        return sb.toString();
    }

    private void logD(String msg) {
        DebugLog.d(TAG + ":" + msg);
    }
}
