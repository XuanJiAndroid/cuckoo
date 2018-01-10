package com.guoxiaoxing.cuckoo.aspectj;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.R;
import com.guoxiaoxing.cuckoo.util.LogUtils;
import com.guoxiaoxing.cuckoo.util.ViewUtils;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class RatingBarOnRatingChangedAppClick {
    private final static String TAG = "RatingBarOnRatingChangedAppClick";

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
            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 3) {
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
                return;
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

            //rating
            float rating = (float) joinPoint.getArgs()[1];

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

            properties.put(AspectjConstants.ELEMENT_TYPE, "RatingBar");

            //Content
            properties.put(AspectjConstants.ELEMENT_CONTENT, String.valueOf(rating));

            //fragmentName
            ViewUtils.getFragmentNameFromView(view, properties);

            //获取View自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                ViewUtils.mergeJSONObject(p, properties);
            }

            Cuckoo.with().track(AspectjConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, "RatingBar.OnRatingBarChangeListener.onRatingChanged AOP ERROR: " + e.getMessage());
        }
    }
}
