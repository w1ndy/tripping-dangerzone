package kvstore;

import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;

import static autograder.TestUtils.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import autograder.AGCategories.AGTestDetails;
import autograder.AGCategories.AG_PROJ3_CODE;

public class ThreadPoolTest {

    protected ThreadPool threadPool;
    protected int[] array = new int[20];
    protected int count = 0;
    protected ReentrantLock lock = new ReentrantLock();

    protected Runnable r = new Runnable() {
        @Override
        public void run() {
            lock.lock();
            int j = count;
            count++;
            lock.unlock();
            array[j] = j;
        }
    };

    protected Runnable s = new Runnable() {
        @Override
        public void run() {
            try {
                lock.lock();
                int j = count;
                count++;
                lock.unlock();
                Thread.sleep(200);
                array[j] = j;
            } catch (InterruptedException e) {
                fail("InterruptedException");
            }
        }
    };

    protected Runnable t = new Runnable() {
        @Override
        public void run() {
            try {
                lock.lock();
                int j = count;
                count++;
                lock.unlock();
                Thread.sleep(20);
                array[j] = j;
            } catch (InterruptedException e) {
                fail("InterruptedException");
            }
        }
    };

    @Before
    public void setUp() {
        count = 0;
    }

    @Test(timeout = kTimeoutQuick)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Tests that a pool of one thread can handle multiple requests")
    public void testOneThread() throws InterruptedException {
        threadPool = new ThreadPool(1);
        threadPool.addJob(r);
        threadPool.addJob(r);
        Thread.sleep(200);
        assertTrue("A thread didn't increment the correct array entry", array[0] == 0);
        assertTrue("A thread didn't increment the correct array entry", array[1] == 1);
    }

    @Test(timeout = kTimeoutSlow)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Spawns multiple threads and checks that all requests are " +
               "executed by medium sized pool")
    public void testMultipleThreads() throws InterruptedException {
        threadPool = new ThreadPool(6);
        for (int i = 0; i < array.length; i++) {
            threadPool.addJob(r);
        }
        Thread.sleep(500);
        for (int i = 0; i < array.length; i++) {
            assertTrue(array[i] == i);
        }
    }

    @Test(timeout = kTimeoutSlow)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Spawns many threads and checks that more threads than tasks " +
               "still runs correctly")
    public void testManyThreads() throws InterruptedException {
        threadPool = new ThreadPool(40);
        for (int i = 0; i < array.length; i++) {
            threadPool.addJob(r);
        }
        Thread.sleep(600);
        for (int i = 0; i < array.length; i++) {
            assertTrue("A thread didn't increment the correct array entry", array[i] == i);
        }
    }

    @Test(timeout = kTimeoutSlow)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Makes sure your thread pool still works even with thread sleeps ")
    public void testSleepingThreads() throws InterruptedException {
        threadPool = new ThreadPool(2);
        threadPool.addJob(s);
        threadPool.addJob(s);
        threadPool.addJob(s);
        Thread.sleep(500);
        for (int i = 0; i < (3 - 1); i++) {
            assertTrue("A thread didn't increment the correct array entry", array[i] == i);
        }
    }

    @Test(timeout = kTimeoutSlow)
    @Category(AG_PROJ3_CODE.class)
    @AGTestDetails(points = 1,
        desc = "Spawns many threads and checks that thread pool handles " +
               "requests correctly even with thread sleeps")
    public void testManySleepingThreads() throws InterruptedException {
        threadPool = new ThreadPool(40);
        for (int i = 0; i < array.length; i++) {
            threadPool.addJob(t);
        }
        Thread.sleep(500);
        for (int i = 0; i < array.length; i++) {
            assertTrue("A thread didn't increment the correct array entry", array[i] == i);
        }
    }
}
