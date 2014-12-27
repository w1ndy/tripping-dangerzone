package kvstore;

import static autograder.TestUtils.kTimeoutDefault;
import static autograder.TestUtils.kTimeoutQuick;
import static kvstore.KVConstants.*;
import static kvstore.Utils.assertKVExceptionEquals;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import autograder.AGCategories.AGTestDetails;
import autograder.AGCategories.AG_PROJ4_CODE;

@RunWith(PowerMockRunner.class)
@PrepareForTest(KVServer.class)
public class KVServerTest {

    KVServer server;
    static KVCache realCache;
    KVCache mockCache;
    static KVStore realStore;
    KVStore mockStore;

    /**
     * Nick: This is necessary because once I start mocking constructors, I
     * haven't figured out a way to use the real ones again.  Also, this sucks
     * because the cache doesn't get reset between tests -_- there is no method
     * for that, though and I can't construct a new cache with the actual
     * constructor.  Only fix I thought of was creating an array of new caches
     * and using a different one for each test, but that's even worse imo :(
     */
    @BeforeClass
    public static void setupRealDependencies() {
        realCache = new KVCache(10, 10);
        realStore = new KVStore();
    }

    public void setupRealServer() {
        try {
            whenNew(KVCache.class).withArguments(anyInt(), anyInt()).thenReturn(realCache);
            whenNew(KVStore.class).withNoArguments().thenReturn(realStore);
            server = new KVServer(10, 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setupMockServer() {
        try {
            mockCache = mock(KVCache.class);
            mockStore = mock(KVStore.class);
            whenNew(KVCache.class).withAnyArguments().thenReturn(mockCache);
            whenNew(KVStore.class).withAnyArguments().thenReturn(mockStore);
            server = new KVServer(10, 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test(timeout = 30000)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 3, desc = "Randomized put/get/del sequence test.")
    public void randStressTest() throws KVException {
        setupRealServer();
        Random rand = new Random(8); // no reason for 8
        Map<String, String> map = new HashMap<String, String>(10000);
        String key, val;
        for (int i = 0; i < 10000; i++) {
            key = Integer.toString(rand.nextInt());
            val = Integer.toString(rand.nextInt());
            server.put(key, val);
            map.put(key, val);
        }
        Iterator<Map.Entry<String, String>> mapIter = map.entrySet().iterator();
        Map.Entry<String, String> pair;
        while(mapIter.hasNext()) {
            pair = mapIter.next();
            assertTrue(server.hasKey(pair.getKey()));
            assertEquals(pair.getValue(), server.get(pair.getKey()));
            server.del(pair.getKey());
            assertTrue(!server.hasKey(pair.getKey()));
            mapIter.remove();
        }

        assertTrue(map.size() == 0);
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1, desc = "Test put throws ERROR_OVERSIZED_KEY")
    public void testPutOversizedKeyKVException() {
        setupMockServer();
        Scanner s2 = null;
        try {
            final String filename = "gobears_maxkey.txt";
            InputStream maxKeyStream = getClass().getClassLoader().getResourceAsStream(filename);
            assertNotNull(String.format("Test file not found: %s - Please report to TA", filename), maxKeyStream);
            s2 = new Scanner(maxKeyStream);
            String oversizedKey = s2.nextLine();
            when(mockCache.getLock(oversizedKey)).thenReturn(new ReentrantLock());
            server.put(oversizedKey, "cal");
            fail("Server was supposed to throw an exception for oversized key");
        } catch (KVException pass) {
            assertKVExceptionEquals(ERROR_OVERSIZED_KEY, pass);
        }
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1, desc = "Test put throws ERROR_OVERSIZED_VALUE")
    public void testPutOversizedValueKVException() {
        setupMockServer();
        Scanner s2 = null;
        try {
            final String filename = "gobears_maxvalue.txt";
            InputStream maxValueStream = getClass().getClassLoader().getResourceAsStream(filename);
            assertNotNull(String.format("Test file not found: %s - Please report to TA", filename), maxValueStream);
            s2 = new Scanner(maxValueStream);
            String oversizedValue = s2.nextLine();
            when(mockCache.getLock(oversizedValue)).thenReturn(new ReentrantLock());
            server.put("foo", oversizedValue);
            fail("Server was supposed to throw an exception for oversized value");
        } catch (KVException pass){
            assertKVExceptionEquals(ERROR_OVERSIZED_VALUE, pass);
        }
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 2, desc = "Test get throws ERROR_NO_SUCH_KEY.")
    public void testGetKVException() {
        setupRealServer();
        try {
            server.get("this key shouldn't be here");
            fail("get with nonexistent key should error");
        } catch (KVException e) {
            assertEquals(KVConstants.RESP, e.getKVMessage().getMsgType());
            assertEquals(KVConstants.ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1, desc = "Test get releases lock cleanly on exception")
    public void testGetLockRelease() {
        setupMockServer();
        ReentrantLock l = new ReentrantLock();
        try {
            when(mockCache.getLock("go")).thenReturn(new ReentrantLock());
            when(mockStore.get("go")).thenThrow(new KVException(ERROR_NO_SUCH_KEY));
            server.get("go");
            fail("Forced exception was not rethrown by KVServer");
        } catch(KVException e1) {
        }
        assertTrue(!(l.isLocked()));

    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 2, desc = "Test del throws ERROR_NO_SUCH_KEY.")
    public void testDelKVException() {
        setupRealServer();
        try {
            server.del("this key shouldn't be here");
            fail("del with nonexistent key should error");
        } catch (KVException e) {
            assertEquals(KVConstants.RESP, e.getKVMessage().getMsgType());
            assertEquals(KVConstants.ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1, desc = "Test del releases lock cleanly on exception")
    public void testDelLockRelease() {
        setupMockServer();
        ReentrantLock l = new ReentrantLock();
        try {
            when(mockCache.getLock("go")).thenReturn(l);
            doThrow(new KVException(ERROR_NO_SUCH_KEY)).when(mockStore).del("go");;
            server.del("go");
            fail("Forced exception was not rethrown by KVServer");
        } catch (KVException e1) {
        }
        assertTrue(!(l.isLocked()));
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 3,
        desc = "Tests that the store is not accessed if a value can be " +
               "retrieved from the cache.")
    public void testGetFromCacheFirst() {
        setupMockServer();
        when(mockCache.getLock("go")).thenReturn(new ReentrantLock());
        when(mockCache.get("go")).thenReturn("bears");
        try {
            server.get("go");
        } catch (KVException e) {
            fail("Threw unnecessary KVException during get");
        }
        try {
            verify(mockStore, never()).get("go");
        } catch (KVException e) {
        }
    }

    @Test (timeout = kTimeoutQuick)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Checks that gets are parallel across sets and serial within " +
               "a set. Checks the set is locked to ensure atomicity within a set.")
    public void testParallelGet() {
        setupMockServer();
        ReentrantLock l1 = new ReentrantLock();
        ReentrantLock l2 = new ReentrantLock();
        when(mockCache.getLock("cal")).thenReturn(l1);
        when(mockCache.getLock("stan")).thenReturn(l2);
        when(mockCache.get("cal")).thenAnswer(checkParallelSerial1);
        try {
            when(mockStore.get("cal")).thenAnswer(checkParallelSerial2);
        } catch (KVException e){
            fail("Unexpected exception on get");
        }

        when(mockCache.get("stan")).thenReturn("furd");
        try {
            String s = server.get("cal");
            assertTrue(s.equals("gobears"));
        } catch (KVException e){
            fail("Unexpected failure.");
        }
    }

    @Test (timeout = kTimeoutQuick)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Checks that dels are parallel across sets and serial within " +
               "a set. Checks the set is locked to ensure atomicity within a set.")
    public void testParallelDel() {
        setupMockServer();
        ReentrantLock l1 = new ReentrantLock();
        ReentrantLock l2 = new ReentrantLock();
        when(mockCache.getLock("cal")).thenReturn(l1);
        when(mockCache.getLock("stan")).thenReturn(l2);
        doAnswer(checkParallelSerial1).when(mockCache).del("cal");
        try {
            doAnswer(checkParallelSerial2).when(mockStore).del("cal");
        } catch (KVException e) {
            fail("Unexpected exception on del");
        }

        when(mockCache.get("stan")).thenReturn("furd");
        try {
            server.del("cal");
        } catch (KVException e) {
            fail("Unexpected failure.");
        }
    }

    @Test (timeout = kTimeoutQuick)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Checks that puts are parallel across sets and serial within " +
               "a set. Checks the set is locked to ensure atomicity within a set.")
    public void testParallelPut() {
        setupMockServer();
        ReentrantLock l1 = new ReentrantLock();
        ReentrantLock l2 = new ReentrantLock();
        when(mockCache.getLock("cal")).thenReturn(l1);
        when(mockCache.getLock("stan")).thenReturn(l2);
        doAnswer(checkParallelSerial1).when(mockCache).put("cal", "gobears");
        //try {
        doAnswer(checkParallelSerial2).when(mockStore).put("cal", "gobears");
        //} catch (KVException e) {
        //    fail("Unexpected exception on put");
        //}

        when(mockCache.get("stan")).thenReturn("furd");
        try {
            server.put("cal", "gobears");
        } catch (KVException e) {
            fail("Unexpected failure.");
        }
    }

    /* ----------------------- BEGIN HELPER METHODS ------------------------ */

    @SuppressWarnings("rawtypes")
    Answer checkParallelSerial2 = new Answer() {
        @Override
        public Object answer(InvocationOnMock inv){
            ReentrantLock l = (ReentrantLock) mockCache.getLock("cal");
            assertTrue(l.isLocked());
            try {
                assertTrue(server.get("stan").equals("furd"));
            } catch (KVException e) {
                fail("unexpected exception");
            }
            return "gobears";
        }
    };

    @SuppressWarnings("rawtypes")
    Answer checkParallelSerial1 = new Answer() {
        @Override
        public Object answer(InvocationOnMock inv) {
            ReentrantLock l = (ReentrantLock) mockCache.getLock("cal");
            assertTrue(l.isLocked());
            try {
                assertTrue(server.get("stan").equals("furd"));
            } catch (KVException e) {
                fail("unexpected exception");
            }
            return null;
        }
    };

    // George: Not sure why Isaac commented this out.
    // @Test (timeout = 5000)
    // public void testParallelOps(){
    //     setupMockServer();
    //     ReentrantLock l1 = new ReentrantLock();
    //     ReentrantLock l2 = new ReentrantLock();
    //     when(mockCache.getLock("cal")).thenReturn(l1);
    //     when(mockCache.getLock("stan")).thenReturn(l2);

    //     when(mockCache.get("cal")).thenReturn(l1.isLocked() ? "gobears" : "fail");

    //     l2.lock();
    //     try{
    //         when(mockStore.get("cal")).thenReturn(l1.isLocked() ? "gobears" : "fail");
    //         String s = server.get("cal");
    //         //System.out.println(s);
    //         assertTrue(s.equals("gobears"));
    //     } catch (KVException e){
    //         fail("Unexpected failure.");
    //     }

    // }

}
