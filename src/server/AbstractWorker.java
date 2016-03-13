package server;

/**
 * Created by john on 3/13/2016.
 * An abstract class which specifies the structure of our workers.
 */
public abstract class AbstractWorker implements Runnable {

    //a unique identifier for each worker type
    final String WORKER_FUNCTION;

    public AbstractWorker(final String WORKER_FUNCTION) {
        this.WORKER_FUNCTION = WORKER_FUNCTION;
    }

    //a method will be used to add events for our worker to process
    public abstract void addEvent(ServerDataEvent event);

}
