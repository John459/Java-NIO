package tpcclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by john on 4/16/2016.
 */
public abstract class TPCResponseHandler {
    protected String response = null;
    private BufferedReader bufferedReader;
    private Socket socket;
    private boolean synchronous;

    public TPCResponseHandler(boolean synchronous) {
        this.synchronous = synchronous;
    }

    protected abstract void actOnResponse();

    public void setClient(Socket socket, BufferedReader bufferedReader) {
        this.socket = socket;
        this.bufferedReader = bufferedReader;
    }

    public synchronized boolean handleResponse() throws IOException {
        this.response = bufferedReader.readLine();
        actOnResponse();
        this.socket.close();
        return true;
    }
}
