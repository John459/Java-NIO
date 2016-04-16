package server;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by john on 3/13/2016.
 * An abstract class which specifies the structure of our workers.
 */
public abstract class AbstractWorker implements Runnable {

    //a unique identifier for each worker type
    final String WORKER_FUNCTION;
    final Queue<ServerDataEvent> eventQueue;
    private ExecutorService executor;


    public AbstractWorker(final String WORKER_FUNCTION, final int NUM_THREADS) {
        this.WORKER_FUNCTION = WORKER_FUNCTION;
        this.executor = Executors.newFixedThreadPool(NUM_THREADS);
        this.eventQueue = new LinkedList<>();
    }

    //ran by a thread in the thread pool.
    //responsible for performing actions based on the event passed in.
    public abstract void parseEvent(ServerDataEvent event);

    //a method will be used to add events for our worker to process
    public void addEvent(ServerDataEvent event) {
        synchronized (eventQueue) {
            eventQueue.add(event);
            eventQueue.notify(); //tell our worker that there is an event for it to process
        }
    }

    @Override
    public void run() {
        ServerDataEvent event;
        while (true) {
            synchronized (eventQueue) {
                while (eventQueue.isEmpty()) {
                    try {
                        eventQueue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                event = eventQueue.poll();
            }
            ThreadPoolEvent threadPoolEvent = new ThreadPoolEvent(event);
            executor.execute(threadPoolEvent);
        }
    }

    public class ThreadPoolEvent implements Runnable {
        private ServerDataEvent event;

        public ThreadPoolEvent(ServerDataEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            parseEvent(event);
        }
    }

}
