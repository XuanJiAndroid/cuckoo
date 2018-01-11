package com.guoxiaoxing.cuckoo.aspectj;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjConstants;
import com.guoxiaoxing.cuckoo.util.LogUtils;
import com.guoxiaoxing.cuckoo.util.ViewUtils;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

import java.lang.reflect.Method;

public class ReactNativeViewReal {
    private final static String TAG = "ReactNativeViewReal";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            if (!Cuckoo.with().isReactNativeAutoTrackEnabled()) {
                return;
            }

            //关闭 AutoTrack
            if (!Cuckoo.with().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (Cuckoo.with().isAutoTrackEventTypeIgnored(Cuckoo.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            int reactTag = (int) joinPoint.getArgs()[0];

            Object target = joinPoint.getTarget();
            JSONObject properties = new JSONObject();
            properties.put(AspectjConstants.ELEMENT_TYPE, "RNView");
            if (target != null) {
                Class<?> clazz = Class.forName("com.facebook.react.uimanager.NativeViewHierarchyManager");
                Method resolveViewMethod = clazz.getMethod("resolveView", int.class);
                if (resolveViewMethod != null) {
                    Object object = resolveViewMethod.invoke(target, reactTag);
                    if (object != null) {
                        View view = (View) object;
                        //获取所在的 Context
                        Context context = view.getContext();
                        if (context == null) {
//                        return;
                        }

                        //将 Context 转成 Activity
                        Activity activity = ViewUtils.getActivityFromContext(context, view);
                        //$screen_name & $title
                        if (activity != null) {
                            properties.put(AspectjConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                            String activityTitle = ViewUtils.getActivityTitle(activity);
                            if (!TextUtils.isEmpty(activityTitle)) {
                                properties.put(AspectjConstants.TITLE, activityTitle);
                            }
                        }
                        if (view instanceof CompoundButton) {//ReactSwitch
                            return;
                        }
                        if (view instanceof TextView) {
                            TextView textView = (TextView) view;
                            if (!TextUtils.isEmpty(textView.getText())) {
                                properties.put(AspectjConstants.ELEMENT_CONTENT, textView.getText().toString());
                            }
                        } else if (view instanceof ViewGroup) {
                            StringBuilder stringBuilder = new StringBuilder();
                            String viewText = ViewUtils.traverseView(stringBuilder, (ViewGroup) view);
                            if (!TextUtils.isEmpty(viewText)) {
                                viewText = viewText.substring(0, viewText.length() - 1);
                            }
                            properties.put(AspectjConstants.ELEMENT_CONTENT, viewText);
                        }
                    }
                }
            }
            Cuckoo.with().track(AspectjConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, "onNativeViewHierarchyManagerSetJSResponderAOP error: " + e.getMessage());
        }
    }
}
