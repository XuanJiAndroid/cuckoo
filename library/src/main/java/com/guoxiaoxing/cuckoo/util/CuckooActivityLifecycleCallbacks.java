package com.guoxiaoxing.cuckoo.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.annotation.AutoTrackAppViewScreenUrl;
import com.guoxiaoxing.cuckoo.model.PersistentFirstStart;
import com.guoxiaoxing.cuckoo.model.ScreenAutoTracker;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class CuckooActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "SA.LifecycleCallbacks";
    private boolean resumeFromBackground = false;
    private Integer startedActivityCount = 0;
    private final Object mActivityLifecycleCallbacksLock = new Object();
    private final Cuckoo mSensorsDataInstance;
    private final PersistentFirstStart mFirstStart;
    private final String mMainProcessName;

    public CuckooActivityLifecycleCallbacks(Cuckoo instance, PersistentFirstStart firstStart, String mainProcessName) {
        this.mSensorsDataInstance = instance;
        this.mFirstStart = firstStart;
        this.mMainProcessName = mainProcessName;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                if (startedActivityCount == 0) {
                    // XXX: 注意内部执行顺序
                    boolean firstStart = mFirstStart.get();

                    try {
                        mSensorsDataInstance.appBecomeActive();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (DataUtils.isMainProcess(activity, mMainProcessName)) {
                        if (mSensorsDataInstance.isAutoTrackEnabled()) {
                            try {
                                if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(Cuckoo.AutoTrackEventType.APP_START)) {
                                    if (firstStart) {
                                        mFirstStart.commit(false);
                                    }
                                    JSONObject properties = new JSONObject();
                                    properties.put("$resume_from_background", resumeFromBackground);
                                    properties.put("$is_first_time", firstStart);
                                    DataUtils.getScreenNameAndTitleFromActivity(properties, activity);

                                    mSensorsDataInstance.track("$AppStart", properties);
                                }

                                if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(Cuckoo.AutoTrackEventType.APP_END)) {
                                    mSensorsDataInstance.trackTimer("$AppEnd", TimeUnit.SECONDS);
                                }
                            } catch (Exception e) {
                                LogUtils.i(TAG, e);
                            }
                        }

                        // 下次启动时，从后台恢复
                        resumeFromBackground = true;
                    }
                }

                startedActivityCount = startedActivityCount + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        try {
            boolean mShowAutoTrack = true;
            if (mSensorsDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())) {
                mShowAutoTrack = false;
            }

            if (mSensorsDataInstance.isAutoTrackEnabled() && mShowAutoTrack && !mSensorsDataInstance.isAutoTrackEventTypeIgnored(Cuckoo.AutoTrackEventType.APP_VIEW_SCREEN)) {
                try {
                    JSONObject properties = new JSONObject();
                    properties.put("$screen_name", activity.getClass().getCanonicalName());
                    DataUtils.getScreenNameAndTitleFromActivity(properties, activity);

                    if (activity instanceof ScreenAutoTracker) {
                        ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;

                        String screenUrl = screenAutoTracker.getScreenUrl();
                        JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                        if (otherProperties != null) {
                            DataUtils.mergeJSONObject(otherProperties, properties);
                        }

                        mSensorsDataInstance.trackViewScreen(screenUrl, properties);
                    } else {
                        AutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = activity.getClass().getAnnotation(AutoTrackAppViewScreenUrl.class);
                        if (autoTrackAppViewScreenUrl != null) {
                            String screenUrl = autoTrackAppViewScreenUrl.url();
                            if (TextUtils.isEmpty(screenUrl)) {
                                screenUrl = activity.getClass().getCanonicalName();
                            }
                            mSensorsDataInstance.trackViewScreen(screenUrl, properties);
                        } else {
                            mSensorsDataInstance.track("$AppViewScreen", properties);
                        }
                    }
                } catch (Exception e) {
                    LogUtils.i(TAG, e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                startedActivityCount = startedActivityCount - 1;

                if (startedActivityCount == 0) {
                    try {
                        mSensorsDataInstance.appEnterBackground();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (DataUtils.isMainProcess(activity, mMainProcessName)) {
                        if (mSensorsDataInstance.isAutoTrackEnabled()) {
                            try {
                                if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(Cuckoo.AutoTrackEventType.APP_END)) {
                                    JSONObject properties = new JSONObject();
                                    DataUtils.getScreenNameAndTitleFromActivity(properties, activity);
                                    mSensorsDataInstance.clearLastScreenUrl();
                                    mSensorsDataInstance.track("$AppEnd", properties);
                                }
                            } catch (Exception e) {
                                LogUtils.i(TAG, e);
                            }
                        }
                    }
                    try {
                        mSensorsDataInstance.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
