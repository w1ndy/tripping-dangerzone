package kvstore;

import static autograder.TestUtils.kTimeoutQuick;
import static autograder.TestUtils.kTimeoutSlow;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.*;

import org.junit.*;
import org.junit.experimental.categories.Category;

import autograder.AGCategories.AGTestDetails;
import autograder.AGCategories.AG_PROJ3_CODE;

public class SocketServerTest {

    static String localhostName;
    SocketServer ss, srv;

    @BeforeClass
    public static void findLocalhostName() {
        try {
            localhostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            System.err.println(e.getMessage());
            localhostName = "localhost";
        }
    }

    @Before
    public void setup() {
        ss = new SocketServer(localhostName);
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 2,
        desc = "Check that the port is updated for the getter if randomly assigned")
    public void serverCanConnect() throws IOException {
		ss.connect();

        assertEquals(localhostName, ss.getHostname());
        assertNotEquals(0, ss.getPort());
        // the following is done simply to close the server socket.
        ss.stop();
        ss.start();
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 2,
        desc = "")
    public void testCS() throws IOException {
        srv = new SocketServer(localhostName, 8080);
        ServerClientHandler handler = new ServerClientHandler(new KVServer(2, 100), 5);
        srv.connect();
        srv.addHandler(handler);
        new Thread(new Runnable() {
            public void run() {
                try {
                    srv.start();
                } catch(Exception e) {
                    //fail(e.toString());
                }
            }
        }).start();

        try {
            KVClient c = new KVClient(localhostName, srv.getPort());
            System.out.println("Putting...");
            c.put("a", "1");
            System.out.println("Reading...");
            assertEquals(c.get("a"), "1");
            c.del("a");
        } catch (KVException e) {
            fail(e.getMessage());
        }

        ss.stop();
    }


}
