package kvstore;

import static autograder.TestUtils.kTimeoutQuick;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.*;

import org.junit.*;
import org.junit.experimental.categories.Category;

import autograder.AGCategories.AGTestDetails;
import autograder.AGCategories.AG_PROJ3_CODE;

public class SocketServerTest {

    static String localhostName;
    SocketServer ss;

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


}
