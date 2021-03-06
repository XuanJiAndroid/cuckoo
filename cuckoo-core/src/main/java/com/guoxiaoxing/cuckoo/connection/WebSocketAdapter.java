package com.guoxiaoxing.cuckoo.connection;

import com.guoxiaoxing.cuckoo.connection.drafts.Draft;
import com.guoxiaoxing.cuckoo.connection.exception.InvalidDataException;
import com.guoxiaoxing.cuckoo.connection.exception.InvalidHandshakeException;
import com.guoxiaoxing.cuckoo.connection.framing.Framedata;
import com.guoxiaoxing.cuckoo.connection.framing.Framedata.Opcode;
import com.guoxiaoxing.cuckoo.connection.framing.FramedataImpl1;
import com.guoxiaoxing.cuckoo.connection.handshake.ClientHandshake;
import com.guoxiaoxing.cuckoo.connection.handshake.HandshakeImpl1Server;
import com.guoxiaoxing.cuckoo.connection.handshake.ServerHandshake;
import com.guoxiaoxing.cuckoo.connection.handshake.ServerHandshakeBuilder;

import java.net.InetSocketAddress;

/**
 * This class default implements all methods of the WebSocketListener that can be overridden optionally when advances functionalities is needed.<br>
 **/
public abstract class WebSocketAdapter implements WebSocketListener {

    /**
     * This default implementation does not do anything. Go ahead and overwrite it.
     *
     * @see com.guoxiaoxing.cuckoo.connection.WebSocketListener#onWebsocketHandshakeReceivedAsServer(WebSocket, Draft, ClientHandshake)
     */
    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
        return new HandshakeImpl1Server();
    }

    @Override
    public void onWebsocketHandshakeReceivedAsClient(WebSocket conn, ClientHandshake request, ServerHandshake response) throws InvalidDataException {
    }

    /**
     * This default implementation does not do anything which will cause the connections to always progress.
     *
     * @see com.guoxiaoxing.cuckoo.connection.WebSocketListener#onWebsocketHandshakeSentAsClient(WebSocket, ClientHandshake)
     */
    @Override
    public void onWebsocketHandshakeSentAsClient(WebSocket conn, ClientHandshake request) throws InvalidDataException {
    }

    /**
     * This default implementation does not do anything. Go ahead and overwrite it
     *
     * @see com.guoxiaoxing.cuckoo.connection.WebSocketListener#onWebsocketMessageFragment(WebSocket, Framedata)
     */
    @Override
    public void onWebsocketMessageFragment(WebSocket conn, Framedata frame) {
    }

    /**
     * This default implementation will send a pong in response to the received ping.
     * The pong frame will have the same payload as the ping frame.
     *
     * @see com.guoxiaoxing.cuckoo.connection.WebSocketListener#onWebsocketPing(WebSocket, Framedata)
     */
    @Override
    public void onWebsocketPing(WebSocket conn, Framedata f) {
        FramedataImpl1 resp = new FramedataImpl1(f);
        resp.setOptcode(Opcode.PONG);
        conn.sendFrame(resp);
    }

    /**
     * This default implementation does not do anything. Go ahead and overwrite it.
     *
     * @see com.guoxiaoxing.cuckoo.connection.WebSocketListener#onWebsocketPong(WebSocket, Framedata)
     */
    @Override
    public void onWebsocketPong(WebSocket conn, Framedata f) {
    }

    /**
     * Gets the XML string that should be returned if a client requests a Flash
     * security policy.
     *
     * The default implementation allows access from all remote domains, but
     * only on the port that this WebSocketServer is listening on.
     *
     * This is specifically implemented for gitime's WebSocket client for Flash:
     * http://github.com/gimite/web-socket-js
     *
     * @return An XML String that comforts to Flash's security policy. You MUST
     * not include the null char at the end, it is appended automatically.
     * @throws InvalidDataException thrown when some data that is required to generate the flash-policy like the websocket local port could not be obtained e.g because the websocket is not connected.
     */
    @Override
    public String getFlashPolicy(WebSocket conn) throws InvalidDataException {
        InetSocketAddress adr = conn.getLocalSocketAddress();
        if (null == adr) {
            throw new InvalidHandshakeException("socket not bound");
        }

        StringBuffer sb = new StringBuffer(90);
        sb.append("<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"");
        sb.append(adr.getPort());
        sb.append("\" /></cross-domain-policy>\0");

        return sb.toString();
    }

}
