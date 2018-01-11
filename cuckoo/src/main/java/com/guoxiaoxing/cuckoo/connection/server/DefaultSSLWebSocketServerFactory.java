package com.guoxiaoxing.cuckoo.connection.server;

import com.guoxiaoxing.cuckoo.connection.SSLSocketChannel2;
import com.guoxiaoxing.cuckoo.connection.WebSocketAdapter;
import com.guoxiaoxing.cuckoo.connection.WebSocketImpl;
import com.guoxiaoxing.cuckoo.connection.drafts.Draft;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;


public class DefaultSSLWebSocketServerFactory implements WebSocketServer.WebSocketServerFactory {
    protected SSLContext sslcontext;
    protected ExecutorService exec;

    public DefaultSSLWebSocketServerFactory(SSLContext sslContext) {
        this(sslContext, Executors.newSingleThreadScheduledExecutor());
    }

    public DefaultSSLWebSocketServerFactory(SSLContext sslContext, ExecutorService exec) {
        if (sslContext == null || exec == null)
            throw new IllegalArgumentException();
        this.sslcontext = sslContext;
        this.exec = exec;
    }

    @Override
    public ByteChannel wrapChannel(SocketChannel channel, SelectionKey key) throws IOException {
        SSLEngine e = sslcontext.createSSLEngine();
        e.setUseClientMode(false);
        return new SSLSocketChannel2(channel, e, exec, key);
    }

    @Override
    public WebSocketImpl createWebSocket(WebSocketAdapter a, Draft d, Socket c) {
        return new WebSocketImpl(a, d);
    }

    @Override
    public WebSocketImpl createWebSocket(WebSocketAdapter a, List<Draft> d, Socket s) {
        return new WebSocketImpl(a, d);
    }
}