package client;

import java.nio.channels.SocketChannel;

/**
 * Created by john on 3/12/2016.
 * This class is a simple container for holding change requests.
 * A specific change request indicates which operation it'd like to perform,
 * as well the the associated channel the operation should be performed on.
 */
public class ChangeRequest {

    public static final int REGISTER = 1;
    public static final int CHANGEOPS = 2;
    public SocketChannel socket;
    public int type;
    public int ops;

    public ChangeRequest(SocketChannel socket, int type, int ops) {
        this.socket = socket;
        this.type = type;
        this.ops = ops;
    }

}
