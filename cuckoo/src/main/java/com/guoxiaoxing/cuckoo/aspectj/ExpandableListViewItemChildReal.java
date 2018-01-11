package com.guoxiaoxing.cuckoo.aspectj;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.R;
import com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjConstants;
import com.guoxiaoxing.cuckoo.util.LogUtils;
import com.guoxiaoxing.cuckoo.model.ExpandableListViewItemTrackProperties;
import com.guoxiaoxing.cuckoo.util.ViewUtils;

import org.aspectj.lang.JoinPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class ExpandableListViewItemChildReal {
    private final static String TAG = "ExpandableListViewItemChildReal";

    public static void onItemChildClick(JoinPoint joinPoint) {
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
            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 5) {
                return;
            }

            //获取 ExpandableListView
            ExpandableListView expandableListView = (ExpandableListView) joinPoint.getArgs()[0];
            if (expandableListView == null) {
                return;
            }

            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = ViewUtils.getActivityFromContext(context, expandableListView);

            //Activity 被忽略
            if (activity != null) {
                if (Cuckoo.with().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //ExpandableListView 被忽略
            if (ViewUtils.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            //View 被忽略
            if (ViewUtils.isViewIgnored(expandableListView)) {
                return;
            }

            //获取 View
            View view = (View) joinPoint.getArgs()[1];

            //View 被忽略
            if (ViewUtils.isViewIgnored(view)) {
                return;
            }

            //获取 groupPosition 位置
            int groupPosition = (int) joinPoint.getArgs()[2];

            //获取 childPosition 位置
            int childPosition = (int) joinPoint.getArgs()[3];

            //获取 View 自定义属性
            JSONObject properties = (JSONObject) view.getTag(R.id.cuckoo_tag_view_properties);

            if (properties == null) {
                properties = new JSONObject();
            }

            properties.put(AspectjConstants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d:%d", groupPosition, childPosition));

            //扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof ExpandableListViewItemTrackProperties) {
                    ExpandableListViewItemTrackProperties trackProperties = (ExpandableListViewItemTrackProperties) listAdapter;
                    JSONObject jsonObject = trackProperties.getSensorsChildItemTrackProperties(groupPosition, childPosition);
                    if (jsonObject != null) {
                        ViewUtils.mergeJSONObject(jsonObject, properties);
                    }
                }
            }

            //$screen_name & $title
            if (activity != null) {
                properties.put(AspectjConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = ViewUtils.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AspectjConstants.TITLE, activityTitle);
                }
            }

            //ViewId
            String idString = ViewUtils.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AspectjConstants.ELEMENT_ID, idString);
            }

            properties.put(AspectjConstants.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = ViewUtils.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AspectjConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            ViewUtils.getFragmentNameFromView(expandableListView, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.cuckoo_tag_view_properties);
            if (p != null) {
                ViewUtils.mergeJSONObject(p, properties);
            }

            Cuckoo.with().track(AspectjConstants.APP_CLICK_EVENT_NAME, properties);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, " ExpandableListView.OnChildClickListener.onChildClick AOP ERROR: " + e.getMessage());
        }
    }

    public static void onItemGroupClick(JoinPoint joinPoint) {
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

            //获取 ExpandableListView
            ExpandableListView expandableListView = (ExpandableListView) joinPoint.getArgs()[0];
            if (expandableListView == null) {
                return;
            }

            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = null;
            if (context instanceof Activity) {
                activity = (Activity) context;
            }

            //Activity 被忽略
            if (activity != null) {
                if (Cuckoo.with().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // ExpandableListView Type 被忽略
            if (ViewUtils.isViewIgnored(joinPoint.getArgs()[0].getClass())) {
                return;
            }

            // View 被忽略
            if (ViewUtils.isViewIgnored(expandableListView)) {
                return;
            }

            // 获取 View
            View view = (View) joinPoint.getArgs()[1];

            // 获取 groupPosition 位置
            int groupPosition = (int) joinPoint.getArgs()[2];

            JSONObject properties = new JSONObject();

            // $screen_name & $title
            if (activity != null) {
                properties.put(AspectjConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = ViewUtils.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AspectjConstants.TITLE, activityTitle);
                }
            }

            // ViewId
            String idString = ViewUtils.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AspectjConstants.ELEMENT_ID, idString);
            }

//                    properties.put(AspectjConstants.ELEMENT_ACTION, "onGroupClick");
            properties.put(AspectjConstants.ELEMENT_TYPE, "ExpandableListView");

            //fragmentName
            ViewUtils.getFragmentNameFromView(expandableListView, properties);

            // 获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.cuckoo_tag_view_properties);
            if (p != null) {
                ViewUtils.mergeJSONObject(p, properties);
            }

            // 扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof ExpandableListViewItemTrackProperties) {
                    try {
                        ExpandableListViewItemTrackProperties trackProperties = (ExpandableListViewItemTrackProperties) listAdapter;
                        JSONObject jsonObject = trackProperties.getSensorsGroupItemTrackProperties(groupPosition);
                        if (jsonObject != null) {
                            ViewUtils.mergeJSONObject(jsonObject, properties);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            Cuckoo.with().track(AspectjConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, " ExpandableListView.OnChildClickListener.onGroupClick AOP ERROR: " + e.getMessage());
        }
    }
}
