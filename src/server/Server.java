package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

/**
 * Created by john on 3/11/2016.
 * A nonblocking concurrent asynchronous nio server
 */
public class Server implements Runnable {

    public ManagerWorker managerWorker;

    private InetAddress host;
    private int port;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    //the list of change requests to be processed by the server
    private final List<ChangeRequest> changeRequests = new LinkedList<>();
    //a map which maps each channel to its queue of pending data that needs to be processed
    private final Map<SocketChannel, Queue<ByteBuffer>> pendingData = new HashMap<>();

    public Server(InetAddress host, int port, ManagerWorker managerWorker) throws IOException {
        this.host = host;
        this.port = port;
        this.selector = this.initSelector();
        this.managerWorker = managerWorker;
    }

    private Selector initSelector() throws IOException {
        //create a new nonblocking channel for our server to process requests with
        Selector selector = SelectorProvider.provider().openSelector();
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        //tell the selector that our server channel is interested in accepting connections
        InetSocketAddress sockAddr = new InetSocketAddress(this.host, this.port);
        serverChannel.socket().bind(sockAddr);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        return selector;
    }

    public void send(SocketChannel socket, byte[] data) {
        synchronized (this.changeRequests) {
            //tell the selector that our channel is interested i changing its operation to writing
            this.changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

            synchronized (this.pendingData) {
                //prepare our data for writing
                Queue<ByteBuffer> dataQueue = this.pendingData.get(socket);
                if (dataQueue == null) {
                    dataQueue = new LinkedList<>();
                    this.pendingData.put(socket, dataQueue);
                }
                dataQueue.add(ByteBuffer.wrap(data));
            }
        }
        //tell the selector that there is new work for it to process
        this.selector.wakeup();
    }

    private void write(SelectionKey key) throws IOException {
        //get the channel associated with this key
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
            //get the data associated with this channel
            Queue<ByteBuffer> dataQueue = this.pendingData.get(socketChannel);

            //write until our queue is empty
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
                //writing is done, so tell the selector we're interested in reading again
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();

        //accept a new client connection on a new channel.
        SocketChannel sChannel = channel.accept();
        sChannel.configureBlocking(false);
        //tell the selector that this channel is interested in reading
        sChannel.register(this.selector, SelectionKey.OP_READ);
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

        byte[] copy = new byte[read];
        System.arraycopy(this.readBuffer.array(), 0, copy, 0, read);
        //send the read data to the managerWorker for processing
        this.managerWorker.addEvent(new ServerDataEvent(this, sChannel, copy, true));
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

                    //true if the channel is ready to accept a connection
                    if (key.isAcceptable()) {
                        this.accept(key);
                    //true if the channel is ready to read
                    } else if (key.isReadable()) {
                        this.read(key);
                    //true if the channel is ready to write
                    } else if (key.isWritable()) {
                        this.write(key);
                    }
                }
                //prevent selecting thread from reprocessing old keys
                this.selector.selectedKeys().clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            //create a new worker whose job it is to manage other workers
            ManagerWorker managerWorker = new ManagerWorker();
            //tell the managerWorker to manage an echo worker
            managerWorker.addWorker(new EchoWorker());
            //tell the managerWorker to manage a counter worker
            managerWorker.addWorker(new CounterWorker());

            //run the managerWorker on a new thread
            new Thread(managerWorker).start();
            //run the server on a new thread
            new Thread(new Server(null, 9090, managerWorker)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
