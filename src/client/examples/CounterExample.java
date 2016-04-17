package client.examples;

import client.Client;
import client.ResponseHandler;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by john on 4/17/2016.
 */
public class CounterExample {

    private static final String IDENTIFIER = "count";

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
                System.out.println(new String(this.response));
            }
        };

        long start = System.currentTimeMillis();

        client.send(IDENTIFIER, "reset", synchronousResponseHandler);
        synchronousResponseHandler.waitForResponse();

        for (int i = 0; i < 1000; i++) {
            client.send(IDENTIFIER, 1000 + "", asynchronousResponseHandler);
        }

        client.send(IDENTIFIER, "check 1000000", synchronousResponseHandler);
        synchronousResponseHandler.waitForResponse();

        client.send("echo", "counting finished successfully", synchronousResponseHandler);
        synchronousResponseHandler.waitForResponse();

        long end = System.currentTimeMillis();
        return end - start;
    }

    public static void main(String[] args) {
        try {
            Client client = Client.createClient(InetAddress.getLocalHost(), 9090);
            CounterExample counterExample = new CounterExample();

            System.out.println(counterExample.runExample(client));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
