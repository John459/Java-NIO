package datastructures;

/**
 * Created by john on 4/16/2016.
 */
public interface ConcurrentQueue<T> {

    void enq(T value);
    T deq() throws Exception;
    int length();

}
