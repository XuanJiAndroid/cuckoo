package com.guoxiaoxing.cuckoo.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * ExpandableListView
 */
public interface ExpandableListViewItemTrackProperties {
    /**
     * 点击 groupPosition、childPosition 处 item 的扩展属性
     *
     * @param groupPosition
     * @param childPosition
     * @return
     * @throws JSONException
     */
    JSONObject getSensorsChildItemTrackProperties(int groupPosition, int childPosition) throws JSONException;

    /**
     * 点击 groupPosition 处 item 的扩展属性
     *
     * @param groupPosition
     * @return
     * @throws JSONException
     */
    JSONObject getSensorsGroupItemTrackProperties(int groupPosition) throws JSONException;
}