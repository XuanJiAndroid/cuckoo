package com.guoxiaoxing.cuckoo.aspectj;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.MenuItem;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjConstants;
import com.guoxiaoxing.cuckoo.util.LogUtils;
import com.guoxiaoxing.cuckoo.util.ViewUtils;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class MenuItemReal {
    private final static String TAG = "MenuItemReal";

    public static void onAppClick(JoinPoint joinPoint, int menuItemIndex) {
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
            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length == 0) {
                return;
            }

            //获取被点击的 MenuItem
            MenuItem menuItem = (MenuItem) joinPoint.getArgs()[menuItemIndex];
            if (menuItem == null) {
                return;
            }

            //MenuItem 被忽略
            if (ViewUtils.isViewIgnored(MenuItem.class)) {
                return;
            }

            //获取所在的 Context
            Object object = joinPoint.getTarget();
            if (object == null) {
                return;
            }

            Context context = null;
            if (object instanceof Context) {
                context = (Context) object;
            }
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = ViewUtils.getActivityFromContext(context, null);

            //Activity 被忽略
            if (activity != null) {
                if (Cuckoo.with().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //获取View ID
            String idString = null;
            try {
                idString = context.getResources().getResourceEntryName(menuItem.getItemId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            JSONObject properties = new JSONObject();

            //$screen_name & $title
            if (activity != null) {
                properties.put(AspectjConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = ViewUtils.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AspectjConstants.TITLE, activityTitle);
                }
            }

            //ViewID
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AspectjConstants.ELEMENT_ID, idString);
            }

            //Content
            if (!TextUtils.isEmpty(menuItem.getTitle())) {
                properties.put(AspectjConstants.ELEMENT_CONTENT, menuItem.getTitle());
            }

            //Type
            properties.put(AspectjConstants.ELEMENT_TYPE, "MenuItem");

            Cuckoo.with().track(AspectjConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, " error: " + e.getMessage());
        }
    }
}
