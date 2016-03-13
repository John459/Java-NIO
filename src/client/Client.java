package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

/**
 * Created by john on 3/11/2016.
 * A nonblocking concurrent asynchronous java nio client
 */
public class Client implements Runnable {

    private InetAddress host;
    private int port;
    private Selector selector; //selects which channels to process
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192); //what we will read into
    //the list of change requests that the client needs to perform
    private final List<ChangeRequest> changeRequests = new LinkedList<>();
    //data which is waiting to be sent to the server
    private final Map<SocketChannel, Queue<ByteBuffer>> pendingData = new HashMap<>();
    //a map which maps each channel to a corresponding response handler to handle that channel's responses
    private final Map<SocketChannel, ResponseHandler> responseHandlers = Collections.synchronizedMap(new HashMap<>());

    public Client(InetAddress host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.selector = this.initSelector();
    }

    private Selector initSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    private SocketChannel initConnection() throws IOException {
        //create a new channel for this specific connection
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        //Begin connecting this new channel to the server
        socketChannel.connect(new InetSocketAddress(this.host, this.port));

        //indicate to the selector that we'd like to create a new connection on a new channel
        synchronized (this.changeRequests) {
            this.changeRequests.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
        }

        return socketChannel;
    }

    private void finishConnection(SelectionKey key) {
        //get the channel associated with this key
        SocketChannel socketChannel  = (SocketChannel) key.channel();

        try {
            //finishing connecting the channel
            socketChannel.finishConnect();
        } catch (IOException e) {
            key.cancel();
            return;
        }

        //tell the selector that this channel is now interested in processing write requests.
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void handleResponse(SocketChannel socketChannel, byte[] data, int amount) throws IOException {
        byte[] response = new byte[amount];
        System.arraycopy(data, 0, response, 0 ,amount);

        //get the response handler associated with this channel
        ResponseHandler responseHandler = this.responseHandlers.get(socketChannel);

        //if the response was handled correctly, then close this connection
        if (responseHandler.handleResponse(response)) {
            socketChannel.close();
            socketChannel.keyFor(this.selector).cancel();
        }
    }

    public void send(byte[] data, ResponseHandler handler) throws IOException {
        //create a new connection to send the data on
        SocketChannel socketChannel = this.initConnection();

        //create a new mapping between this new channel and the response handler
        this.responseHandlers.put(socketChannel, handler);

        //add the data we'd like to write to the queue of pending data.
        synchronized (this.pendingData) {
            Queue<ByteBuffer> byteBufferQueue = this.pendingData.get(socketChannel);
            if (byteBufferQueue == null) {
                byteBufferQueue = new LinkedList<>();
                this.pendingData.put(socketChannel, byteBufferQueue);
            }
            byteBufferQueue.add(ByteBuffer.wrap(data));
        }

        //tell the selector that work is available
        this.selector.wakeup();
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
            Queue<ByteBuffer> dataQueue = this.pendingData.get(socketChannel);
            if (dataQueue == null) {
                return;
            }

            //write data until our queue is empty
            while (!dataQueue.isEmpty()) {
                ByteBuffer buffer = dataQueue.peek();
                socketChannel.write(buffer);
                if (buffer.remaining() > 0) {
                    //we'll write the rest on the next pass
                    break;
                }
                dataQueue.remove();
            }

            if (dataQueue.isEmpty()) {
                //tell the selector that this channel is now interested in reading.
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel sChannel = (SocketChannel) key.channel();

        this.readBuffer.clear();

        int read;
        try {
            read = sChannel.read(this.readBuffer);
        } catch (IOException e) {
            key.cancel();
            sChannel.close();
            return;
        }

        if (read == -1) {
            key.channel().close();
            key.cancel();
            return;
        }

        this.handleResponse(sChannel, this.readBuffer.array(), read);
    }

    public void run() {
        while (true) {
            try {
                //process our change requests
                synchronized (this.changeRequests) {
                    for (ChangeRequest changeRequest : this.changeRequests) {
                        switch (changeRequest.type) {
                            case ChangeRequest.CHANGEOPS:
                                SelectionKey key = changeRequest.socket.keyFor(this.selector);
                                key.interestOps(changeRequest.ops);
                                break;
                            case ChangeRequest.REGISTER:
                                changeRequest.socket.register(this.selector, changeRequest.ops);
                                break;
                        }
                    }
                    //prevent selecting thread from reapplying old change requests
                    this.changeRequests.clear();
                }

                //find all keys which have their associated channels ready to perform operations
                this.selector.select();

                for (SelectionKey key : this.selector.selectedKeys()) {
                    if (!key.isValid()) {
                        //the channel has been closed.
                        continue;
                    }

                    //true if the channel is ready to connect to the server
                    if (key.isConnectable()) {
                        this.finishConnection(key);
                    //true if the channel is ready to read
                    } else if (key.isReadable()) {
                        this.read(key);
                    //true if the channel is ready to write
                    } else if (key.isWritable()) {
                        this.write(key);
                    }
                }
                //prevent selector from reprocessing old keys
                this.selector.selectedKeys().clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            //create a new client, whose channels will connect to localhost:9090
            Client client = new Client(InetAddress.getLocalHost(), 9090);
            Thread t = new Thread(client);
            t.setDaemon(true);
            t.start();

            //a response handler which will process responses synchronously
            ResponseHandler synchronousResponseHandler = new ResponseHandler(true);
            //a response handler which will process responses asynchronously
            ResponseHandler asynchronousResponseHandler = new ResponseHandler(false);

            //tell the server to reset the counter.
            //do this synchronously because we don't want to start counting until we know the counter is at 0.
            client.send("count: reset".getBytes(), synchronousResponseHandler);
            synchronousResponseHandler.waitForResponse();


            for (int i = 0; i < 1000; i++) {
                //send an message telling the server to increment the counter 1000 times.
                //wait for the response asynchronously.
                client.send(("count: 1000").getBytes(), asynchronousResponseHandler);
            }

            //tell the server to respond once the counter has hit 1000000.
            //do this synchronously because we do not want to proceed until the counting is done.
            client.send("count: check 1000000".getBytes(), synchronousResponseHandler);
            synchronousResponseHandler.waitForResponse();

            //At this point we know we've counted to 1000000

            //tell the server to echo back our message.
            //do this synchronously because we don't want to exit until the message has been received.
            client.send("echo: Counting completed successfully".getBytes(), synchronousResponseHandler);
            synchronousResponseHandler.waitForResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
