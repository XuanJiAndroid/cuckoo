package com.guoxiaoxing.cuckoo.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 获取 ListView、GridView position 位置 Item 的 properties
 */
public interface ObjectTrackProperties {
    /**
     * Object 扩展属性
     *
     * @return
     * @throws JSONException
     */
    JSONObject getSensorsTrackProperties() throws JSONException;
}