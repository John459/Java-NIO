package tpcserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by john on 4/16/2016.
 */
public class TPCServer {

    private static final int PORT = 9090;
    private ServerSocket listener;
    private ConcurrentQueueWorker concurrentQueueWorker = null;

    public TPCServer() throws IOException {
        listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Thread(new SocketThread(listener.accept())).start();
            }
        } finally {
            listener.close();
        }
    }

    private class SocketThread implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public SocketThread(Socket socket) throws IOException {
            this.socket = socket;
        }

        public void run() {
            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream(), true);
                while (true) {
                    String input = this.in.readLine();
                    if (input == null || !input.contains(":")) {
                        continue;
                    }
                    String[] parts = input.split(":");
                    if (concurrentQueueWorker == null) {
                        concurrentQueueWorker = new ConcurrentQueueWorker(parts[0].trim());
                    }
                    synchronized (concurrentQueueWorker) {
                        String result = concurrentQueueWorker.processData(parts[1].trim());
                        out.println(result + "\r\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            TPCServer tpcServer = new TPCServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
