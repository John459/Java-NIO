package tpcclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by john on 4/16/2016.
 */
public class TPCQueueExample {

    private enum TPCQueueType {
        LOCK_QUEUE, CAS_QUEUE
    }

    private final String IDENTIFIER;

    public TPCQueueExample(TPCQueueType queueType) {
        switch (queueType) {
            case LOCK_QUEUE:
                this.IDENTIFIER = "clq";
                break;
            case CAS_QUEUE:
                this.IDENTIFIER = "ccq";
                break;
            default:
                this.IDENTIFIER = null;
                break;
        }
    }

    public void send(final String IDENTIFIER, String data, TPCResponseHandler handler) throws IOException {
        Socket socket = new Socket(InetAddress.getLocalHost(), 9090);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        handler.setClient(socket, in);
        String output = IDENTIFIER + ": " + data;
        out.println(output);
        handler.handleResponse();
        socket.close();
    }

    public long runExample() throws IOException {
        TPCResponseHandler synchronousResponseHandler = new TPCResponseHandler(true) {
            @Override
            protected void actOnResponse() {
                System.out.println(new String(this.response));
            }
        };
        TPCResponseHandler asynchronousResponseHandler = new TPCResponseHandler(false) {
            @Override
            protected void actOnResponse() {
                //System.out.println(new String(this.response));
            }
        };

        long start = System.currentTimeMillis();

        send(IDENTIFIER, "clear", synchronousResponseHandler);

        for (int i = 0; i < 101000; i+= 1000) {
            send(IDENTIFIER, "enqr " + i + "-"+(i+1000), asynchronousResponseHandler);
            send(IDENTIFIER, "deq 1000", asynchronousResponseHandler);
        }

        send(IDENTIFIER, "len", synchronousResponseHandler);

        send(IDENTIFIER, "print", synchronousResponseHandler);

        long end = System.currentTimeMillis();
        return end - start;
    }

    public static void main(String[] args) {
        try {
            TPCQueueExample queueExample = new TPCQueueExample(TPCQueueType.LOCK_QUEUE);
            System.out.println(queueExample.runExample());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
