package server;

import server.datastructures.UnboundedTotalQueue;

import java.nio.channels.SocketChannel;

/**
 * Created by john on 4/15/2016.
 */
public class ConcurrentLockedQueueWorker extends AbstractWorker {

    private static final int NUM_THREADS = 2;
    private UnboundedTotalQueue<Integer> concurrentQueue;

    public ConcurrentLockedQueueWorker() {
        super("clq", NUM_THREADS);
        this.concurrentQueue = new UnboundedTotalQueue<>();
    }

    private void processData(ServerDataEvent event) {
        if (event.getData() == null) {
            return;
        }

        Server server = event.getServer();
        SocketChannel socket = event.getSocket();

        String data = new String(event.getData());

        if (data.equalsIgnoreCase("clear")) {
            this.concurrentQueue = new UnboundedTotalQueue<>();
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
                this.addEvent(new ServerDataEvent(server, socket, ("dequeued: " + value).getBytes(), false));
            } catch (Exception e) {
                this.addEvent(new ServerDataEvent(server, socket, e.getMessage().getBytes(), false));
            } finally {
                return;
            }
        }

        if (data.startsWith("checklen")) {
            try {
                int value = Integer.parseInt(data.split(" ")[1]);
                if (concurrentQueue.length() == value) {
                    this.addEvent(new ServerDataEvent(server, socket, ("length = " + value).getBytes(), false));
                } else {
                    this.addEvent(new ServerDataEvent(server, socket, data.getBytes(), true));
                }
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
                this.addEvent(new ServerDataEvent(server, socket, ("enqueued: " + value).getBytes(), false));
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
