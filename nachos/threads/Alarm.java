package nachos.threads;

import nachos.machine.*;

import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() {
                timerInterrupt();
            }
        });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        Machine.interrupt().disable();
        while(waitQueue.peek() != null && ((BlockedThread)waitQueue.peek()).x < Machine.timer().getTime()) {
            waitQueue.poll().t.ready();
        }
        KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        // for now, cheat just to get something working (busy waiting is bad)
        // long wakeTime = Machine.timer().getTime() + x;
        // while (wakeTime > Machine.timer().getTime())
        //     KThread.yield();
        Machine.interrupt().disable();
        BlockedThread t = new BlockedThread();
        t.t = KThread.currentThread();
        t.x = x + Machine.timer().getTime();
        waitQueue.offer(t);
        KThread.sleep();
    }

    public class BlockedThread implements Comparable {
        public KThread t;
        public long x;

        public int compareTo(Object t) {
            if(x == ((BlockedThread)t).x) return 0;
            else if(x < ((BlockedThread)t).x) return 1;
            else return -1;
        }
    }

    PriorityQueue<BlockedThread> waitQueue = new PriorityQueue<BlockedThread>();
}
