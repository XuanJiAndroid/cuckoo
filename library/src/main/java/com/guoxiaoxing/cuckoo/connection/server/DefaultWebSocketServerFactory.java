package com.guoxiaoxing.cuckoo.connection.server;

import com.guoxiaoxing.cuckoo.connection.WebSocketAdapter;
import com.guoxiaoxing.cuckoo.connection.WebSocketImpl;
import com.guoxiaoxing.cuckoo.connection.drafts.Draft;
import com.guoxiaoxing.cuckoo.connection.server.WebSocketServer.WebSocketServerFactory;

import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;

public class DefaultWebSocketServerFactory implements WebSocketServerFactory {
    @Override
    public WebSocketImpl createWebSocket(WebSocketAdapter a, Draft d, Socket s) {
        return new WebSocketImpl(a, d);
    }

    @Override
    public WebSocketImpl createWebSocket(WebSocketAdapter a, List<Draft> d, Socket s) {
        return new WebSocketImpl(a, d);
    }

    @Override
    public SocketChannel wrapChannel(SocketChannel channel, SelectionKey key) {
        return (SocketChannel) channel;
    }
}