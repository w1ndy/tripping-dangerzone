package kvstore;

import java.net.InetAddress;

public class SampleServer {

    public static void main(String[] args) {
        try {
            String hostname = InetAddress.getLocalHost().getHostAddress();
            SocketServer ss = new SocketServer(hostname, 8080);
            ss.addHandler(new ServerClientHandler(new KVServer(100, 10)));
            ss.connect();
            System.out.println("Server listening for clients at " + ss.getHostname());
            ss.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
