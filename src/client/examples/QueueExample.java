package client.examples;

import client.Client;
import client.ResponseHandler;
import datastructures.ConcurrentCASQueue;
import datastructures.ConcurrentLockQueue;
import datastructures.ConcurrentQueue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by john on 4/16/2016.
 */
public class QueueExample {

    private enum QueueType {
        LOCK_QUEUE, CAS_QUEUE
    }

    private final String IDENTIFIER;

    public QueueExample(QueueType queueType) {
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

    public long runExample(Client client) throws IOException {
        ResponseHandler synchronousResponseHandler = new ResponseHandler(true) {
            @Override
            protected void actOnResponse() {
                System.out.println(new String(this.response));
            }
        };
        ResponseHandler asynchronousResponseHandler = new ResponseHandler(false) {
            @Override
            protected void actOnResponse() {
                //System.out.println(new String(this.response));
            }
        };

        long start = System.currentTimeMillis();

        client.send(IDENTIFIER, "clear", synchronousResponseHandler);
        synchronousResponseHandler.waitForResponse();

        for (int i = 0; i < 101000; i+= 1000) {
            if (i < 91000) {
                client.send(IDENTIFIER, "enqr " + i + "-"+(i+1000), asynchronousResponseHandler);
            }
            if (i >= 10000) {
                client.send(IDENTIFIER, "deq 1000", asynchronousResponseHandler);
            }
        }

        client.send(IDENTIFIER, "len", synchronousResponseHandler);
        synchronousResponseHandler.waitForResponse();

        client.send(IDENTIFIER, "print", synchronousResponseHandler);
        synchronousResponseHandler.waitForResponse();

        long end = System.currentTimeMillis();
        return end - start;
    }

    public static void main(String[] args) {
        try {
            Client client = Client.createClient(InetAddress.getLocalHost(), 9090);
            QueueExample queueExample = new QueueExample(QueueType.CAS_QUEUE);

            System.out.println(queueExample.runExample(client));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
