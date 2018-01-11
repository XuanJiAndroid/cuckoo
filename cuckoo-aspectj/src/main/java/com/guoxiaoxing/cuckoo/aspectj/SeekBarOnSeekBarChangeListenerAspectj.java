package com.guoxiaoxing.cuckoo.aspectj;

import com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjClient;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class SeekBarOnSeekBarChangeListenerAspectj {
    private final static String TAG = SeekBarOnSeekBarChangeListenerAspectj.class.getCanonicalName();

    @After("execution(* android.widget.SeekBar.OnSeekBarChangeListener.onStartTrackingTouch(android.widget.SeekBar))")
    public void onStartTrackingTouchMethod(JoinPoint joinPoint) throws Throwable {
        actionAOP(joinPoint, "onStartTrackingTouch");
    }

    @After("execution(* android.widget.SeekBar.OnSeekBarChangeListener.onStopTrackingTouch(android.widget.SeekBar))")
    public void onStopTrackingTouchMethod(JoinPoint joinPoint) throws Throwable {
        actionAOP(joinPoint, "onStopTrackingTouch");
    }

    private void actionAOP(final JoinPoint joinPoint, final String action) {
        AspectjClient.invokeMethodFromServer(joinPoint, "onSeekBarChange");
    }
}
