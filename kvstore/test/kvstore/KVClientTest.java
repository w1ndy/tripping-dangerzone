package kvstore;

import static autograder.TestUtils.*;
import static kvstore.KVConstants.*;
import static kvstore.Utils.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.IOException;
import java.net.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import autograder.AGCategories.AGTestDetails;
import autograder.AGCategories.AG_PROJ3_CODE;

@RunWith(PowerMockRunner.class)
@PrepareForTest(KVClient.class)
public class KVClientTest {

    KVClient client;
    KVMessage msg;
    Socket sock;

    @Before
    public void setupClient() throws IOException {
        String hostname = InetAddress.getLocalHost().getHostAddress();
        client = new KVClient(hostname, 8080);
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Client should raise ERROR_COULD_NOT_CONNECT on UnknownHostException")
    public void testInvalidHost() throws Exception {
        setupSocketHostException();
        try {
            client = new KVClient("not_a_real_host", 8000);
            client.put("this should", "fail now");
            fail("Put did not throw a KVException!");
        } catch (KVException kve) {
            // Flexible since the sp14 spec was too vague.
            String errMsg = kve.getKVMessage().getMessage();
            assertTrue(errMsg.equals(ERROR_COULD_NOT_CONNECT) ||
                errMsg.equals(ERROR_COULD_NOT_CREATE_SOCKET));
        }
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Client should raise ERROR_COULD_NOT_CREATE_SOCKET on IOException")
    public void testSocketErrors() throws Exception {
        setupSocketIOException();
        try {
            client.put("foo", "bar");
            fail("Expected error on IOException during Socket creation");
        } catch (KVException kve) {
            // Flexible since the sp14 spec was too vague.
            String errMsg = kve.getKVMessage().getMessage();
            assertTrue(errMsg.equals(ERROR_COULD_NOT_CONNECT) ||
                errMsg.equals(ERROR_COULD_NOT_CREATE_SOCKET));
        }
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Client should accept successful put response")
    public void testPut() throws Exception {
        setupSocketSuccess();
        when(msg.getMsgType()).thenReturn(RESP);
        when(msg.getMessage()).thenReturn(SUCCESS);
        try {
            client.put("foo", "bar");
        } catch (KVException e) {
            fail("unexpected KVException on valid put request");
        }
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Client should accept successful get response")
    public void testGet() throws Exception {
        setupSocketSuccess();
        String key = "foo";
        String val = "bar";
        when(msg.getMsgType()).thenReturn(RESP);
        when(msg.getKey()).thenReturn(key);
        when(msg.getValue()).thenReturn(val);
        try {
            assertEquals(val, client.get(key));
        } catch (KVException e) {
            fail("Client threw unexpected exception!");
        }
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Client should raise KVException on failed get response")
    public void testGetFail() throws Exception {
        setupSocketSuccess();
        when(msg.getMsgType()).thenReturn(RESP);
        when(msg.getMessage()).thenReturn(ERROR_NO_SUCH_KEY);
        try {
            client.get("key");
            fail("Client did not throw exception!");
        } catch (KVException kve) {
            assertKVExceptionEquals(ERROR_NO_SUCH_KEY, kve);
        }
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "IOException on socket close should be handled cleanly")
    public void testCloseFail() throws Exception {
        setupSocketSuccess();
        when(msg.getMsgType()).thenReturn(RESP);
        when(msg.getMessage()).thenReturn(SUCCESS);
        doThrow(new IOException()).when(sock).close();
        try {
            client.put("key", "value");
        } catch (Exception e) {
            fail("Unexpected exception thrown!");
        }
    }

    /* ----------------------- BEGIN HELPER METHODS ------------------------ */

    private void setupSocketSuccess() throws Exception {
        sock = mock(Socket.class);
        whenNew(Socket.class).withArguments(anyString(), anyInt()).thenReturn(sock);
        msg = mock(KVMessage.class);
        whenNew(KVMessage.class).withAnyArguments().thenReturn(msg);
        whenNew(KVMessage.class).withArguments(any(Socket.class), anyInt()).thenReturn(msg);
    }

    private void setupSocketHostException() throws Exception {
        whenNew(Socket.class).withArguments(anyString(), anyInt())
            .thenThrow(new UnknownHostException());
    }

    private void setupSocketIOException() throws Exception {
        whenNew(Socket.class).withArguments(anyString(), anyInt())
            .thenThrow(new IOException());
    }

}
