package com.guoxiaoxing.cuckoo.vtrack;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.JsonWriter;
import android.util.Pair;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.debug.DebugTracking;
import com.guoxiaoxing.cuckoo.event.EditState;
import com.guoxiaoxing.cuckoo.model.ResourceIds;
import com.guoxiaoxing.cuckoo.model.ResourceReader;
import com.guoxiaoxing.cuckoo.util.Base64Coder;
import com.guoxiaoxing.cuckoo.util.DataUtils;
import com.guoxiaoxing.cuckoo.util.JSONUtils;
import com.guoxiaoxing.cuckoo.util.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

/**
 * 可视化埋点，调度截屏工具ViewSnapshot
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com.
 *
 * @author guoxiaoxing
 */
@TargetApi(Cuckoo.VTRACK_SUPPORTED_MIN_API)
public class VTrackSupported implements VTrack, DebugTracking {

    private static final String TAG = "SCuckoo.VTrackSupported";

    private static final String SHARED_PREF_EDITS_FILE = "sensorsdata";
    private static final String SHARED_PREF_BINDINGS_KEY = "sensorsdata.viewcrawler.bindings";

    private static final int MESSAGE_INITIALIZE_CHANGES = 0;
    private static final int MESSAGE_CONNECT_TO_EDITOR = 1;
    private static final int MESSAGE_SEND_STATE_FOR_EDITING = 2;
    private static final int MESSAGE_SEND_DEVICE_INFO = 4;
    private static final int MESSAGE_EVENT_BINDINGS_RECEIVED = 5;
    private static final int MESSAGE_HANDLE_EDITOR_BINDINGS_RECEIVED = 6;
    private static final int MESSAGE_SEND_EVENT_TRACKED = 7;
    private static final int MESSAGE_HANDLE_EDITOR_CLOSED = 8;
    private static final int MESSAGE_HANDLE_DISCONNECT = 13;

    private static final int EMULATOR_CONNECT_ATTEMPT_INTERVAL_MILLIS = 1000 * 30;

    private static boolean mIsRetryConnect = true;
    private static int mCurrentRetryTimes = 0;
    private static final int CONNECT_RETRY_TIMES = 40;
    private static final long RETRY_TIME_INTERVAL = 30 * 1000;
    /*
        WebSocket close Code:
        check com.guoxiaoxing.cuckoo.java_websocket.framing.CloseFrame.NORMAL
    */
    private static final int CLOSE_CODE_NORMAL = 1000;
    private static final int CLOSE_CODE_GOING_AWAY = 1001;
    private static final int CLOSE_CODE_NOCODE = 1005;

    private final Context mContext;
    private final LifecycleCallbacks mLifecycleCallbacks;
    private final EventViewTracker mEventViewTracker;
    private final EditState mEditState;
    private final ViewFinderHandler mMessageThreadHandler;

    // VTrack Server 地址
    private String mVTrackServer = null;

    //当前显示的Activity
    private final HashSet<Activity> mStartedActivities = new HashSet<>();
    private final HashSet<String> mDisabledActivity = new HashSet<String>();

    public VTrackSupported(Context context, String resourcePackageName) {
        mContext = context;

        mEditState = new EditState();

        mLifecycleCallbacks = new LifecycleCallbacks();
        final Application app = (Application) context.getApplicationContext();
        app.registerActivityLifecycleCallbacks(mLifecycleCallbacks);

        final HandlerThread thread =
                new HandlerThread(VTrackSupported.class.getCanonicalName(), Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mMessageThreadHandler = new ViewFinderHandler(context, thread.getLooper(), resourcePackageName);

        mEventViewTracker = new EventViewTracker(context, mMessageThreadHandler);
    }

    @Override
    public void startUpdates() {
        mMessageThreadHandler.start();
        mMessageThreadHandler
                .sendMessage(mMessageThreadHandler.obtainMessage(MESSAGE_INITIALIZE_CHANGES));
    }

    @Override
    public void enableEditingVTrack() {
        mLifecycleCallbacks.enableConnector();
    }

    @Override
    public void disableActivity(String canonicalName) {
        mDisabledActivity.add(canonicalName);
    }

    @Override
    public void setEventBindings(JSONArray bindings) {
        final Message msg =
                mMessageThreadHandler.obtainMessage(VTrackSupported.MESSAGE_EVENT_BINDINGS_RECEIVED);
        msg.obj = bindings;
        mMessageThreadHandler.sendMessage(msg);
    }

    @Override
    public void setVTrackServer(String serverUrl) {
        // XXX: 为了兼容历史版本，有三种方式设置可视化埋点管理界面服务地址，优先级从高到低：
        //  1. 从 SDK 构造函数传入
        //  2. 从 SDK 配置分发的结果中获取（1.6+）
        //  3. 从 SDK 配置分发的 Url 中自动生成（兼容旧版本）

        // 对应 2.
        if (mVTrackServer == null && serverUrl != null && serverUrl.length() > 0) {
            mVTrackServer = serverUrl;
            LogUtils.i(TAG, "Gets VTrack server URL '" + mVTrackServer + "' from configure.");
        }

        // 对应 3.
        if (mVTrackServer == null) {
            Uri configureURI = Uri.parse(Cuckoo.with(mContext).getConfigureUrl());
            mVTrackServer = configureURI.buildUpon().path("/api/ws").scheme("ws").build().toString();
            LogUtils.i(TAG, "Generates VTrack server URL '" + mVTrackServer + "' with configure URL.");
        }

        if (mVTrackServer == null) {
            LogUtils.i(TAG, "Unknown VTrack server URL.");
        }
    }

    @Override
    public void reportTrack(JSONObject eventJson) {
        final Message m = mMessageThreadHandler.obtainMessage();
        m.what = MESSAGE_SEND_EVENT_TRACKED;
        m.obj = eventJson;

        mMessageThreadHandler.sendMessage(m);
    }

    private class EmulatorConnector implements Runnable {

        private volatile boolean mStopped;

        public EmulatorConnector() {
            mStopped = true;
        }

        @Override
        public void run() {
            if (mStopped) {
                return;
            }

            if (mVTrackServer == null) {
                // XXX: 若未设置可视化埋点管理界面地址，说明正在等待获取 SDK 配置，提高重试频率
                mMessageThreadHandler.postDelayed(this, EMULATOR_CONNECT_ATTEMPT_INTERVAL_MILLIS / 10);
            } else {
                final Message message = mMessageThreadHandler.obtainMessage(MESSAGE_CONNECT_TO_EDITOR);
                mMessageThreadHandler.sendMessage(message);

                mMessageThreadHandler.postDelayed(this, EMULATOR_CONNECT_ATTEMPT_INTERVAL_MILLIS);
            }
        }

        public void start() {
            if (mStopped) {
                mStopped = false;
                mMessageThreadHandler.post(this);
            }
        }

        public void stop() {
            mStopped = true;
            mMessageThreadHandler.removeCallbacks(this);
        }
    }

    /**
     * Activity生命周期，采集当前显示的Activity。
     */
    private class LifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

        private boolean mEnableConnector = false;
        private final EmulatorConnector mEmulatorConnector = new EmulatorConnector();

        public LifecycleCallbacks() {
        }

        void enableConnector() {
            mEnableConnector = true;
            mEmulatorConnector.start();
        }

        void disableConnector() {
            mEnableConnector = false;
            mEmulatorConnector.stop();
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (mEnableConnector) {
                mEmulatorConnector.start();
            }

            mStartedActivities.add(activity);

            for (String className : mDisabledActivity) {
                if (className.equals(activity.getClass().getCanonicalName())) {
                    return;
                }
            }
            mEditState.add(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            mStartedActivities.remove(activity);

            mEditState.remove(activity);
            if (mEditState.isEmpty()) {
                mEmulatorConnector.stop();
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }

    /**
     * 查找事件对应的View
     */
    private class ViewFinderHandler extends Handler {

        private VTrackClient mVTrackClient;
        private ViewSnapshot mSnapshot;
        private final Context mContext;
        private final Lock mStartLock;
        private final VTrackProtocol mProtocol;

        // 是否启用 GZip 压缩
        private boolean mUseGzip;

        private int mHasRetryCount = 0;

        private final List<Pair<String, JSONObject>> mVTrackEventBindings;
        private final List<Pair<String, JSONObject>> mPersistentEventBindings;

        public ViewFinderHandler(Context context, Looper looper, String resourcePackageName) {
            super(looper);
            mContext = context;
            mSnapshot = null;

            final ResourceIds resourceIds = new ResourceReader.Ids(resourcePackageName, context);

            mProtocol = new VTrackProtocol(resourceIds);
            mVTrackEventBindings = new ArrayList<>();
            mPersistentEventBindings = new ArrayList<>();

            // 默认关闭 GZip 压缩
            mUseGzip = false;

            mStartLock = new ReentrantLock();
            mStartLock.lock();
        }

        public void start() {
            mStartLock.unlock();
        }

        @Override
        public void handleMessage(Message msg) {
            //pay close attention to this
            mStartLock.lock();

            try {
                switch (msg.what) {
                    case MESSAGE_INITIALIZE_CHANGES:
                        setupBindings();
                        break;
                    case MESSAGE_CONNECT_TO_EDITOR:
                        connectToVTrackServer();
                        break;
                    case MESSAGE_SEND_DEVICE_INFO:
                        sendDeviceInfo((JSONObject) msg.obj);
                        break;
                    case MESSAGE_SEND_STATE_FOR_EDITING:
                        sendSnapshot((JSONObject) msg.obj);
                        break;
                    case MESSAGE_SEND_EVENT_TRACKED:
                        sendReportTrackToServer((JSONObject) msg.obj);
                        break;
                    case MESSAGE_EVENT_BINDINGS_RECEIVED:
                        handleEventBindingsReceived((JSONArray) msg.obj);
                        break;
                    case MESSAGE_HANDLE_EDITOR_BINDINGS_RECEIVED:
                        handleEditorBindingsReceived((JSONObject) msg.obj);
                        break;
                    case MESSAGE_HANDLE_EDITOR_CLOSED:
                        handleVTrackServerClosed();
                        break;
                    case MESSAGE_HANDLE_DISCONNECT:
                        handleDisconnect();
                        break;
                }
            } finally {
                mStartLock.unlock();
            }
        }

        /**
         * Load stored changes from persistent storage and apply them to the application.
         */
        private void setupBindings() {
            final SharedPreferences preferences = getSharedPreferences();
            final String storedBindings = preferences.getString(SHARED_PREF_BINDINGS_KEY, null);
            try {
                if (null != storedBindings) {
                    LogUtils.i(TAG, "Initialize event bindings: " + storedBindings);

                    final JSONArray bindings = new JSONArray(storedBindings);

                    mPersistentEventBindings.clear();
                    for (int i = 0; i < bindings.length(); i++) {
                        final JSONObject event = bindings.getJSONObject(i);
                        final String targetActivity = JSONUtils.optionalStringKey(event, "target_activity");
                        mPersistentEventBindings.add(new Pair<String, JSONObject>(targetActivity, event));
                    }
                }
            } catch (final JSONException e) {
                LogUtils.i(TAG, "JSON error when initializing saved changes, clearing persistent memory", e);
                final SharedPreferences.Editor editor = preferences.edit();
                editor.remove(SHARED_PREF_BINDINGS_KEY);
                editor.apply();
            }

            applyVariantsAndEventBindings();
        }

        private void retrySendDeviceInfo(final JSONObject message) {
            if (mHasRetryCount < 3) {
                mHasRetryCount++;
                final Message msg = mMessageThreadHandler.obtainMessage(VTrackSupported.MESSAGE_SEND_DEVICE_INFO);
                msg.obj = message;
                mMessageThreadHandler.sendMessageDelayed(msg, 3000);
            }
        }

        /**
         * Try to connect to the remote interactive editor, if a connection does not already exist.
         */
        private void connectToVTrackServer() {
            if (mVTrackClient != null && mVTrackClient.isValid()) {
                LogUtils.i(TAG, "The VTrack server has been connected.");
                return;
            }

            if (mVTrackServer != null) {
                LogUtils.i(TAG, "Connecting to the VTrack server with " + mVTrackServer);

                try {
                    mVTrackClient = new VTrackClient(new URI(mVTrackServer), new VTrackClientConnection());
                } catch (final URISyntaxException e) {
                    LogUtils.i(TAG, "Error parsing URI " + mVTrackServer + " for VTrack webSocket", e);
                } catch (final VTrackClient.EditorConnectionException e) {
                    LogUtils.i(TAG, "Error connecting to URI " + mVTrackServer, e);
                }
            }
        }

        /**
         * Report on device info to the connected web UI.
         */
        private void sendDeviceInfo(final JSONObject message) {
            if (mVTrackClient == null || !mVTrackClient.isValid()) {
                return;
            }

            Iterator<Activity> activityIt = mStartedActivities.iterator();
            if (!activityIt.hasNext()) {
                retrySendDeviceInfo(message);
                return;
            }
            Activity activity = activityIt.next();

            if (activity == null) {
                if (mVTrackClient != null) {
                    mVTrackClient.close(true);
                }
                return;
            }

            try {
                new AlertDialog.Builder(activity).setMessage("正在连接到 Sensors Analytics 可视化埋点管理界面...")
                        .setTitle("Connecting to VTrack")
                        .setPositiveButton("继续", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();

                                try {
                                    final JSONObject payload = message.getJSONObject("payload");
                                    if (payload.has("support_gzip")) {
                                        mUseGzip = payload.getBoolean("support_gzip");
                                    }
                                } catch (final JSONException e) {
                                    // Do NOTHING
                                    // 旧版的 WebServer，DeviceInfoRequest 不带 "payload" 字段
                                }

                                final PackageManager manager = mContext.getPackageManager();
                                final DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();

                                try {
                                    JSONObject payload = new JSONObject();

                                    payload.put("$lib", "Android");
                                    payload.put("$lib_version", Cuckoo.SDK_VERSION);
                                    payload.put("$os", "Android");
                                    payload
                                            .put("$os_version", Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION
                                                    .RELEASE);
                                    payload.put("$screen_height", String.valueOf(displayMetrics.heightPixels));
                                    payload.put("$screen_width", String.valueOf(displayMetrics.widthPixels));
                                    try {
                                        final PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
                                        payload.put("$main_bundle_identifier", info.packageName);
                                        payload.put("$app_version", info.versionName);
                                    } catch (PackageManager.NameNotFoundException e) {
                                        payload.put("$main_bundle_identifier", "");
                                        payload.put("$app_version", "");
                                    }
                                    payload.put("$device_name", Build.BRAND + "/" + Build.MODEL);
                                    payload.put("$device_model", Build.MODEL == null ? "UNKNOWN" : Build.MODEL);
                                    payload.put("$device_id", DataUtils.getDeviceID(mContext));

                                    if (mVTrackClient != null && mVTrackClient.isValid()) {
                                        mVTrackClient
                                                .sendMessage(setUpPayload("device_info_response", payload).toString());
                                    }
                                } catch (JSONException e) {
                                    LogUtils.i(TAG, "Can't write the response for device information.", e);
                                } catch (IOException e) {
                                    LogUtils.i(TAG, "Can't write the response for device information.", e);
                                }
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();

                                if (mVTrackClient == null) {
                                    return;
                                }

                                mVTrackClient.close(true);
                            }
                        }).show();
            } catch (RuntimeException e) {
                LogUtils.i(TAG, "Failed to show dialog of VTrack connector", e);
            }
        }

        /**
         * Send a snapshot response, with crawled views and screenshot image, to the connected web UI.
         */
        private void sendSnapshot(JSONObject message) {
            if (mVTrackClient == null || !mVTrackClient.isValid()) {
                return;
            }

            final long startSnapshot = System.currentTimeMillis();

            try {
                final JSONObject payload = message.getJSONObject("payload");
                if (payload.has("config")) {
                    mSnapshot = mProtocol.readSnapshotConfig(payload);
                }

                if (null == mSnapshot) {
                    LogUtils.i(TAG, "Snapshot should be setup at first calling.");
                    return;
                }

                if (payload.has("last_image_hash")) {
                    final String lastImageHash = payload.getString("last_image_hash");
                    mSnapshot.updateLastImageHashArray(lastImageHash);
                }
            } catch (final JSONException e) {
                LogUtils.i(TAG, "Payload with snapshot config required with snapshot request", e);
                return;
            } catch (final VTrackProtocol.BadInstructionsException e) {
                LogUtils.i(TAG, "VTrack server sent malformed message with snapshot request", e);
                return;
            }

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final OutputStreamWriter writer = new OutputStreamWriter(out);

            try {
                writer.write("{");
                writer.write("\"type\": \"snapshot_response\",");
                if (mUseGzip) {
                    final ByteArrayOutputStream payload_out = new ByteArrayOutputStream();
                    final OutputStreamWriter payload_writer = new OutputStreamWriter(payload_out);

                    payload_writer.write("{\"activities\":");
                    payload_writer.flush();
                    mSnapshot.snapshots(mEditState, payload_out);
                    final long snapshotTime = System.currentTimeMillis() - startSnapshot;
                    payload_writer.write(",\"snapshot_time_millis\": ");
                    payload_writer.write(Long.toString(snapshotTime));
                    payload_writer.write("}");
                    payload_writer.flush();

                    payload_out.close();
                    byte[] payloadData = payload_out.toString().getBytes();

                    ByteArrayOutputStream os = new ByteArrayOutputStream(payloadData.length);
                    GZIPOutputStream gos = new GZIPOutputStream(os);
                    gos.write(payloadData);
                    gos.close();
                    byte[] compressed = os.toByteArray();
                    os.close();

                    writer.write("\"gzip_payload\": \"" + new String(Base64Coder.encode(compressed)) + "\"");
                } else {
                    writer.write("\"payload\": {");

                    {
                        writer.write("\"activities\":");
                        writer.flush();
                        mSnapshot.snapshots(mEditState, out);
                    }

                    final long snapshotTime = System.currentTimeMillis() - startSnapshot;
                    writer.write(",\"snapshot_time_millis\": ");
                    writer.write(Long.toString(snapshotTime));

                    writer.write("}");
                }

                writer.write("}");
                writer.flush();
            } catch (final IOException e) {
                LogUtils.i(TAG, "Can't write snapshot request to server", e);
            } finally {
                try {
                    writer.close();
                } catch (final IOException e) {
                    LogUtils.i(TAG, "Can't close writer.", e);
                }
            }

            if (mVTrackClient != null && mVTrackClient.isValid()) {
                mVTrackClient.sendMessage(out.toString());
            }
        }

        /**
         * Report the eventBinding response to the connected web UI.
         */
        private void sendEventBindingResponse(boolean result) {
            if (mVTrackClient == null || !mVTrackClient.isValid()) {
                return;
            }

            //final OutputStream out = mVTrackClient.getBufferedOutputStream();
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final JsonWriter j = new JsonWriter(new OutputStreamWriter(out));

            try {
                j.beginObject();
                j.name("type").value("event_binding_response");
                j.name("payload").beginObject();
                j.name("status").value("OK");
                j.endObject();
                j.endObject();
            } catch (final IOException e) {
                LogUtils.i(TAG, "Can't write event_binding_response to server", e);
            } finally {
                try {
                    j.close();
                } catch (final IOException e) {
                    LogUtils.i(TAG, "Can't close webSocket writer", e);
                }
            }

            if (mVTrackClient != null && mVTrackClient.isValid()) {
                mVTrackClient.sendMessage(out.toString());
            }
        }

        /**
         * Report that a track has occurred to the connected web UI.
         */
        private void sendReportTrackToServer(JSONObject eventJson) {
            if (mVTrackClient == null || !mVTrackClient.isValid() || eventJson == null) {
                return;
            }

            final JSONObject sendProperties = eventJson.optJSONObject("properties");
            if (sendProperties == null) {
                return;
            }

            LogUtils.i(TAG, "Sending debug track to vtrack. original event: " + eventJson.toString());

            final String fromVTrack = sendProperties.optString("$from_vtrack", "");
            if (fromVTrack.length() < 1) {
                return;
            }

            final OutputStream out = mVTrackClient.getBufferedOutputStream();
            final OutputStreamWriter writer = new OutputStreamWriter(out);

            try {
                JSONObject payload = new JSONObject();
                payload.put("depolyed", sendProperties.getBoolean("$binding_depolyed"));
                payload.put("trigger_id", sendProperties.getString("$binding_trigger_id"));
                payload.put("path", sendProperties.getString("$binding_path"));

                sendProperties.remove("$binding_path");
                sendProperties.remove("$binding_depolyed");
                sendProperties.remove("$binding_trigger_id");
                eventJson.put("properties", sendProperties);

                payload.put("event", eventJson);

                JSONObject result = new JSONObject();
                result.put("type", "debug_track");
                result.put("payload", payload);

                writer.write(result.toString());
                writer.flush();
            } catch (JSONException e) {
                LogUtils.i(TAG, "Invalid properties", e);
            } catch (final IOException e) {
                LogUtils.i(TAG, "Can't write track_message to server", e);
            } finally {
                try {
                    writer.close();
                } catch (final IOException e) {
                    LogUtils.i(TAG, "Can't close writer.", e);
                }
            }
        }

        /**
         * Accept and apply a persistent event binding from a non-interactive source.
         */
        private void handleEventBindingsReceived(JSONArray eventBindings) {
            final SharedPreferences preferences = getSharedPreferences();
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(SHARED_PREF_BINDINGS_KEY, eventBindings.toString());
            editor.apply();
            setupBindings();
        }

        /**
         * Accept and apply a temporary event binding from the connected UI.
         */
        private void handleEditorBindingsReceived(JSONObject message) {
            LogUtils.i(TAG, String.format("Received event bindings from VTrack editor: %s", message
                    .toString()));

            final JSONArray eventBindings;

            sendEventBindingResponse(true);

            try {
                final JSONObject payload = message.getJSONObject("payload");
                eventBindings = payload.getJSONArray("events");
            } catch (final JSONException e) {
                LogUtils.i(TAG, "Bad event bindings received", e);
                return;
            }

            final int eventCount = eventBindings.length();

            mVTrackEventBindings.clear();
            for (int i = 0; i < eventCount; i++) {
                try {
                    final JSONObject event = eventBindings.getJSONObject(i);
                    final String targetActivity = JSONUtils.optionalStringKey(event, "target_activity");
                    mVTrackEventBindings.add(new Pair<String, JSONObject>(targetActivity, event));
                } catch (final JSONException e) {
                    LogUtils.i(TAG, "Bad event binding received from VTrack server in " + eventBindings
                            .toString(), e);
                }
            }

            applyVariantsAndEventBindings();
        }

        /**
         * Clear state associated with the editor now that the editor is gone.
         */
        private void handleVTrackServerClosed() {
            LogUtils.i(TAG, "VTrack server connection closed.");

            mSnapshot = null;

            mVTrackEventBindings.clear();
            applyVariantsAndEventBindings();
        }

        /**
         * disconnect webSocket server;.
         */
        private void handleDisconnect() {
            if (mVTrackClient == null) {
                return;
            }

            mLifecycleCallbacks.disableConnector();
            mVTrackClient.close(true);
        }

        /**
         * Reads our JSON-stored edits from memory and submits them to our EditState. Overwrites
         * any existing edits at the time that it is run.
         * <p/>
         * applyVariantsAndEventBindings should be called any time we load new event bindings from
         * disk or when we receive new edits from the interactive UI editor.
         * Changes and event bindings from our persistent storage and temporary changes
         * received from interactive editing will all be submitted to our EditState, tweaks
         * will be updated, and experiment statuses will be tracked.
         */
        private void applyVariantsAndEventBindings() {
            final List<Pair<String, ViewVisitor>> newVisitors =
                    new ArrayList<Pair<String, ViewVisitor>>();

            LogUtils.i(TAG, String.format(Locale.CHINA, "Event bindings are loaded. %d events from VTrack editor "
                            + "，%d events from VTrack configure",
                    mVTrackEventBindings.size(), mPersistentEventBindings.size()));

            if (mVTrackEventBindings.size() > 0) {
                // 如果mEditorEventBindings.size() > 0，说明连接了VTrack模拟器，只是用模拟器下发的事件配置
                for (Pair<String, JSONObject> changeInfo : mVTrackEventBindings) {
                    try {
                        final ViewVisitor visitor =
                                mProtocol.readEventBinding(changeInfo.second, mEventViewTracker);
                        newVisitors.add(new Pair<String, ViewVisitor>(changeInfo.first, visitor));
                    } catch (final VTrackProtocol.InapplicableInstructionsException e) {
                        LogUtils.i(TAG, e.getMessage());
                    } catch (final VTrackProtocol.BadInstructionsException e) {
                        LogUtils.i(TAG, "Bad editor event binding cannot be applied.", e);
                    }
                }
            } else {
                for (final Pair<String, JSONObject> changeInfo : mPersistentEventBindings) {
                    try {
                        final ViewVisitor visitor =
                                mProtocol.readEventBinding(changeInfo.second, mEventViewTracker);
                        newVisitors.add(new Pair<String, ViewVisitor>(changeInfo.first, visitor));
                    } catch (final VTrackProtocol.InapplicableInstructionsException e) {
                        LogUtils.i(TAG, e.getMessage());
                    } catch (final VTrackProtocol.BadInstructionsException e) {
                        LogUtils.i(TAG, "Bad persistent event binding cannot be applied.", e);
                    }
                }
            }

            final Map<String, List<ViewVisitor>> editMap = new HashMap<String, List<ViewVisitor>>();
            final int totalEdits = newVisitors.size();
            for (int i = 0; i < totalEdits; i++) {
                final Pair<String, ViewVisitor> next = newVisitors.get(i);
                final List<ViewVisitor> mapElement;
                if (editMap.containsKey(next.first)) {
                    mapElement = editMap.get(next.first);
                } else {
                    mapElement = new ArrayList<ViewVisitor>();
                    editMap.put(next.first, mapElement);
                }
                mapElement.add(next.second);
            }

            mEditState.setEdits(editMap);
        }

        private JSONObject setUpPayload(String type, JSONObject payload)
                throws JSONException, IOException {
            JSONObject response = new JSONObject();
            response.put("type", type);


            if (mUseGzip) {
                byte[] payloadData = payload.toString().getBytes();
                ByteArrayOutputStream os = new ByteArrayOutputStream(payloadData.length);
                GZIPOutputStream gos = new GZIPOutputStream(os);
                gos.write(payloadData);
                gos.close();
                byte[] compressed = os.toByteArray();
                os.close();
                response.put("gzip_payload", new String(Base64Coder.encode(compressed)));
            } else {
                response.put("payload", payload);
            }

            return response;
        }

        private SharedPreferences getSharedPreferences() {
            final String sharedPrefsName = SHARED_PREF_EDITS_FILE;
            return mContext.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        }
    }

    private class VTrackClientConnection implements VTrackClient.ClientConnection {

        @Override
        public void sendSnapshot(JSONObject message) {
            final Message msg =
                    mMessageThreadHandler.obtainMessage(VTrackSupported.MESSAGE_SEND_STATE_FOR_EDITING);
            msg.obj = message;
            mMessageThreadHandler.sendMessage(msg);
        }

        @Override
        public void bindEvents(JSONObject message) {
            final Message msg =
                    mMessageThreadHandler.obtainMessage(VTrackSupported.MESSAGE_HANDLE_EDITOR_BINDINGS_RECEIVED);
            msg.obj = message;
            mMessageThreadHandler.sendMessage(msg);
        }

        @Override
        public void sendDeviceInfo(JSONObject message) {
            final Message msg = mMessageThreadHandler.obtainMessage(VTrackSupported.MESSAGE_SEND_DEVICE_INFO);
            msg.obj = message;
            mMessageThreadHandler.sendMessage(msg);
        }

        @Override
        public void cleanup() {
            final Message msg =
                    mMessageThreadHandler.obtainMessage(VTrackSupported.MESSAGE_HANDLE_EDITOR_CLOSED);
            mMessageThreadHandler.sendMessage(msg);
        }

        @Override
        public void disconnect() {
            mIsRetryConnect = false;
            final Message msg =
                    mMessageThreadHandler.obtainMessage(VTrackSupported.MESSAGE_HANDLE_DISCONNECT);
            mMessageThreadHandler.sendMessage(msg);
        }

        @Override
        public void onWebSocketOpen() {
            LogUtils.i(TAG, "onWebSocketOpen");

            mCurrentRetryTimes = 0;
            mIsRetryConnect = true;
        }

        @Override
        public void onWebSocketClose(int code) {
            LogUtils.i(TAG, "onWebSocketClose; mIsRetryConnect=" + mIsRetryConnect + ";"
                    + "mCurrentRetryTimes="
                    + mCurrentRetryTimes);

            if (code != CLOSE_CODE_NOCODE) {
                mIsRetryConnect = false;
                mCurrentRetryTimes = 0;
                return;
            }

            if (mCurrentRetryTimes >= CONNECT_RETRY_TIMES) {
                mIsRetryConnect = false;
            }

            if (mIsRetryConnect) {
                final Message message = mMessageThreadHandler.obtainMessage(MESSAGE_CONNECT_TO_EDITOR);
                mMessageThreadHandler.sendMessageDelayed(message, RETRY_TIME_INTERVAL);
                mCurrentRetryTimes++;
            }
        }
    }
}
