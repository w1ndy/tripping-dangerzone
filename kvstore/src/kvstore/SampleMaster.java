package kvstore;

import java.io.IOException;
import java.net.InetAddress;

public class SampleMaster {

    static SocketServer clientSocketServer;
    static SocketServer slaveSocketServer;
    static TPCMaster tpcMaster;

    public static void main(String[] args) throws IOException, InterruptedException {
        final String hostname = InetAddress.getLocalHost().getHostAddress();
        tpcMaster = new TPCMaster(2, new KVCache(1, 4));

        new Thread() {
            @Override
            public void run() {
                slaveSocketServer = new SocketServer(hostname, 9090);
                NetworkHandler slaveHandler = new TPCRegistrationHandler(tpcMaster);
                slaveSocketServer.addHandler(slaveHandler);
                try {
                    slaveSocketServer.connect();
                    System.out.println("Starting registration server in background...");
                    slaveSocketServer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        Thread.sleep(100);
        clientSocketServer = new SocketServer(hostname, 8080);
        NetworkHandler clientHandler = new TPCClientHandler(tpcMaster);
        clientSocketServer.addHandler(clientHandler);
        clientSocketServer.connect();

        System.out.println("Master listening for clients and slaves at " +
            clientSocketServer.getHostname());
        clientSocketServer.start();
    }

}
