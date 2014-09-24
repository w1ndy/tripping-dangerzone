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
        if(speakerCount >= 0) {
            condLock.acquire();
            speakerCount++;
            cond.sleep();
            speakerCount--;
            msg = word;
            msgStored = true;
            condLock.release();
        } else {
            condLock.acquire();
            msg = word;
            msgStored = true;
            cond.wake();
            condLock.release();
        }
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
    public int listen() {
        if(speakerCount <= 0) {
            condLock.acquire();
            speakerCount--;
            cond.sleep();
            speakerCount++;
            msgStored = false;
            condLock.release();
            return msg;
        } else {
            condLock.acquire();
            msgStored = false;
            cond.wake();
            condLock.release();
            while(!msgStored) {
                KThread.yield();
            }
            msgStored =false;
            return msg;
        }
    }

    private Lock condLock = new Lock();
    private Condition2 cond = new Condition2(condLock);
    private int speakerCount, msg;
    private boolean msgStored;
}
