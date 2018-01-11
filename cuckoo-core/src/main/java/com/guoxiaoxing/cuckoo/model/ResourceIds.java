package com.guoxiaoxing.cuckoo.model;

/**
 * SDK内部接口
 */
public interface ResourceIds {

    boolean knownIdName(String name);

    int idFromName(String name);

    String nameForId(int id);
}
