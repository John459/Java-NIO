package server.datastructures;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by john on 4/15/2016.
 */
public class UnboundedTotalQueue<T> {

    private Node head;
    private Node tail;
    private ReentrantLock enqLock;
    private ReentrantLock deqLock;
    private AtomicInteger length;

    public UnboundedTotalQueue() {
        this.head = new Node(null);
        this.tail = head;
        this.enqLock = new ReentrantLock();
        this.deqLock = new ReentrantLock();
        this.length = new AtomicInteger(0);
    }

    public void enq(T value) {
        enqLock.lock();
        try {
            Node node = new Node(value);
            tail.next = node;
            tail = node;
            length.getAndIncrement();
        } finally {
            enqLock.unlock();
        }
    }

    public T deq() throws Exception {
        T result;
        deqLock.lock();
        try {
            if (head.next == null) {
                throw new Exception("Can't deq from empty queue");
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
