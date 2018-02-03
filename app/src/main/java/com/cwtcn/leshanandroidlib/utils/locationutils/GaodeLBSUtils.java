package com.cwtcn.leshanandroidlib.utils.locationutils;

import android.content.Context;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnLocateResultListener;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by leizhiheng on 2018/2/3.
 */

public class GaodeLBSUtils {
    private Context mContext;
    private OnLocateResultListener mListener;
    public GaodeLBSUtils(Context context, OnLocateResultListener listener) {
        mContext = context;
        this.mListener = listener;
        initLoc();
    }

    long startLocTime, endLocTime;
    //声明AMapLocationClient类对象
    AMapLocationClient mLocationClient = null;
    AMapLocationClientOption mLocationOption;
    //声明定位回调监听器
    public AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            endLocTime = new Date().getTime();

            DebugLog.d("locate time  = " + (endLocTime - startLocTime));
            if (amapLocation != null) {
                if (amapLocation.getErrorCode() == 0) {
                    //可在其中解析amapLocation获取相应内容。
                    int type = amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                    double latitude = amapLocation.getLatitude();//获取纬度
                    double longitude = amapLocation.getLongitude();//获取经度
                    float accuracy = amapLocation.getAccuracy();//获取精度信息
                    String address = amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                    String country = amapLocation.getCountry();//国家信息
                    String province = amapLocation.getProvince();//省信息
                    String city = amapLocation.getCity();//城市信息
                    String district = amapLocation.getDistrict();//城区信息
                    String street = amapLocation.getStreet();//街道信息
                    String streetNum = amapLocation.getStreetNum();//街道门牌号信息
                    String cityCode = amapLocation.getCityCode();//城市编码
                    String adCode = amapLocation.getAdCode();//地区编码
                    //String aoiName = amapLocation.getAoiName();//获取当前定位点的AOI信息
                    //amapLocation.getBuildingId();//获取当前室内定位的建筑物Id
                    //amapLocation.getFloor();//获取当前室内定位的楼层
                    //amapLocation.getGpsStatus();//获取GPS的当前状态
                    //获取定位时间
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date(amapLocation.getTime());
                    df.format(date);

                    mListener.onLocateResult(true, latitude, longitude, String.valueOf(accuracy));


                    StringBuffer buffer = new StringBuffer();
                    buffer.append("定位信息：");
                    buffer.append("\ntype:" + type);
                    buffer.append("\nlatitude:" + latitude);
                    buffer.append("\nlongitude:" + longitude);
                    buffer.append("\naccuracy:" + accuracy);
                    buffer.append("\naddress:" + address);
                    buffer.append("\ncountry:" + country);
                    buffer.append("\nprovince:" + province);
                    buffer.append("\ncity:" + city);
                    buffer.append("\ndistrict:" + district);
                    buffer.append("\ngetLocationType:" + type);
                    buffer.append("\nstreet" + street);
                    DebugLog.d("Location result " + buffer.toString());
                    Toast.makeText(mContext, buffer.toString(), Toast.LENGTH_LONG).show();
                } else {
                    mListener.onLocateResult(false, -1, -1, null);

                    //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                    DebugLog.e("AmapError: location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                    Toast.makeText(mContext, "AmapError: location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo(), Toast.LENGTH_LONG).show();
                }
            } else {
                mListener.onLocateResult(false, -1, -1, null);
            }

            mLocationClient.stopLocation();//停止定位后，本地定位服务并不会被销毁
        }
    };

    /**
     * 开启高德定位
     * 注意：
     * 》目前手机设备在长时间黑屏或锁屏时CPU会休眠，这导致定位SDK不能正常进行位置更新。若您有锁屏状态下获取位置的需求，您可以应用alarmManager实现1个可叫醒CPU的Timer，定时请求定位。
     》使用定位SDK务必要注册GPS和网络的使用权限。
     》在使用定位SDK时，请尽量保证网络畅通，如获取网络定位，地址信息等都需要设备可以正常接入网络。
     》定位SDK在国内返回高德类型坐标，海外定位将返回GPS坐标。
     */
    private void initLoc() {
        DebugLog.d("initLoc==>");
        //初始化定位
        mLocationClient = new AMapLocationClient(mContext.getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);
        mLocationOption = new AMapLocationClientOption();

        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式:会同时使用网络定位和GPS定位，优先返回最高精度的定位结果，以及对应的地址描述信息
//        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位模式为AMapLocationMode.Battery_Saving，低功耗模式:不会使用GPS和其他传感器，只会使用网络定位（Wi-Fi和基站定位）；
        //mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
        //设置定位模式为AMapLocationMode.Device_Sensors，仅设备模式:不需要连接网络，只使用GPS进行定位，这种模式下不支持室内环境的定位，自 v2.9.0 版本支持返回地址描述信息。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Device_Sensors);


        //如果您需要使用单次定位，需要进行如下设置：
        //获取一次定位结果：
        //该方法默认为false。
        mLocationOption.setOnceLocation(true);
        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        mLocationOption.setOnceLocationLatest(true);


        //SDK默认采用连续定位模式，时间间隔2000ms。如果您需要自定义调用间隔：
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        //mLocationOption.setInterval(1000);

        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);

        //设置是否允许模拟位置,默认为true，允许模拟位置
        //mLocationOption.setMockEnable(true);


        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.setHttpTimeOut(20000);

        //关闭缓存机制.当开启定位缓存功能，在高精度模式和低功耗模式下进行的网络定位结果均会生成本地缓存，不区分单次定位还是连续定位。GPS定位结果不会被缓存。
        mLocationOption.setLocationCacheEnable(false);
    }

    public void startLoc() {
        DebugLog.d("startLoc==>");
        startLocTime = new Date().getTime();
        /**
         * 设置定位场景，目前支持三种场景（签到、出行、运动，默认无场景）
         */
        mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);
        if (null != mLocationClient) {
            mLocationClient.setLocationOption(mLocationOption);
            //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
            mLocationClient.stopLocation();
            mLocationClient.startLocation();
        }
    }

    public void stopLoc() {
        DebugLog.d("stopLoc==>");
        //mLocationClient.stopLocation();//停止定位后，本地定位服务并不会被销毁
        mLocationClient.onDestroy();//销毁定位客户端，同时销毁本地定位服务。
    }
}
