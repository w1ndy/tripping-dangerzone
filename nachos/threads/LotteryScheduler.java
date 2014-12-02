package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }

    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new LotteryQueue(transferPriority);
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());
        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();
        setPriority(thread, getPriority(thread) + 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();
        setPriority(thread, getPriority(thread) - 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new LotteryThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    public class LotteryQueue extends PriorityQueue {
        public LotteryQueue(boolean transferPriority) {
            super(transferPriority);
            donationController = new LotteryDonationController(queue);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            if(queue.isEmpty())
                return null;

            ThreadState ts = pickNextThread();
            queue.remove(threadStates.get(ts));
            threadStates.remove(ts);
            if(transferPriority)
                donationController.resetMaximumPriority(ts);
            Lib.debug('P', "NextThread: " + ts.thread + ", priority = " + ts.getPriority() + ", effective priority = " + ts.getEffectivePriority() + ", size = " + queue.size());
            acquire(ts.thread);
            return ts.thread;
        }

        protected ThreadState pickNextThread() {
            int sum = ((LotteryDonationController)donationController).sum;
            if(sum == 0)
                return queue.peek().state;
            int r = (new Random()).nextInt(sum);
            for(ThreadState ts : threadStates.keySet()) {
                r -= ts.getEffectivePriority();
                if(r < 0)
                    return ts;
            }
            System.out.println(sum + " : " + r);
            Lib.assertNotReached("Failed to pick next thread");
            return null;
        }
    }

    protected class LotteryDonationController extends DonationController {
        public LotteryDonationController(java.util.PriorityQueue<PriorityQueue.ThreadWrapper> queue) {
            super(queue);
        }

        public void setTarget(ThreadState t) {
            if(target != null)
                target.retractDonatedPriority(this);
            target = t;
            target.donatePriority(this, sum);
        }

        public void resetMaximumPriority(ThreadState t) {
            sum -= t.getEffectivePriority();
            target.donatePriority(this, sum);
        }

        public void transferPriority(ThreadState t) {
            sum += t.getEffectivePriority();
            target.donatePriority(this, t.getEffectivePriority());
        }

        public int sum = 0;
    }

    protected class LotteryThreadState extends ThreadState {
        public LotteryThreadState(KThread thread) {
            super(thread);
            effectivePriority = new LotteryTicketDesc(0);
        }

        protected class LotteryTicketDesc extends EffectivePriorityDesc {
            public LotteryTicketDesc(int priority) {
                super(priority);
            }

            public int getEffectivePriority() {
                return priority + max_donation;
            }

            public void donate(DonationController q, int priority) {
                if(this.donations.containsKey(q))
                    max_donation -= this.donations.get(q);
                this.donations.put(q, priority);
                max_donation += priority;
            }

            public void retract(DonationController q) {
                if(donations.containsKey(q)) {
                    int p = donations.get(q);
                    donations.remove(q);
                    max_donation -= p;
                }
            }
        }
    }
}
