package com.taobao.weex.devtools.inspector.network.utils;

import java.util.Map;

/**
 * Created by moxun on 17/5/18.
 */

public class ExtractUtil {
    public static <T> T getValue(Map<String, Object> data, String key, T defValue) {
        if (key != null) {
            if (data.get(key) != null) {
                try {
                    return (T) data.get(key);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        return defValue;
    }
}
