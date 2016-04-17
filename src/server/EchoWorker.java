package server;

/**
 * Created by john on 3/12/2016.
 * A worker which echos back the data it was passed.
 */
public class EchoWorker extends AbstractWorker {

    private static final int NUM_THREADS = 1;

    public EchoWorker() {
        super("echo", NUM_THREADS);
    }

    public void addEvent(ServerDataEvent event) {
        synchronized (this.eventQueue) {
            this.eventQueue.add(event);
            this.eventQueue.notify(); //tell our worker that there is an event for it to process
        }
    }

    public void processData(ServerDataEvent event) {
        //simply send back the data we were given
        addEvent(new ServerDataEvent(event.getServer(), event.getSocket(), event.getData(), false));
    }

    public void parseEvent(ServerDataEvent event) {
        if (event.processEvent()) {
            processData(event);
        //if not, tell the server that we'd like to send our data
        } else {
            event.getServer().send(event.getSocket(), event.getData());
        }
    }

}
