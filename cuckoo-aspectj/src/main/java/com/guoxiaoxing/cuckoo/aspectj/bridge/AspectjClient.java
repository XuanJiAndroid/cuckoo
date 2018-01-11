package com.guoxiaoxing.cuckoo.aspectj.bridge;

import android.text.TextUtils;

import org.aspectj.lang.JoinPoint;

import java.lang.reflect.Method;

public class AspectjClient {
    private static Class clazz;
    private static Object object;

    public static void invokeMethodFromServer3(final JoinPoint joinPoint, final String methodName, Object result) {
        try {
            if (joinPoint == null || TextUtils.isEmpty(methodName)) {
                return;
            }

            if (clazz == null) {
                clazz = Class.forName("com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjServer");
            }
            if (clazz == null) {
                return;
            }

            if (object == null) {
                object = clazz.newInstance();
            }
            if (object == null) {
                return;
            }

            Class[] params = new Class[2];
            params[0] = JoinPoint.class;
            params[1] = Object.class;

            Method method = clazz.getDeclaredMethod(methodName, params);
            if (method == null) {
                return;
            }

            method.invoke(object, joinPoint, result);
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    public static void invokeMethodFromServer2(final JoinPoint joinPoint, final String methodName, int menuItemIndex) {
        try {
            if (joinPoint == null || TextUtils.isEmpty(methodName)) {
                return;
            }

            if (clazz == null) {
                clazz = Class.forName("com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjServer");
            }
            if (clazz == null) {
                return;
            }

            if (object == null) {
                object = clazz.newInstance();
            }
            if (object == null) {
                return;
            }

            Class[] params = new Class[2];
            params[0] = JoinPoint.class;
            params[1] = Integer.class;

            Method method = clazz.getDeclaredMethod(methodName, params);
            if (method == null) {
                return;
            }

            method.invoke(object, joinPoint, menuItemIndex);
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    public static void invokeMethodFromServer(final JoinPoint joinPoint, final String methodName) {
        try {
            if (joinPoint == null || TextUtils.isEmpty(methodName)) {
                return;
            }

            if (clazz == null) {
                clazz = Class.forName("com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjServer");
            }
            if (clazz == null) {
                return;
            }

            if (object == null) {
                object = clazz.newInstance();
            }
            if (object == null) {
                return;
            }

            Class[] params = new Class[1];
            params[0] = JoinPoint.class;

            Method method = clazz.getDeclaredMethod(methodName, params);
            if (method == null) {
                return;
            }

            method.invoke(object, joinPoint);
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }
}
