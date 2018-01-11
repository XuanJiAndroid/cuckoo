package com.guoxiaoxing.cuckoo.aspectj;

import com.guoxiaoxing.cuckoo.aspectj.bridge.AspectjClient;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class DialogOnClickAspectj {
    private final static String TAG = DialogOnClickAspectj.class.getCanonicalName();

    @After("execution(* android.content.DialogInterface.OnClickListener.onClick(android.content.DialogInterface, int))")
    public void onClickAOP(final JoinPoint joinPoint) throws Throwable {
        AspectjClient.invokeMethodFromServer(joinPoint, "onDialogClick");
    }

    @After("execution(* android.content.DialogInterface.OnMultiChoiceClickListener.onClick(android.content.DialogInterface, int, boolean))")
    public void onMultiChoiceClickAOP(final JoinPoint joinPoint) throws Throwable {
        AspectjClient.invokeMethodFromServer(joinPoint, "onMultiChoiceClick");
    }
}
