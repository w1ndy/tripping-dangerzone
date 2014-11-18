package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                       priority <= priorityMaximum);

        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority+1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority-1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
            this.maximumPriority = priorityDefault;
            donateTarget = null;
        }

        public void waitForAccess(KThread thread) {
            //System.out.println("! Thread " + thread.toString() + " waiting for access.");
            Lib.assertTrue(Machine.interrupt().disabled());
            Lib.assertTrue(!threadStates.containsKey(getThreadState(thread)));

            threadStates.put(getThreadState(thread), new ThreadWrapper(getThreadState(thread)));
            queue.offer(threadStates.get(getThreadState(thread)));

            if(transferPriority) {
                Lib.assertTrue(donateTarget != null);

                maximumPriority = (getThreadState(thread).getEffectivePriority() > maximumPriority) ? getThreadState(thread).getEffectivePriority() : maximumPriority;
                if(maximumPriority > donateTarget.getEffectivePriority()) {
                    donateTarget.setEffectivePriority(maximumPriority);
                    if(threadStates.containsKey(donateTarget)) {
                        ThreadWrapper w = threadStates.get(donateTarget);
                        queue.remove(w);
                        queue.offer(w);
                    }
                }
            }

            getThreadState(thread).waitForAccess(this);
            //System.out.println("! Inserted. queue size = " + queue.size());
        }

        public void acquire(KThread thread) {
            //System.out.println("! Thread " + thread + " acquiring...");
            Lib.assertTrue(Machine.interrupt().disabled());
            Lib.assertTrue(!threadStates.containsKey(getThreadState(thread)));

            if(transferPriority) {
                if(maximumPriority > getThreadState(thread).getEffectivePriority()) 
                    getThreadState(thread).setEffectivePriority(maximumPriority);
                donateTarget = getThreadState(thread);
            }

            getThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            //System.out.println("! Pumping next thread");
            Lib.assertTrue(Machine.interrupt().disabled());
            if(queue.isEmpty())
                return null;
            ThreadWrapper w = queue.poll();
            if(transferPriority && w.state.getPriority() == maximumPriority)
                resetMaximumPriority();
            threadStates.remove(w.state);
            return w.state.thread;
        }

        public void resetMaximumPriority() {
            maximumPriority = priorityDefault;
            for(Map.Entry<ThreadState, ThreadWrapper> e : threadStates.entrySet())
                maximumPriority = (maximumPriority > e.getKey().getEffectivePriority()) ? maximumPriority : e.getKey().getEffectivePriority();
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return	the next thread that <tt>nextThread()</tt> would
         *		return.
         */
        protected ThreadState pickNextThread() {
            return queue.peek().state;
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }

        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;
        public int maximumPriority;
        public java.util.PriorityQueue<ThreadWrapper> queue = new java.util.PriorityQueue<ThreadWrapper>();
        public HashMap<ThreadState, ThreadWrapper> threadStates = new HashMap<ThreadState, ThreadWrapper>();
        public ThreadState donateTarget;

        protected class ThreadWrapper implements Comparable {

            public ThreadWrapper(ThreadState s) {
                state = s;
                timeInserted = Machine.timer().getTime();
                //System.out.println("timeInserted = " + timeInserted);
            }

            public int compareTo(Object o) {
                Lib.assertTrue(o instanceof ThreadWrapper);
                ThreadWrapper s = (ThreadWrapper) o;
                return (state.getEffectivePriority() == s.state.getEffectivePriority()) ? (int)(timeInserted - s.timeInserted) : (s.state.getEffectivePriority() - state.getEffectivePriority());
            }

            public ThreadState state;
            public long timeInserted;
        }
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param	thread	the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;
            setPriority(priorityDefault);
            setEffectivePriority(priorityDefault);
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return	the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return	the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
            return eff_priority;
        }

        public void setEffectivePriority(int priority) {
            this.eff_priority = priority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param	priority	the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;

            this.priority = priority;
            eff_priority = (eff_priority > priority) ? eff_priority : priority;
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param	waitQueue	the queue that the associated thread is
         *				now waiting on.
         *
         * @see	nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(PriorityQueue waitQueue) {
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see	nachos.threads.ThreadQueue#acquire
         * @see	nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityQueue waitQueue) {
        }

        /** The thread with which this object is associated. */
        protected KThread thread;
        /** The priority of the associated thread. */
        protected int priority, eff_priority;
    }
}
