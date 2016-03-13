package client;

/**
 * Created by john on 3/12/2016.
 * Handles both synchronous and asynchronous responses
 */
public class ResponseHandler {

    private byte[] response = null;
    private boolean synchronous;

    public ResponseHandler(boolean synchronous) {
        this.synchronous = synchronous;
    }

    private void actOnResponse() {
        System.out.println(new String(response));
    }

    public synchronized boolean handleResponse(byte[] response) {
        this.response = response;
        if (synchronous) {
            //if we want to handle responses synchronously, we notify ourselves that a response is ready.
            this.notify();
        } else {
            //if we want to handle responses asynchronously, we handle this response immediately.
            actOnResponse();
        }
        return true;
    }

    public synchronized void waitForResponse() {
        while (this.response == null) {
            try {
                //block until we've been notified that a response is ready
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //handle the response, and reset to null for reuse.
        actOnResponse();
        this.response = null;
    }

}
