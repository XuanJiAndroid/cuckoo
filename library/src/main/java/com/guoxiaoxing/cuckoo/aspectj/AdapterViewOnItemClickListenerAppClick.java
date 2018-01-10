package com.guoxiaoxing.cuckoo.aspectj;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.R;
import com.guoxiaoxing.cuckoo.util.LogUtils;
import com.guoxiaoxing.cuckoo.model.AdapterViewItemTrackProperties;
import com.guoxiaoxing.cuckoo.util.ViewUtils;

import org.aspectj.lang.JoinPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AdapterViewOnItemClickListenerAppClick {
    private final static String TAG = "AdapterViewOnItemClickListenerAppClick";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            //闭 AutoTrack
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

            //AdapterView
            Object object = joinPoint.getArgs()[0];
            if (object == null) {
                return;
            }

            //View
            View view = (View) joinPoint.getArgs()[1];
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

            //View Type 被忽略
            if (ViewUtils.isViewIgnored(object.getClass())) {
                return;
            }

            //position
            int position = (int) joinPoint.getArgs()[2];

            JSONObject properties = new JSONObject();

            //View 被忽略
            AdapterView adapterView = (AdapterView) object;

            List<Class> mIgnoredViewTypeList = Cuckoo.with().getIgnoredViewTypeList();
            if (mIgnoredViewTypeList != null) {
                if (adapterView instanceof ListView) {
                    properties.put(AspectjConstants.ELEMENT_TYPE, "ListView");
                    if (ViewUtils.isViewIgnored(ListView.class)) {
                        return;
                    }
                } else if (adapterView instanceof GridView) {
                    properties.put(AspectjConstants.ELEMENT_TYPE, "GridView");
                    if (ViewUtils.isViewIgnored(GridView.class)) {
                        return;
                    }
                }
            }

            //扩展属性
            Adapter adapter = adapterView.getAdapter();
            if (adapter != null && adapter instanceof AdapterViewItemTrackProperties) {
                try {
                    AdapterViewItemTrackProperties objectProperties = (AdapterViewItemTrackProperties) adapter;
                    JSONObject jsonObject = objectProperties.getSensorsItemTrackProperties(position);
                    if (jsonObject != null) {
                        ViewUtils.mergeJSONObject(jsonObject, properties);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            //Activity 名称和页面标题
            if (activity != null) {
                properties.put(AspectjConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = ViewUtils.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AspectjConstants.TITLE, activityTitle);
                }
            }

            //点击的 position
            properties.put(AspectjConstants.ELEMENT_POSITION, String.valueOf(position));

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
            ViewUtils.getFragmentNameFromView(adapterView, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                ViewUtils.mergeJSONObject(p, properties);
            }

            Cuckoo.with().track(AspectjConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, " AdapterView.OnItemClickListener.onItemClick AOP ERROR: " + e.getMessage());
        }
    }
}
