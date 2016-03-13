package server;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by john on 3/12/2016.
 * A worker which increments a counter and prints its value.
 */
public class CounterWorker extends AbstractWorker {

    //the queue of events that this worker needs to process
    private final Queue<ServerDataEvent> eventQueue = new LinkedList<>();
    private AtomicInteger counter = new AtomicInteger(0);

    public CounterWorker() {
        super("count");
    }

    public void addEvent(ServerDataEvent e) {
        synchronized (eventQueue) {
            eventQueue.add(e);
            eventQueue.notify(); //tell our worker that there is an event for it to process
        }
    }

    private void processData(ServerDataEvent event) {
        Server server = event.getServer();
        SocketChannel socket = event.getSocket();

        //if no data was passed to our worker, just increment the counter
        if (event.getData() == null) {
            counter.incrementAndGet();
            return;
        }

        //convert the byte array into the string it represents
        String data = new String(event.getData());

        //reset the counter
        if (data.equalsIgnoreCase("reset")) {
            counter.set(0);
            //send the counter's value (0) back to the server
            this.addEvent(new ServerDataEvent(server, socket, (counter + "").getBytes(), false));
            return;
        }
        //print the counter's current value
        if (data.equalsIgnoreCase("print")) {
            this.addEvent(new ServerDataEvent(server, socket, ("Count: " + counter.get()).getBytes(), false));
            return;
        }
        //check if the counter's value is equal to some specified value.
        //if it is, respond with that value.
        //if not, add this event to the queue again and check later.
        if (data.startsWith("check")) {
            try {
                int value = Integer.parseInt(data.split(" ")[1]);
                if (counter.get() == value) {
                    this.addEvent(new ServerDataEvent(server, socket, ("checked: " + counter.get()).getBytes(), false));
                } else {
                    this.addEvent(new ServerDataEvent(server, socket, data.getBytes(), true));
                }
            } catch (NumberFormatException e) {
                this.addEvent(new ServerDataEvent(server, socket, "invalid argument".getBytes(), false));
            } finally {
                return;
            }
        }

        try {
            //get how many times the counter should be incremented
            int increment = Integer.parseInt(data);
            if (increment < 0) {
                this.addEvent(new ServerDataEvent(server, socket, "invalid increment".getBytes(), false));
                return;
            }

            //increment the counter the specified number of times
            for (int i = 0; i < increment; i++) {
                this.addEvent(new ServerDataEvent(server, socket, null, true));
            }

            //add an event to print our counter's value
            this.addEvent(new ServerDataEvent(server, socket, ("print").getBytes(), true));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            this.addEvent(new ServerDataEvent(event.getServer(), event.getSocket(),
                    e.toString().getBytes(), false));
            return;
        }
    }


    public void run() {
        while (true) {
            ServerDataEvent event;
            synchronized (eventQueue) {
                while (eventQueue.isEmpty()) {
                    try {
                        eventQueue.wait(); //wait until we're notified that an event is available
                    } catch (InterruptedException e) {}
                }
                event = eventQueue.poll();
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
