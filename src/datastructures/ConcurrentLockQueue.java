package datastructures;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by john on 4/15/2016.
 */
public class ConcurrentLockQueue<T> implements ConcurrentQueue<T> {

    private Node head;
    private Node tail;
    private ReentrantLock enqLock;
    private ReentrantLock deqLock;
    private Condition deqCondition;
    private AtomicInteger length;

    public ConcurrentLockQueue() {
        this.head = new Node(null);
        this.tail = head;
        this.enqLock = new ReentrantLock();
        this.deqLock = new ReentrantLock();
        this.deqCondition = this.deqLock.newCondition();
        this.length = new AtomicInteger(0);
    }

    public void enq(T value) {
        enqLock.lock();
        try {
            Node node = new Node(value);
            tail.next = node;
            tail = node;
            if (length.getAndIncrement() == 0) {
                deqLock.lock();
                try {
                    deqCondition.signalAll();
                } finally {
                    deqLock.unlock();
                }
            }
        } finally {
            enqLock.unlock();
        }
    }

    public T deq() throws Exception {
        T result;
        deqLock.lock();
        try {
            while (head.next == null) {
                boolean signalReceived = deqCondition.await(10000, TimeUnit.MILLISECONDS);
                if (!signalReceived) {
                    throw new Exception("Cant dequeue from empty queue");
                }
            }
            result = head.next.value;
            head = head.next;
            length.getAndDecrement();
        } finally {
            deqLock.unlock();
        }
        return result;
    }

    public int length() {
        return length.get();
    }

    public String toString() {
        Node node = head.next;
        if (node == null) {
            return "Empty";
        }

        StringBuilder stringBuilder = new StringBuilder();
        while (node != null) {
            stringBuilder.append(node.value + " ");
            node = node.next;
        }
        return stringBuilder.toString();
    }

    private class Node {
        T value;
        Node next;

        public Node(T value) {
            this.value = value;
            this.next = null;
        }
    }

}
