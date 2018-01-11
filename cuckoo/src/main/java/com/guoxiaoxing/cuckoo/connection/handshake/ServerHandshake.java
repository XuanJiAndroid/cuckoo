package com.guoxiaoxing.cuckoo.connection.handshake;

public interface ServerHandshake extends Handshakedata {
    public short getHttpStatus();

    public String getHttpStatusMessage();
}
