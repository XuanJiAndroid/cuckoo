package com.guoxiaoxing.cuckoo.util;

import android.util.Log;

import com.guoxiaoxing.cuckoo.Cuckoo;

public class LogUtils {

    private static Cuckoo mCuckoo;

    private LogUtils() {

    }

    public static void init(Cuckoo cuckoo) {
        mCuckoo = cuckoo;
    }

    public static void d(String tag, String msg) {
        try {
            if (mCuckoo.isDebugMode()) {
                Log.i(tag, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        try {
            if (mCuckoo.isDebugMode()) {
                Log.i(tag, msg, tr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void i(String tag, String msg) {
        try {
            if (Cuckoo.ENABLE_LOG) {
                Log.i(tag, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void i(String tag, Throwable tr) {
        try {
            if (Cuckoo.ENABLE_LOG) {
                Log.i(tag, "", tr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void i(String tag, String msg, Throwable tr) {
        try {
            if (Cuckoo.ENABLE_LOG) {
                Log.i(tag, msg, tr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
