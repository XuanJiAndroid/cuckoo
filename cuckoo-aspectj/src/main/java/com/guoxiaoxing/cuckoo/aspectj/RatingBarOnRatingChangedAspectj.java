package com.guoxiaoxing.cuckoo.aspectj;

import com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjClient;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class RatingBarOnRatingChangedAspectj {
    private final static String TAG = RatingBarOnRatingChangedAspectj.class.getCanonicalName();

    @After("execution(* android.widget.RatingBar.OnRatingBarChangeListener.onRatingChanged(android.widget.RatingBar,float,boolean))")
    public void onRatingChangedAOP(final JoinPoint joinPoint) throws Throwable {
        AspectjClient.invokeMethodFromServer(joinPoint, "onRatingBarChanged");
    }
}
