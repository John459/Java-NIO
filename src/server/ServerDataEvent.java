package server;

import java.nio.channels.SocketChannel;

/**
 * Created by john on 3/12/2016.
 * A holder for data which needs to be processed by the workers
 */
public class ServerDataEvent {

    private Server server;
    private SocketChannel socket;
    private byte[] data;
    //true if the data needs more processing,
    //false if the data is ready to be sent
    private boolean processEvent;

    public ServerDataEvent(Server server, SocketChannel socket, byte[] data, boolean processEvent) {
        this.server = server;
        this.socket = socket;
        this.data = data;
        this.processEvent = processEvent;
    }

    public Server getServer() {
        return server;
    }

    public SocketChannel getSocket() {
        return socket;
    }

    public byte[] getData() {
        return data;
    }

    public boolean processEvent() {
        return processEvent;
    }

}
