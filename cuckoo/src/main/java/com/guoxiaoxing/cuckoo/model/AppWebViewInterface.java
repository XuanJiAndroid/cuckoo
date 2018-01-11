package com.guoxiaoxing.cuckoo.model;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.util.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class AppWebViewInterface {

    private static final String TAG = "Cuckoo.AppWebViewInterface";
    private Context mContext;
    private JSONObject properties;

    public AppWebViewInterface(Context c, JSONObject p) {
        this.mContext = c;
        this.properties = p;
    }

    @JavascriptInterface
    public String sensorsdata_call_app() {
        try {
            if (properties == null) {
                properties = new JSONObject();
            }
            properties.put("type", "Android");
            String loginId = Cuckoo.with(mContext).getLoginId();
            if (!TextUtils.isEmpty(loginId)) {
                properties.put("distinct_id", loginId);
                properties.put("is_login", true);
            } else {
                properties.put("distinct_id", Cuckoo.with(mContext).getAnonymousId());
                properties.put("is_login", false);
            }
            return properties.toString();
        } catch (JSONException e) {
            LogUtils.i(TAG, e.getMessage());
        }
        return null;
    }

    @JavascriptInterface
    public void sensorsdata_track(String event) {
        Cuckoo.with(mContext).trackEventFromH5(event);
    }
}
