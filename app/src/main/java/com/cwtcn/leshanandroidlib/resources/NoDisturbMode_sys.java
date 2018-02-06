package com.cwtcn.leshanandroidlib.resources;

import android.app.NotificationManager;
import android.content.Context;
//import android.content.ComponentName;
//import android.net.Uri;
//import android.provider.Settings;
//import android.service.notification.ZenModeConfig;
//import android.service.notification.ZenModeConfig.EventInfo;
//import android.service.notification.ZenModeConfig.ScheduleInfo;
//import android.service.notification.ZenModeConfig.ZenRule;

import com.cwtcn.leshanandroidlib.utils.DebugLog;
import com.cwtcn.leshanandroidlib.utils.interfaces.OnWriteReadListener;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 这个类用于实现免打扰模式
 1、服务器对这个Object中的某个Text Resource执行Write操作时，向Client发送命令，可以设置免打扰时间段和一周中的生效日期。命令是一个json字符串，格式如下：
 {
 "sec1": "07:00-09:30",
 "sec2": "10:00-13:40",
 "repeatExpression": "0111110"
 }
 sec1：表示第一个免打扰时间段
 sec2：表示第二个免打扰时间段
 repeatExpression:表示一周中的那些天，免打扰模式是生效的。从周日开始，0表示免打扰模式不生效，1表示免打扰模式生效。

 2、服务器可以对Object中的Text Resource执行Read操作，Client接收到后向服务器返回当前的免打扰设置信息。

 */
public class NoDisturbMode_sys extends ExtendBaseInstanceEnabler {
    public static final String KEY_NO_DISTURB_MODE_MSG = "NoDisturbModeMsg";
    public static final int EVENT_IDENTIFIER = 5823;
    public static final int TEXT = 5527;
//    public static final String ACTION = Settings.ACTION_ZEN_MODE_SCHEDULE_RULE_SETTINGS;

    @Override
    public void onCreate(Context context, int objectId, OnWriteReadListener onWriteReadListener) {
        super.onCreate(context, objectId, onWriteReadListener);
    }

    @Override
    public void onDestory() {

    }

    @Override
    public synchronized ReadResponse read(int resourceId) {
        switch (resourceId) {
            case TEXT:
                //读联系人信息，提交到服务器
                return ReadResponse.success(resourceId, getNoDisturbModeMsg());
            default:
                return super.read(resourceId);
        }
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        switch (resourceid) {
            case TEXT:
                String settingMsg = (String) value.getValue();
                DebugLog.d("settingMsg:" + settingMsg);
//                setNonDisturbMode(settingMsg);
                return WriteResponse.success();
            default:
                return super.write(resourceid, value);
        }
    }

    /**
     * 获取免打扰模式的设置信息，以如下格式返回
     {
     "sec1": "07:00-09:30",
     "sec2": "10:00-13:40",
     "repeatExpression": "0111110"
     }
     * @return
     */
    private String getNoDisturbModeMsg() {
        return mOnWriteReadListener.getStringFromPreferemce(KEY_NO_DISTURB_MODE_MSG, null);
    }

    /**
     * 根据设置Write过来的信息设置免打扰模式。
     * 设置以后可以跳转到Settings页面查看设置是否设置成功，跳转代码如下：
     Intent intent = new Intent("android.settings.ZEN_MODE_SETTINGS");
     startActivity(intent);
     * @param settingMsg
     */
//
//    public void setNonDisturbMode(String settingMsg) {
//        DebugLog.d("setNonDisturbMode == >");
//        settingMsg = "{\"sec1\": \"07:00-09:30\",\"sec2\": \"10:00-13:40\",\"repeatExpression\": \"0111110\"}";
//        //mOnWriteReadListener.setStringToPreference(KEY_NO_DISTURB_MODE_MSG, settingMsg);
//        setZenModeConfig(getZenModeConfig(settingMsg));
//    }
//
//
//    protected boolean setZenModeConfig(ZenModeConfig config) {
//
//        final String reason = getClass().getSimpleName();
//        final boolean success = NotificationManager.from(mContext).setZenModeConfig(config, reason);
//        if (success) {
//            DebugLog.d("Saved mConfig=" + config);
//        }
//        return success;
//    }
//
//    private ZenModeConfig getZenModeConfig(String settingMsg) {
//        DebugLog.d("getZenModeConfig == >");
//        try {
//            JSONObject jsonObject = new JSONObject(settingMsg);
//            String sec1 = jsonObject.getString("sec1");
//            String sec2 = jsonObject.getString("sec2");
//            String repeatExpression = jsonObject.getString("repeatExpression");
//
//            RuleInfo info1 = getRuleInfo(sec1, repeatExpression);
//            RuleInfo info2 = getRuleInfo(sec2, repeatExpression);
//
//            ZenRule rule1 = getZenRule(info1, "sec1");
//            ZenRule rule2 = getZenRule(info2, "sec2");
//
//            final ZenModeConfig newConfig = new ZenModeConfig();
//            final String ruleId1 = newConfig.newRuleId();
//            final String ruleId2 = newConfig.newRuleId();
//            newConfig.automaticRules.put(ruleId1, rule1);
//            newConfig.automaticRules.put(ruleId2, rule2);
//
//            return newConfig;
//        } catch (JSONException e) {
//            e.printStackTrace();
//            DebugLog.d("getZenModeConfig failed, e:" + e.getMessage());
//        }
//
//        return null;
//    }
//
//    private ZenRule getZenRule(RuleInfo ri, String name) {
//        final ZenRule rule = new ZenRule();
//        rule.name = name;
//        rule.enabled = true;
//        rule.zenMode = Settings.Global.ZEN_MODE_ALARMS;
//        rule.conditionId = ri.defaultConditionId;
//        rule.component = null;
//        DebugLog.d("getZenRule ZenRule:" + rule);
//
//        return rule;
//    }
//
//    /**
//     *
//     * @param time 防干扰的一个时间段，格式："07:00-09:30"
//     * @param repeat 一周中的重复日期，格式："0111110"， "0"表示关闭；"1"表示开启
//     * @return
//     */
//    private RuleInfo getRuleInfo(String time, String repeat) {
//        String times[] = time.split("-");
//        String startTimeParts[] = times[0].split(":");
//        String endTimeParts[] = times[1].split(":");
//
//        final ScheduleInfo schedule = new ScheduleInfo();
//        schedule.days = getRepeatWeekDay(repeat);//ZenModeConfig.ALL_DAYS;
//        schedule.startHour = Integer.valueOf(startTimeParts[0]);
//        schedule.startMinute = Integer.valueOf(startTimeParts[1]);
//        schedule.endHour = Integer.valueOf(endTimeParts[0]);
//        schedule.endMinute = Integer.valueOf(endTimeParts[1]);
//        final RuleInfo rt = new RuleInfo();
//        rt.settingsAction = ACTION;
//        rt.defaultConditionId = ZenModeConfig.toScheduleConditionId(schedule);
//        DebugLog.d("getRuleInfo ruluInfor:" + rt);
//
//        return rt;
//    }
//
//    private int[] getRepeatWeekDay(String repeat) {
//        char [] rChars = repeat.toCharArray();
//        ArrayList<Integer> temp = new ArrayList<Integer>();
//        for (char i = 0; i < rChars.length; i++) {
//            int k = rChars[i] - '0';
//            if (k == 1) {
//                temp.add(i + 1);
//            }
//        }
//        int [] days = new int[temp.size()];
//        for (int i = 0; i < temp.size(); i++) {
//            days[i] = temp.get(i);
//        }
//        return days;
//
//    }
//
//    public static class RuleInfo {
//        public String caption;
//        public String settingsAction;
//        public Uri defaultConditionId;
//    }
}
