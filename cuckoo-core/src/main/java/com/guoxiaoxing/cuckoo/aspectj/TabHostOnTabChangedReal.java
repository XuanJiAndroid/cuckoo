package com.guoxiaoxing.cuckoo.aspectj;

import android.text.TextUtils;
import android.widget.TabHost;

import com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjConstants;
import com.guoxiaoxing.cuckoo.util.LogUtils;
import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.util.ViewUtils;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class TabHostOnTabChangedReal {
    private final static String TAG = "TabHostOnTabChangedReal";

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
            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 1) {
                return;
            }

            //TabHost 被忽略
            if (ViewUtils.isViewIgnored(TabHost.class)) {
                return;
            }

            //获取被点击的 tabName
            String tabName = (String) joinPoint.getArgs()[0];

            JSONObject properties = new JSONObject();

            //$title、$screen_name、$element_content
            try {
                if (!TextUtils.isEmpty(tabName)) {
                    String[] temp = tabName.split("##");

                    switch (temp.length) {
                        case 3:
                            properties.put(AspectjConstants.TITLE, temp[2]);
                        case 2:
                            properties.put(AspectjConstants.SCREEN_NAME, temp[1]);
                        case 1:
                            properties.put(AspectjConstants.ELEMENT_CONTENT, temp[0]);
                            break;
                    }
                }
            } catch (Exception e) {
                properties.put(AspectjConstants.ELEMENT_CONTENT, tabName);
                e.printStackTrace();
            }

            properties.put(AspectjConstants.ELEMENT_TYPE, "TabHost");

            Cuckoo.with().track(AspectjConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, " onTabChanged AOP ERROR: " + e.getMessage());
        }
    }
}
