package datastructures;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by john on 4/16/2016.
 */
public class ConcurrentCASQueue<T> implements ConcurrentQueue<T> {

    private AtomicReference<Node> head;
    private AtomicReference<Node> tail;
    private AtomicInteger length;

    public ConcurrentCASQueue() {
        this.head = new AtomicReference<>(new Node(null));
        this.tail = new AtomicReference<>(head.get());
        this.length = new AtomicInteger(0);
    }

    public void enq(T value) {
        Node node = new Node(value);
        while (true) {
            Node last = tail.get();
            Node next = last.next.get();
            if (last == tail.get()) {
                if (next == null) {
                    if (last.next.compareAndSet(next, node)) {
                        tail.compareAndSet(last, node);
                        length.getAndIncrement();
                        return;
                    }
                } else {
                    tail.compareAndSet(last, next);
                }
            }
        }
    }

    public T deq() throws Exception {
        while (true) {
            Node first = head.get();
            Node last = tail.get();
            Node next = first.next.get();
            if (first == head.get()) {
                if (first == last) {
                    if (next == null) {
                        throw new Exception("Deqed Empty");
                    }
                    tail.compareAndSet(last, next);
                } else {
                    T value = next.value;
                    if (head.compareAndSet(first, next)) {
                        length.getAndDecrement();
                        return value;
                    }
                }
            }
        }
    }

    public int length() {
        return length.get();
    }

    public String toString() {
        Node node = head.get().next.get();
        if (node == null) {
            return "Empty";
        }

        StringBuilder stringBuilder = new StringBuilder();
        while (node != null) {
            stringBuilder.append(node.value);
            stringBuilder.append(" ");
            node = node.next.get();
        }
        return stringBuilder.toString();
    }

    private class Node {
        T value;
        AtomicReference<Node> next;

        public Node(T value) {
            this.value = value;
            this.next = new AtomicReference<>(null);
        }
    }

}
