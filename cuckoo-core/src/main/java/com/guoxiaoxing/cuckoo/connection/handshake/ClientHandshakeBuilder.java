package com.guoxiaoxing.cuckoo.connection.handshake;

public interface ClientHandshakeBuilder extends HandshakeBuilder, ClientHandshake {
    public void setResourceDescriptor(String resourceDescriptor);
}
