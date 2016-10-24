package com.alibaba.weex.commons.util;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

/**
 * 其他工具类
 * @author Senyu, email: senyu.ylj@alibaba-inc.com
 */
public class OtherUtil {
    /**
     * 判断当前设备是否是模拟器。如果返回TRUE，则当前是模拟器，不是返回FALSE
     */
    public static boolean isEmulator(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String imei = tm.getDeviceId();
            if (imei != null && imei.equals("000000000000000")) {
                return true;
            }
            return (Build.MODEL.equals("sdk"))
                    || (Build.MODEL.equals("google_sdk"));
        } catch (Exception e) {

        }
        return false;
    }

}
