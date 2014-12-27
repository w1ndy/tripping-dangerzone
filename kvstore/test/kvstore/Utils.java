package kvstore;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.*;
import java.net.*;
import java.util.Random;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class Utils {
    static void registerWithMaster(long slaveID, int port) throws IOException, KVException {
        String hostname = InetAddress.getLocalHost().getHostAddress();
        Socket master = new Socket(hostname, 9090);
        KVMessage regMessage = new KVMessage(
                "register", slaveID + "@" + hostname+ ":" + port);
        regMessage.sendMessage(master);
        new KVMessage(master);
        master.close();
    }

    static boolean containsMsg(Exception e, String msg) {
        return (((KVException) e).getKVMessage().getMessage()).toLowerCase()
                .contains(msg.toLowerCase());
    }

    static String makeLongString(int n) {
        return new String(new char[n]).replace('\0', 'a');
    }

    static String truncateString(String s) {
        if (s == null) {
            return "<null>";
        }
        int n = s.length();
        if (n > 20) {
            return String.format("%s...", s.substring(0, 20));
        } else {
            return s;
        }
    }

    static void assertKVExceptionEquals(String expectedErrorMessage, KVException kve) {
        assertEquals(expectedErrorMessage, kve.getKVMessage().getMessage());
    }

    static void assertKVExceptionEquals(String message, String expectedErrorMessage, KVException kve) {
        assertEquals(message, expectedErrorMessage, kve.getKVMessage().getMessage());
    }

    /* Adapted from http://stackoverflow.com/a/41156 */
    public static class RandomString {

        private static char[] symbols;

        static {
            StringBuilder tmp = new StringBuilder();
            for (char ch = '0'; ch <= '9'; ++ch)
                tmp.append(ch);
            for (char ch = 'a'; ch <= 'z'; ++ch)
                tmp.append(ch);
            symbols = tmp.toString().toCharArray();
        }

        private final Random random = new Random();

        private final char[] buf;

        public RandomString(int maxLength) {
            if (maxLength < 0)
                throw new IllegalArgumentException("length < 0: " + maxLength);
            buf = new char[maxLength];
        }

        public String nextString() {
            for (int idx = 0; idx < buf.length; ++idx)
                buf[idx] = symbols[random.nextInt(symbols.length)];
            return new String(buf, 0, random.nextInt(buf.length));
        }
    }

    public static class ErrorLogger {
        private String err = null;
        public ErrorLogger() {
        }
        public synchronized void logError(String err) {
            this.err = err;
        }
        public boolean allPass() {
            return err == null;
        }
        public String getError() {
            return err;
        }
    }

    @SuppressWarnings(value = "rawtypes")
    static void setupMockThreadPool() throws Exception {
        ThreadPool mockTP = mock(ThreadPool.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                ((Runnable) args[0]).run();
                return null;
                }
            }).when(mockTP).addJob((any(Runnable.class)));
        whenNew(ThreadPool.class).withArguments(anyInt()).thenReturn(mockTP);
    }

    static Socket setupReadFromFile(String filename) {
        Socket sock = mock(Socket.class);
        URL fileLocation = ClassLoader.getSystemResource(filename);
        try {
            doNothing().when(sock).setSoTimeout(anyInt());
            when(sock.getInputStream()).thenReturn(new FileInputStream(fileLocation.getPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sock;
    }

}
