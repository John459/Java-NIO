package server;

import datastructures.ConcurrentCASQueue;

import java.nio.channels.SocketChannel;

/**
 * Created by john on 4/16/2016.
 */
public class ConcurrentCASQueueWorker extends AbstractWorker {

    private static final int NUM_THREADS = 5;
    //lock free queue
    private ConcurrentCASQueue<Integer> concurrentQueue;

    public ConcurrentCASQueueWorker() {
        super("ccq", NUM_THREADS);
        this.concurrentQueue = new ConcurrentCASQueue<>();
    }

    private void processData(ServerDataEvent event) {
        if (event.getData() == null) {
            return;
        }

        Server server = event.getServer();
        SocketChannel socket = event.getSocket();

        String data = new String(event.getData());

        if (data.equalsIgnoreCase("clear")) {
            this.concurrentQueue = new ConcurrentCASQueue<>();
            this.addEvent(new ServerDataEvent(server, socket, ("cleared queue").getBytes(), false));
        }

        if (data.equalsIgnoreCase("print")) {
            this.addEvent(new ServerDataEvent(server, socket,
                    this.concurrentQueue.toString().getBytes(), false));
            return;
        }

        if (data.equalsIgnoreCase("deq")) {
            try {
                int value = this.concurrentQueue.deq();
            } catch (Exception e) {
                this.addEvent(new ServerDataEvent(server, socket, e.getMessage().getBytes(), false));
            } finally {
                return;
            }
        }

        if (data.startsWith("deq")) {
            try {
                int numDeqs = Integer.parseInt(data.split(" ")[1]);
                System.out.println("Dequeuing " + numDeqs + " items");
                for (int i = 0; i < numDeqs; i++) {
                    this.addEvent(new ServerDataEvent(server, socket, "deq".getBytes(), true));
                }
                this.addEvent(new ServerDataEvent(server, socket,
                        "print".getBytes(), true));
            } catch (NumberFormatException e) {
                this.addEvent(new ServerDataEvent(server, socket, "invalid argument".getBytes(), false));
            } finally {
                return;
            }
        }

        if (data.startsWith("len")) {
            int length = this.concurrentQueue.length();
            this.addEvent(new ServerDataEvent(server, socket, ("Length: " + length).getBytes(), false));
            return;
        }

        if (data.startsWith("enqr")) {
            try {
                int minValue;
                int maxValue;
                if (!data.contains("-")) {
                    minValue = Integer.parseInt(data.split(" ")[1]);
                    maxValue = minValue+1;
                } else {
                    String[] strRange = data.split(" ")[1].split("-");
                    minValue = Integer.parseInt(strRange[0]);
                    maxValue = Integer.parseInt(strRange[1]);
                }
                System.out.println("enqueuing " + minValue + " to " + maxValue);
                for (int i = minValue; i < maxValue; i++) {
                    this.addEvent(new ServerDataEvent(server, socket, ("enq " + i).getBytes(), true));
                }
                this.addEvent(new ServerDataEvent(server, socket, "print".getBytes(), true));
            } catch (NumberFormatException e) {
                this.addEvent(new ServerDataEvent(server, socket, "invalid argument".getBytes(), false));
            } finally {
                return;
            }
        }

        if (data.startsWith("enq")) {
            try {
                int value = Integer.parseInt(data.split(" ")[1]);
                this.concurrentQueue.enq(value);
            } catch (NumberFormatException e) {
                this.addEvent(new ServerDataEvent(server, socket, "invalid argument".getBytes(), false));
            } finally {
                return;
            }
        }
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
