package com.guoxiaoxing.cuckoo.aspectj;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.R;
import com.guoxiaoxing.cuckoo.annotation.ignore.IgnoreTrackOnClick;
import com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjConstants;
import com.guoxiaoxing.cuckoo.util.LogUtils;
import com.guoxiaoxing.cuckoo.util.ViewUtils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;

import java.lang.reflect.Method;

public class ViewOnClickReal {
    private final static String TAG = "ViewOnClickReal";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            //基本校验
            if (joinPoint.getArgs() == null || joinPoint.getArgs().length != 1) {
                return;
            }

            //关闭 AutoTrack
            if (!Cuckoo.with().isAutoTrackEnabled()) {
                return;
            }

            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            if (methodSignature == null) {
                return;
            }

            //忽略 onClick
            Method method = methodSignature.getMethod();
            if (method == null) {
                return;
            }

            IgnoreTrackOnClick trackEvent = method.getAnnotation(IgnoreTrackOnClick.class);
            if (trackEvent != null) {
                return;
            }

            //$AppClick 被过滤
            if (Cuckoo.with().isAutoTrackEventTypeIgnored(Cuckoo.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取被点击的 View
            View view = (View) joinPoint.getArgs()[0];
            if (view == null) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
//                        return;
            }

            //将 Context 转成 Activity
            Activity activity = ViewUtils.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (Cuckoo.with().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (ViewUtils.isViewIgnored(view)) {
                return;
            }

            long currentOnClickTimestamp = System.currentTimeMillis();
            String tag = (String) view.getTag(R.id.cuckoo_tag_view_onclick_timestamp);
            if (!TextUtils.isEmpty(tag)) {
                try {
                    long lastOnClickTimestamp = Long.parseLong(tag);
                    if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                        LogUtils.i(TAG, "This onClick maybe extends from super, IGNORE");
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            view.setTag(R.id.cuckoo_tag_view_onclick_timestamp, String.valueOf(currentOnClickTimestamp));

            JSONObject properties = new JSONObject();

            //ViewId
            String idString = ViewUtils.getViewId(view);
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

            String viewType = view.getClass().getCanonicalName();
            CharSequence viewText = null;
            if (view instanceof CheckBox) { // CheckBox
                viewType = "CheckBox";
                CheckBox checkBox = (CheckBox) view;
                viewText = checkBox.getText();
            } else if (view instanceof SwitchCompat) {
                viewType = "SwitchCompat";
                SwitchCompat switchCompat = (SwitchCompat) view;
                viewText = switchCompat.getTextOn();
            } else if (view instanceof RadioButton) { // RadioButton
                viewType = "RadioButton";
                RadioButton radioButton = (RadioButton) view;
                viewText = radioButton.getText();
            } else if (view instanceof ToggleButton) { // ToggleButton
                viewType = "ToggleButton";
                ToggleButton toggleButton = (ToggleButton) view;
                boolean isChecked = toggleButton.isChecked();
                if (isChecked) {
                    viewText = toggleButton.getTextOn();
                } else {
                    viewText = toggleButton.getTextOff();
                }
            } else if (view instanceof Button) { // Button
                viewType = "Button";
                Button button = (Button) view;
                viewText = button.getText();
            } else if (view instanceof CheckedTextView) { // CheckedTextView
                viewType = "CheckedTextView";
                CheckedTextView textView = (CheckedTextView) view;
                viewText = textView.getText();
            } else if (view instanceof TextView) { // TextView
                viewType = "TextView";
                TextView textView = (TextView) view;
                viewText = textView.getText();
            } else if (view instanceof ImageButton) { // ImageButton
                viewType = "ImageButton";
            } else if (view instanceof ImageView) { // ImageView
                viewType = "ImageView";
            } else if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = ViewUtils.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AspectjConstants.ELEMENT_CONTENT, viewText.toString());
            }

            //$element_type
            properties.put(AspectjConstants.ELEMENT_TYPE, viewType);

            //fragmentName
            ViewUtils.getFragmentNameFromView(view, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.cuckoo_tag_view_properties);
            if (p != null) {
                ViewUtils.mergeJSONObject(p, properties);
            }
            Cuckoo.with().track(AspectjConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, "onViewClickMethod error: " + e.getMessage());
        }
    }
}
