package com.guoxiaoxing.cuckoo.util;

import android.content.Context;

public class ConfigurationChecker {

    public static boolean checkBasicConfiguration(Context context) {
        return DataUtils.checkHasPermission(context, "android.permission.INTERNET");
    }
}
