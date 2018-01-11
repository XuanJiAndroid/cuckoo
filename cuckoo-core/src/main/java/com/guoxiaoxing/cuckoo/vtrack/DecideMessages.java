package com.guoxiaoxing.cuckoo.vtrack;

import com.guoxiaoxing.cuckoo.vtrack.VTrack;

import org.json.JSONArray;

// Will be called from both customer threads and the Cuckoo worker thread.
public class DecideMessages {

    public DecideMessages(VTrack vTrack) {
        mVTrack = vTrack;
    }

    public synchronized void setEventBindings(JSONArray eventBindings) {
        mVTrack.setEventBindings(eventBindings);
    }

    public synchronized void setVTrackServer(String vtrackServer) {
        mVTrack.setVTrackServer(vtrackServer);
    }

    private final VTrack mVTrack;

    private static final String LOGTAG = "SA.DecideMessages";
}
