package kvstore;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

public class SampleSlave {

    static String logPath;
    static TPCLog log;

    static KVServer keyServer;
    static SocketServer server;

    static long slaveID;
    static String masterHostname;

    static int masterPort = 8080;
    static int registrationPort = 9090;

    public static void main(String[] args) throws IOException, KVException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Need master IP address");
        }

        Random rand = new Random();
        slaveID = rand.nextLong();

        masterHostname = args[0];
        if (masterHostname.charAt(0) == '$') {
            masterHostname = InetAddress.getLocalHost().getHostAddress();
        }
        System.out.println("Looking for master at " + masterHostname);

        server = new SocketServer(InetAddress.getLocalHost().getHostAddress());

        keyServer = new KVServer(100, 10);
        logPath = "bin/log." + slaveID + "@" + server.getHostname();
        log = new TPCLog(logPath, keyServer);

        TPCMasterHandler handler = new TPCMasterHandler(slaveID, keyServer, log);
        server.addHandler(handler);
        server.connect();



        handler.registerWithMaster(masterHostname, server);

        System.out.println("Starting SlaveServer " + slaveID + " at " +
            server.getHostname() + ":" + server.getPort());
        server.start();
    }

}
