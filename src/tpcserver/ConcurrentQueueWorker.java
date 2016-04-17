package tpcserver;

import datastructures.ConcurrentCASQueue;
import datastructures.ConcurrentLockQueue;
import datastructures.ConcurrentQueue;

/**
 * Created by john on 4/16/2016.
 */
public class ConcurrentQueueWorker {

    private ConcurrentQueue<Integer> concurrentQueue;

    public ConcurrentQueueWorker(String type) {
        switch (type) {
            case "clq":
                concurrentQueue = new ConcurrentLockQueue<>();
                break;
            case "ccq":
                concurrentQueue = new ConcurrentCASQueue<>();
                break;
            default:
                concurrentQueue = null;
                break;
        }
    }

    public String processData(String data) {
        if (data.equalsIgnoreCase("clear")) {
            this.concurrentQueue = new ConcurrentCASQueue<>();
            return "cleared queue";
        }

        if (data.equalsIgnoreCase("print")) {
            return this.concurrentQueue.toString();
        }

        if (data.equalsIgnoreCase("deq")) {
            try {
                int value = this.concurrentQueue.deq();
                return null;
            } catch (Exception e) {
                return e.getMessage();
            } finally {
                return null;
            }
        }

        if (data.startsWith("deq")) {
            try {
                int numDeqs = Integer.parseInt(data.split(" ")[1]);
                System.out.println("Dequeuing " + numDeqs + " items");
                for (int i = 0; i < numDeqs; i++) {
                    processData("deq");
                }
                return processData("print");
            } catch (NumberFormatException e) {
                return "invalid argument";
            } finally {
                return null;
            }
        }

        if (data.startsWith("len")) {
            int length = this.concurrentQueue.length();
            return length + "";
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
                    processData("enq " + i);
                }
                return processData("print");
            } catch (NumberFormatException e) {
                return "invalid argument";
            } finally {
                return null;
            }
        }

        if (data.startsWith("enq")) {
            try {
                int value = Integer.parseInt(data.split(" ")[1]);
                this.concurrentQueue.enq(value);
            } catch (NumberFormatException e) {
                return "invalid argument";
            } finally {
                return null;
            }
        }

        return null;
    }

}
