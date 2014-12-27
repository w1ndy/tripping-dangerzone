package kvstore;

import static kvstore.KVConstants.DEL_REQ;
import static kvstore.KVConstants.GET_REQ;
import static kvstore.KVConstants.PUT_REQ;
import static kvstore.KVConstants.RESP;
import static kvstore.KVConstants.SUCCESS;

import java.io.IOException;
import java.net.Socket;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * Uses a thread pool to ensure that none of its methods are blocking.
 */
public class ServerClientHandler implements NetworkHandler {

    public KVServer kvServer;
    public ThreadPool threadPool;

    /**
     * Constructs a ServerClientHandler with ThreadPool of a single thread.
     *
     * @param kvServer KVServer to carry out requests
     */
    public ServerClientHandler(KVServer kvServer) {
        this(kvServer, 1);
    }

    /**
     * Constructs a ServerClientHandler with ThreadPool of thread equal to
     * the number passed in as connections.
     *
     * @param kvServer KVServer to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public ServerClientHandler(KVServer kvServer, int connections) {
        this.kvServer = kvServer;
        threadPool = new ThreadPool(connections);
    }

    /**
     * Creates a job to service the request for a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param client Socket connected to the client with the request
     */
    @Override
    public void handle(final Socket client) {
        System.out.println("Ready to handle request...");
        try {
            threadPool.addJob(new Runnable() {
                @Override
                public void run() {
                    handleRequest(client);
                }
            });
        } catch(InterruptedException e) {}
    }
    
    public void handleRequest(Socket client) {
        System.out.println("Handling request...");
        try {
            KVMessage msg = new KVMessage(client);
            if(msg.getMsgType().equals(GET_REQ)) {
                String key = msg.getKey();
                String value = kvServer.get(key);
                msg = new KVMessage(RESP);
                msg.setKey(key);
                msg.setValue(value);
                msg.sendMessage(client);
            } else if(msg.getMsgType().equals(PUT_REQ)) {
                kvServer.put(msg.getKey(), msg.getValue());
                (new KVMessage(RESP, SUCCESS)).sendMessage(client);
            } else if(msg.getMsgType().equals(DEL_REQ)) {
                kvServer.del(msg.getKey());
                (new KVMessage(RESP, SUCCESS)).sendMessage(client);
            }
        } catch (KVException e) {
            System.out.println("Faulty request: " + e.toString());
            try {
                e.getKVMessage().sendMessage(client);
            } catch(KVException s) {}
        }
        System.out.println("Request handled.");
    }

}
