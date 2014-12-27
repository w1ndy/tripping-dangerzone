package kvstore;

import java.io.IOException;
import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;

public class EndToEndTemplate {

    KVClient client;
    ServerRunner serverRunner;

    @Before
    public void setUp() throws IOException, InterruptedException {
        String hostname = InetAddress.getLocalHost().getHostAddress();

        SocketServer ss = new SocketServer(hostname, 8080);
        ss.addHandler(new ServerClientHandler(new KVServer(100, 10), 20));
        serverRunner = new ServerRunner(ss, "server");
        serverRunner.start();

        client = new KVClient(hostname, 8080);
    }

    @After
    public void tearDown() throws InterruptedException {
        serverRunner.stop();
    }

}
