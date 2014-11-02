package nachos.threads;

import nachos.machine.*;

import java.util.Queue;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        inTransaction = false;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        commLock.acquire();

        while(inTransaction || listenerQueue.getThreadCount() == 0) {
            //System.out.println("speaker " + KThread.currentThread() + " sleep, " + speakerQueue.getThreadCount() + " speakers " + listenerQueue.getThreadCount() + " listeners");
            speakerQueue.sleep();
            //System.out.println("speaker " + KThread.currentThread() + " wake, " + speakerQueue.getThreadCount() + " speakers " + listenerQueue.getThreadCount() + " listeners");
        }
        inTransaction = true;
        msg = word;
        listenerQueue.wake();

        commLock.release();
        //System.out.println("speaker " + KThread.currentThread() + " out, " + speakerQueue.getThreadCount() + " speakers " + listenerQueue.getThreadCount() + " listeners");
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
    public int listen() {
        int ret;
        commLock.acquire();

        while(!inTransaction) {
            if(speakerQueue.getThreadCount() > 0)
                speakerQueue.wake();
            //System.out.println("listener " + KThread.currentThread() + " sleep, " + speakerQueue.getThreadCount() + " speakers " + listenerQueue.getThreadCount() + " listeners");
            listenerQueue.sleep();
            //System.out.println("listener " + KThread.currentThread() + " wake, " + speakerQueue.getThreadCount() + " speakers " + listenerQueue.getThreadCount() + " listeners");
        }
        ret = msg;
        inTransaction = false;
        if(listenerQueue.getThreadCount() > 0 && speakerQueue.getThreadCount() > 0)
            speakerQueue.wake();

        commLock.release();
        //System.out.println("listener " + KThread.currentThread() + " out, " + speakerQueue.getThreadCount() + " speakers " + listenerQueue.getThreadCount() + " listeners");
        return ret;
    }

    private Lock commLock = new Lock();
    private Condition2 speakerQueue = new Condition2(commLock);
    private Condition2 listenerQueue = new Condition2(commLock);
    private int msg;
    private boolean inTransaction;
}
