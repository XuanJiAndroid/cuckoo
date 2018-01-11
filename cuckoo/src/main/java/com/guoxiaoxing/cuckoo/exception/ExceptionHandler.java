package com.guoxiaoxing.cuckoo.exception;

import com.guoxiaoxing.cuckoo.Cuckoo;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "Cuckoo.Exception";

    private static final int SLEEP_TIMEOUT_MS = 3000;

    private static ExceptionHandler sInstance;
    private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    public ExceptionHandler() {
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static void setup() {
        if (sInstance == null) {
            synchronized (ExceptionHandler.class) {
                if (sInstance == null) {
                    sInstance = new ExceptionHandler();
                }
            }
        }
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        // Only one worker thread - giving priority to storing the event first and then flush
        Cuckoo.allInstances(new Cuckoo.InstanceProcessor() {
            @Override
            public void process(Cuckoo sensorsData) {
                try {
                    final JSONObject messageProp = new JSONObject();

                    try {
                        Writer writer = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(writer);
                        e.printStackTrace(printWriter);
                        Throwable cause = e.getCause();
                        while (cause != null) {
                            cause.printStackTrace(printWriter);
                            cause = cause.getCause();
                        }
                        printWriter.close();
                        String result = writer.toString();

                        messageProp.put("app_crashed_reason", result);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    sensorsData.track("AppCrashed", messageProp);
                    sensorsData.clearLastScreenUrl();
                    sensorsData.track("$AppEnd");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Cuckoo.allInstances(new Cuckoo.InstanceProcessor() {
            @Override
            public void process(Cuckoo sensorsData) {
                sensorsData.flush();
            }
        });

        if (mDefaultExceptionHandler != null) {
            mDefaultExceptionHandler.uncaughtException(t, e);
        } else {
            killProcessAndExit();
        }
    }

    private void killProcessAndExit() {
        try {
            Thread.sleep(SLEEP_TIMEOUT_MS);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
