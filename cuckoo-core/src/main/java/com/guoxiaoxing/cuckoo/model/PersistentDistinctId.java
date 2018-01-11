package com.guoxiaoxing.cuckoo.model;

import android.content.SharedPreferences;

import java.util.UUID;
import java.util.concurrent.Future;

public class PersistentDistinctId extends PersistentIdentity<String> {
    public PersistentDistinctId(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "events_distinct_id", new PersistentSerializer<String>() {
            @Override
            public String load(String value) {
                return value;
            }

            @Override
            public String save(String item) {
                return item;
            }

            @Override
            public String create() {
                return UUID.randomUUID().toString();
            }
        });
    }
}
