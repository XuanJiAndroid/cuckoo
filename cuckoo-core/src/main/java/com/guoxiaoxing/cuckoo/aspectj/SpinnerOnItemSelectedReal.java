package com.guoxiaoxing.cuckoo.aspectj;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Spinner;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.R;
import com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjConstants;
import com.guoxiaoxing.cuckoo.util.LogUtils;
import com.guoxiaoxing.cuckoo.util.ViewUtils;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class SpinnerOnItemSelectedReal {
    private final static String TAG = "SpinnerOnItemSelectedReal";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            //关闭 AutoTrack
            if (!Cuckoo.with().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (Cuckoo.with().isAutoTrackEventTypeIgnored(Cuckoo.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //基本校验
            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 4) {
                return;
            }

            //获取被点击的 View
            android.widget.AdapterView adapterView = (android.widget.AdapterView) joinPoint.getArgs()[0];
            if (adapterView == null) {
                return;
            }

            //获取所在的 Context
            Context context = adapterView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = ViewUtils.getActivityFromContext(context, adapterView);

            //Activity 被忽略
            if (activity != null) {
                if (Cuckoo.with().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (ViewUtils.isViewIgnored(adapterView)) {
                return;
            }

            //position
            int position = (int) joinPoint.getArgs()[2];

            JSONObject properties = new JSONObject();

            //ViewId
            String idString = ViewUtils.getViewId(adapterView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AspectjConstants.ELEMENT_ID, idString);
            }

            //$screen_name & $title
            if (activity != null) {
                properties.put(AspectjConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = ViewUtils.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AspectjConstants.TITLE, activityTitle);
                }
            }

            if (adapterView instanceof Spinner) { // Spinner
                properties.put(AspectjConstants.ELEMENT_TYPE, "Spinner");
                Object item = adapterView.getItemAtPosition(position);
                properties.put(AspectjConstants.ELEMENT_POSITION, String.valueOf(position));
                if (item != null) {
                    if (item instanceof String) {
                        properties.put(AspectjConstants.ELEMENT_CONTENT, item);
                    }
                }
            } else {
                properties.put(AspectjConstants.ELEMENT_TYPE, adapterView.getClass().getCanonicalName());
            }

            //fragmentName
            ViewUtils.getFragmentNameFromView(adapterView, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) adapterView.getTag(R.id.cuckoo_tag_view_properties);
            if (p != null) {
                ViewUtils.mergeJSONObject(p, properties);
            }

            Cuckoo.with().track(AspectjConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, " AdapterView.OnItemSelectedListener.onItemSelected AOP ERROR: " + e.getMessage());
        }
    }
}
