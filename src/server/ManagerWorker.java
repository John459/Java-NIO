package server;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by john on 3/12/2016.
 * A worker whose job it is to manage other workers
 */
public class ManagerWorker extends AbstractWorker {

    //the list of workers which are managed by this manager
    private List<AbstractWorker> workers = new LinkedList<>();
    private static final int NUM_THREADS = 1;

    public ManagerWorker() {
        super("manager", NUM_THREADS);
    }

    public void addWorker(AbstractWorker worker) {
        //create a new thread for the new worker and start it
        new Thread(worker).start();
        //add the worker to our list of workers to manage
        workers.add(worker);
    }

    private void callWorkers(String command, ServerDataEvent event) {
        //for all workers whose function is equal to the command passed,
        //add the corresponding event to that worker's event eventQueue.
        for (AbstractWorker worker : workers) {
            if (!worker.WORKER_FUNCTION.equalsIgnoreCase(command)) {
                continue;
            }
            worker.addEvent(event);
        }
    }

    private void processEvent(ServerDataEvent event) {
        String dataString = new String(event.getData());

        //break up the data string into its command and arguments
        String command;
        byte[] args;
        if (!dataString.contains(":")) {
            command = dataString;
            args = null;
        } else {
            command = dataString.substring(0, dataString.indexOf(':')).trim();
            args = dataString.substring(dataString.indexOf(':') + 1).trim().getBytes();
        }

        //create a new event whose data is equivalent to the arguments of the command
        ServerDataEvent managedEvent = new ServerDataEvent(event.getServer(), event.getSocket(), args, true);
        //pass this event to all workers who are able to process it.
        callWorkers(command, managedEvent);
    }

    public void parseEvent(ServerDataEvent event) {
        //process the passed event.
        //Note: Manager Workers never respond to the server.
        //that is the job of the workers that it manages.
        this.processEvent(event);
    }
}
