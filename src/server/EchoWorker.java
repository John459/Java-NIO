package server;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by john on 3/12/2016.
 * A worker which echos back the data it was passed.
 */
public class EchoWorker extends AbstractWorker {

    //the queue of events that this worker needs to process
    private final Queue<ServerDataEvent> queue = new LinkedList<>();

    public EchoWorker() {
        super("echo");
    }

    public void addEvent(ServerDataEvent event) {
        synchronized (queue) {
            queue.add(event);
            queue.notify(); //tell our worker that there is an event for it to process
        }
    }

    public void processData(ServerDataEvent event) {
        //simply send back the data we were given
        addEvent(new ServerDataEvent(event.getServer(), event.getSocket(), event.getData(), false));
    }

    public void run() {
        ServerDataEvent event;

        while (true) {
            synchronized (queue) {
                while (queue.isEmpty()) {
                    try {
                        queue.wait(); //wait until we're notified that an event is available
                    } catch (InterruptedException e) {}
                }
                event = queue.poll();
            }
            //if the data needs processing, then process the data
            if (event.processEvent()) {
                processData(event);
            //if not, tell the server that we'd like to send our data
            } else {
                event.getServer().send(event.getSocket(), event.getData());
            }
        }
    }

}
