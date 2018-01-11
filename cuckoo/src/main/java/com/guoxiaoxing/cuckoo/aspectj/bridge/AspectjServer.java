package com.guoxiaoxing.cuckoo.aspectj.bridge;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.R;
import com.guoxiaoxing.cuckoo.aspectj.AdapterViewOnItemClickListenerReal;
import com.guoxiaoxing.cuckoo.aspectj.CheckBoxOnCheckedChangedReal;
import com.guoxiaoxing.cuckoo.aspectj.DialogOnClickReal;
import com.guoxiaoxing.cuckoo.aspectj.ExpandableListViewItemChildReal;
import com.guoxiaoxing.cuckoo.aspectj.MenuItemReal;
import com.guoxiaoxing.cuckoo.aspectj.RadioGroupOnCheckedReal;
import com.guoxiaoxing.cuckoo.aspectj.RatingBarOnRatingChangedReal;
import com.guoxiaoxing.cuckoo.aspectj.ReactNativeViewReal;
import com.guoxiaoxing.cuckoo.aspectj.SeekBarOnSeekBarChangeReal;
import com.guoxiaoxing.cuckoo.aspectj.SpinnerOnItemSelectedReal;
import com.guoxiaoxing.cuckoo.aspectj.TabHostOnTabChangedReal;
import com.guoxiaoxing.cuckoo.aspectj.TrackViewReal;
import com.guoxiaoxing.cuckoo.aspectj.ViewOnClickReal;
import com.guoxiaoxing.cuckoo.model.ScreenAutoTracker;
import com.guoxiaoxing.cuckoo.annotation.AutoTrackAppViewScreenUrl;
import com.guoxiaoxing.cuckoo.annotation.TrackEvent;
import com.guoxiaoxing.cuckoo.annotation.TrackFragmentAppViewScreen;
import com.guoxiaoxing.cuckoo.annotation.ignore.IgnoreTrackAppViewScreen;
import com.guoxiaoxing.cuckoo.util.ViewUtils;
import com.guoxiaoxing.cuckoo.util.LogUtils;
import com.guoxiaoxing.cuckoo.util.DataUtils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Locale;

public class AspectjServer {

    private final static String TAG = "Cuckoo.AspectjServer";

    //FragmentAspectj
    public static void onFragmentOnResumeMethod(JoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            IgnoreTrackAppViewScreen trackEvent = method.getAnnotation(IgnoreTrackAppViewScreen.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(IgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            Activity activity = targetFragment.getActivity();

            String methodDeclaringClass = targetMethod.getDeclaringClass().getName();

            if (targetMethod.getDeclaringClass().getAnnotation(TrackFragmentAppViewScreen.class) == null) {
                return;
            }

            if (!"android.support.v4.app.Fragment".equals(methodDeclaringClass)) {
                if (!targetFragment.isHidden() && targetFragment.getUserVisibleHint()) {
                    trackFragmentViewScreen(targetFragment, fragmentName, activity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //FragmentAspectj
    public static void onFragmentSetUserVisibleHintMethod(JoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            //Fragment名称
            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            IgnoreTrackAppViewScreen trackEvent = method.getAnnotation(IgnoreTrackAppViewScreen.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(IgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            if (targetMethod.getDeclaringClass().getAnnotation(TrackFragmentAppViewScreen.class) == null) {
                return;
            }

            Activity activity = targetFragment.getActivity();

            //获取所在的Context
            boolean isVisibleHint = (boolean) joinPoint.getArgs()[0];

            if (isVisibleHint) {
                if (targetFragment.isResumed()) {
                    if (!targetFragment.isHidden()) {
                        trackFragmentViewScreen(targetFragment, fragmentName, activity);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //FragmentAspectj
    public static void onFragmentHiddenChangedMethod(JoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            //Fragment名称
            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            IgnoreTrackAppViewScreen trackEvent = method.getAnnotation(IgnoreTrackAppViewScreen.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(IgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            if (targetMethod.getDeclaringClass().getAnnotation(TrackFragmentAppViewScreen.class) == null) {
                return;
            }

            Activity activity = targetFragment.getActivity();

            //获取所在的Context
            boolean hidden = (boolean) joinPoint.getArgs()[0];

            if (!hidden) {
                if (targetFragment.isResumed()) {
                    if (targetFragment.getUserVisibleHint()) {
                        trackFragmentViewScreen(targetFragment, fragmentName, activity);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //FragmentAspectj
    public static void trackFragmentView(JoinPoint joinPoint, Object result) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            if (targetMethod == null) {
                return;
            }

            //Fragment名称
            String fragmentName = joinPoint.getTarget().getClass().getName();

            if (result instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) result);
            } else if (result instanceof View) {
                View view = (View) result;
                view.setTag(R.id.cuckoo_tag_view_fragment_name, fragmentName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void trackFragmentViewScreen(android.support.v4.app.Fragment targetFragment, String fragmentName, Activity activity) {
        try {
            if (targetFragment == null) {
                return;
            }

            if (!Cuckoo.with().isTrackFragmentAppViewScreenEnabled()) {
                return;
            }

            if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragmentName)) {
                return;
            }

            JSONObject properties = new JSONObject();
            if (activity != null) {
                String activityTitle = ViewUtils.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AspectjConstants.TITLE, activityTitle);
                }
                properties.put(AspectjConstants.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName));
            } else {
                properties.put(AspectjConstants.SCREEN_NAME, fragmentName);
            }

            if (targetFragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) targetFragment;

                String screenUrl = screenAutoTracker.getScreenUrl();
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    DataUtils.mergeJSONObject(otherProperties, properties);
                }

                Cuckoo.with().trackViewScreen(screenUrl, properties);
            } else {
                AutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = targetFragment.getClass().getAnnotation(AutoTrackAppViewScreenUrl.class);
                if (autoTrackAppViewScreenUrl != null) {
                    String screenUrl = autoTrackAppViewScreenUrl.url();
                    if (TextUtils.isEmpty(screenUrl)) {
                        screenUrl = fragmentName;
                    }
                    Cuckoo.with().trackViewScreen(screenUrl, properties);
                } else {
                    Cuckoo.with().track("$AppViewScreen", properties);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void traverseView(String fragmentName, ViewGroup root) {
        try {
            if (TextUtils.isEmpty(fragmentName)) {
                return;
            }

            if (root == null) {
                return;
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                if (child instanceof ListView ||
                        child instanceof GridView ||
                        child instanceof Spinner ||
                        child instanceof RadioGroup) {
                    child.setTag(R.id.cuckoo_tag_view_fragment_name, fragmentName);
                } else if (child instanceof ViewGroup) {
                    traverseView(fragmentName, (ViewGroup) child);
                } else {
                    child.setTag(R.id.cuckoo_tag_view_fragment_name, fragmentName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //AdapterViewOnItemClickListenerAspectj
    public static void onAdapterViewItemClick(JoinPoint joinPoint) {
        AdapterViewOnItemClickListenerReal.onAppClick(joinPoint);
    }

    //CheckBoxOnCheckedChangedAspectj
    public static void onCheckBoxCheckedChanged(JoinPoint joinPoint) {
        CheckBoxOnCheckedChangedReal.onAppClick(joinPoint);
    }

    //DialogOnClickAspectj
    public static void onMultiChoiceClick(JoinPoint joinPoint) {
        DialogOnClickReal.onMultiChoiceAppClick(joinPoint);
    }

    //DialogOnClickAspectj
    public static void onDialogClick(JoinPoint joinPoint) {
        DialogOnClickReal.onAppClick(joinPoint);
    }

    //ExpandableListViewItemOnClickAspectj
    public static void onExpandableListViewItemGroupClick(JoinPoint joinPoint) {
        ExpandableListViewItemChildReal.onItemGroupClick(joinPoint);
    }

    //ExpandableListViewItemOnClickAspectj
    public static void onExpandableListViewItemChildClick(JoinPoint joinPoint) {
        ExpandableListViewItemChildReal.onItemChildClick(joinPoint);
    }

    //MenuItemSelectedAspectj
    public static void onMenuClick(JoinPoint joinPoint, int menuItemIndex) {
        MenuItemReal.onAppClick(joinPoint, menuItemIndex);
    }

    //RadioGroupOnCheckedChangeAspectj
    public static void onRadioGroupCheckedChanged(JoinPoint joinPoint) {
        RadioGroupOnCheckedReal.onAppClick(joinPoint);
    }

    //RatingBarOnRatingChangedAspectj
    public static void onRatingBarChanged(JoinPoint joinPoint) {
        RatingBarOnRatingChangedReal.onAppClick(joinPoint);
    }

    //ReactNativeAspectj
    public static void onReactNativeViewAppClick(JoinPoint joinPoint) {
        ReactNativeViewReal.onAppClick(joinPoint);
    }

    //SeekBarOnSeekBarChangeListenerAspectj
    public static void onSeekBarChange(JoinPoint joinPoint) {
        SeekBarOnSeekBarChangeReal.onAppClick(joinPoint);
    }

    //SpinnerOnItemSelectedAspectj
    public static void onSpinnerItemSelected(JoinPoint joinPoint) {
        SpinnerOnItemSelectedReal.onAppClick(joinPoint);
    }

    //TabHostOnTabChangedAspectj
    public static void onTabHostChanged(JoinPoint joinPoint) {
        TabHostOnTabChangedReal.onAppClick(joinPoint);
    }

    //TrackEventAspectj
    public static void trackEventAOP(JoinPoint joinPoint) {
        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

            Method method = methodSignature.getMethod();
            TrackEvent trackEvent = method.getAnnotation(TrackEvent.class);
            String eventName = trackEvent.eventName();
            if (TextUtils.isEmpty(eventName)) {
                return;
            }

            String pString = trackEvent.properties();
            JSONObject properties = new JSONObject();
            if (!TextUtils.isEmpty(pString)) {
                properties = new JSONObject(pString);
            }

            Cuckoo.with().track(eventName, properties);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, "trackEventAOP error: " + e.getMessage());
        }
    }

    //TrackViewOnClickAspectj
    public static void trackViewOnClick(JoinPoint joinPoint) {
        TrackViewReal.onAppClick(joinPoint);
    }

    //ViewOnClickListenerAspectj
    public static void onButterknifeClick(JoinPoint joinPoint) {
        try {
            if (Cuckoo.with().isButterknifeOnClickEnabled()) {
                onViewOnClick(joinPoint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //ViewOnClickListenerAspectj
    public static void onViewOnClick(JoinPoint joinPoint) {
        ViewOnClickReal.onAppClick(joinPoint);
    }
}
