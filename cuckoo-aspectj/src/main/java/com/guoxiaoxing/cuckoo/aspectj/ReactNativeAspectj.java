package com.guoxiaoxing.cuckoo.aspectj;

import com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjClient;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class ReactNativeAspectj {
    private final static String TAG = ReactNativeAspectj.class.getCanonicalName();

    @After("execution(* com.facebook.react.uimanager.NativeViewHierarchyManager.setJSResponder(int,int,boolean))")
    public void onNativeViewHierarchyManagerSetJSResponderAOP(final JoinPoint joinPoint) throws Throwable {
        AspectjClient.invokeMethodFromServer(joinPoint, "onReactNativeViewAppClick");
    }
}
