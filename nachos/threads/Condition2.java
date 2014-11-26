package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
        this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        conditionLock.release();
        Machine.interrupt().disable();
        count++;
        waitQueue.waitForAccess(KThread.currentThread());
        KThread.sleep();
        conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean intStatus = Machine.interrupt().disable();
        KThread t = waitQueue.nextThread();
        if(t != null) {
            count--;
            t.ready();
        }
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean intStatus = Machine.interrupt().disable();
        KThread t;
        while((t = waitQueue.nextThread()) != null)
            t.ready();
        count = 0;
        Machine.interrupt().restore(intStatus);
    }
    
    public void acquireDonation(KThread t) {
        if(ThreadedKernel.scheduler instanceof PriorityScheduler)
            waitQueue.acquire(t);
    }

    public int getThreadCount() {
        return count;
    }

    private Lock conditionLock;
    private ThreadQueue waitQueue =
        ThreadedKernel.scheduler.newThreadQueue(true);
    private int count;
}
