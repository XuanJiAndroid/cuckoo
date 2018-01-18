package com.guoxiaoxing.cuckoo.vtrack;

import android.util.Log;

import com.guoxiaoxing.cuckoo.Cuckoo;
import com.guoxiaoxing.cuckoo.connection.client.WebSocketClient;
import com.guoxiaoxing.cuckoo.connection.drafts.Draft_17;
import com.guoxiaoxing.cuckoo.connection.exception.NotSendableException;
import com.guoxiaoxing.cuckoo.connection.exception.WebsocketNotConnectedException;
import com.guoxiaoxing.cuckoo.connection.framing.Framedata;
import com.guoxiaoxing.cuckoo.connection.handshake.ServerHandshake;
import com.guoxiaoxing.cuckoo.util.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * VTrackWebSocketClient should handle all communication to and from the socket. It should be fairly naive and
 * only know how to delegate messages to the ABHandler class.
 */
public class VTrackClient {

    private static final String TAG = "$Cuckoo.VTrackClient";

    private final URI mURI;
    private final ClientConnection mClientConnection;
    private final VTrackWebSocketClient mWebSocketClient;

    private static final int CONNECT_TIMEOUT = 1000;
    private static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocate(0);


    public class EditorConnectionException extends IOException {
        private static final long serialVersionUID = -1884953175346045636L;

        public EditorConnectionException(Throwable cause) {
            super(cause.getMessage()); // IOException(cause) is only available in API level 9!
        }
    }

    public interface ClientConnection {
        void sendSnapshot(JSONObject message);

        void bindEvents(JSONObject message);

        void sendDeviceInfo(JSONObject message);

        void cleanup();

        void disconnect();

        void onWebSocketOpen();

        void onWebSocketClose(int code);
    }

    public VTrackClient(URI uri, ClientConnection service)
            throws EditorConnectionException {
        mURI = uri;
        mClientConnection = service;
        try {
            mWebSocketClient = new VTrackWebSocketClient(uri, CONNECT_TIMEOUT);
            mWebSocketClient.connectBlocking();
        } catch (final InterruptedException e) {
            throw new EditorConnectionException(e);
        }
    }

    public boolean isValid() {
        return !mWebSocketClient.isClosed() && !mWebSocketClient.isClosing() && !mWebSocketClient.isFlushAndClose();
    }

    public BufferedOutputStream getBufferedOutputStream() {
        return new BufferedOutputStream(new WebSocketOutputStream());
    }

    public void sendMessage(String message) {
        LogUtils.i(TAG, "Sending message: " + message);
        try {
            mWebSocketClient.send(message);
        } catch (Exception e) {
            LogUtils.i(TAG, "sendMessage;error", e);
        }
    }

    public void close(boolean block) {
        if (mWebSocketClient == null) {
            return;
        }
        try {
            if (block) {
                mWebSocketClient.closeBlocking();
            } else {
                mWebSocketClient.close();
            }
        } catch (Exception e) {
            LogUtils.i(TAG, "close;error", e);
        }
    }

    private class VTrackWebSocketClient extends WebSocketClient {

        public VTrackWebSocketClient(URI uri, int connectTimeout) throws InterruptedException {
            super(uri, new Draft_17(), null, connectTimeout);
        }

        @Override
        public void onOpen(ServerHandshake handShakeData) {
            if (Cuckoo.ENABLE_LOG) {
                LogUtils.i(TAG, "WebSocket connected: " + handShakeData.getHttpStatus() + " " + handShakeData
                        .getHttpStatusMessage());
            }

            mClientConnection.onWebSocketOpen();
        }

        @Override
        public void onMessage(String message) {
//      Log.i(LOGTAG, "Received message from editor:\n" + message);

            try {
                final JSONObject messageJson = new JSONObject(message);
                final String type = messageJson.getString("type");
                if (type.equals("device_info_request")) {
                    mClientConnection.sendDeviceInfo(messageJson);
                } else if (type.equals("snapshot_request")) {
                    mClientConnection.sendSnapshot(messageJson);
                } else if (type.equals("event_binding_request")) {
                    mClientConnection.bindEvents(messageJson);
                } else if (type.equals("disconnect")) {
                    mClientConnection.disconnect();
                }
            } catch (final JSONException e) {
                LogUtils.i(TAG, "Bad JSON received:" + message, e);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.i(TAG, "WebSocket closed. Code: " + code + ", reason: " + reason + "\nURI: " + mURI);
            mClientConnection.cleanup();
            mClientConnection.onWebSocketClose(code);
        }

        @Override
        public void onError(Exception ex) {
            if (ex != null && ex.getMessage() != null) {
                LogUtils.i(TAG, "Websocket Error: " + ex.getMessage());
            } else {
                LogUtils.i(TAG, "Unknown websocket error occurred");
            }
        }
    }


    private class WebSocketOutputStream extends OutputStream {
        @Override
        public void write(int b)
                throws EditorConnectionException {
            // This should never be called.
            final byte[] oneByte = new byte[1];
            oneByte[0] = (byte) b;
            write(oneByte, 0, 1);
        }

        @Override
        public void write(byte[] b)
                throws EditorConnectionException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len)
                throws EditorConnectionException {
            final ByteBuffer message = ByteBuffer.wrap(b, off, len);
            try {
                mWebSocketClient.sendFragmentedFrame(Framedata.Opcode.TEXT, message, false);
            } catch (final WebsocketNotConnectedException e) {
                throw new EditorConnectionException(e);
            } catch (final NotSendableException e) {
                throw new EditorConnectionException(e);
            }
        }

        @Override
        public void close()
                throws EditorConnectionException {
            try {
                mWebSocketClient.sendFragmentedFrame(Framedata.Opcode.TEXT, EMPTY_BYTE_BUFFER, true);
            } catch (final WebsocketNotConnectedException e) {
                throw new EditorConnectionException(e);
            } catch (final NotSendableException e) {
                throw new EditorConnectionException(e);
            }
        }
    }
}
