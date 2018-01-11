package com.guoxiaoxing.cuckoo.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ThreadPoolUtils {
    private static ThreadPoolUtils singleton;
    private static Executor mExecutor;

    public static ThreadPoolUtils getInstance() {
        if (singleton == null) {
            synchronized (ThreadPoolUtils.class) {
                if (singleton == null) {
                    singleton = new ThreadPoolUtils();
                    mExecutor = Executors.newFixedThreadPool(5);
                }
            }
        }
        return singleton;
    }

    public void execute(Runnable runnable) {
        try {
            if (runnable != null) {
                mExecutor.execute(runnable);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
