package com.guoxiaoxing.cuckoo.debug;

import org.json.JSONObject;

public interface DebugTracking {
    void reportTrack(JSONObject eventJson);
}
