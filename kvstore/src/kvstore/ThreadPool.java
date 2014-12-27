package kvstore;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadPool {

    /* Array of threads in the threadpool */
    public Thread threads[];
    public ConcurrentLinkedQueue<Runnable> jobs;
    public ConcurrentLinkedQueue<WorkerThread> idle;


    /**
     * Constructs a Threadpool with a certain number of threads.
     *
     * @param size number of threads in the thread pool
     */
    public ThreadPool(int size) {
        threads = new Thread[size];
        jobs = new ConcurrentLinkedQueue<Runnable>();
        idle = new ConcurrentLinkedQueue<WorkerThread>();
        for(int i = 0; i < size; i++) {
            threads[i] = new WorkerThread(this);
            threads[i].start();
        }
    }

    /**
     * Add a job to the queue of jobs that have to be executed. As soon as a
     * thread is available, the thread will retrieve a job from this queue if
     * if one exists and start processing it.
     *
     * @param r job that has to be executed
     * @throws InterruptedException if thread is interrupted while in blocked
     *         state. Your implementation may or may not actually throw this.
     */
    public void addJob(Runnable r) throws InterruptedException {
        jobs.add(r);
        if(!idle.isEmpty())
            idle.poll().interrupt();
    }

    /**
     * Block until a job is present in the queue and retrieve the job
     * @return A runnable task that has to be executed
     * @throws InterruptedException if thread is interrupted while in blocked
     *         state. Your implementation may or may not actually throw this.
     */
    public Runnable getJob(WorkerThread t) throws InterruptedException {
        Runnable ret;
        ret = null;
        while((ret = jobs.poll()) == null)
            try {
                idle.add(t);
                t.sleep(60000);
            } catch(InterruptedException e) {
            } finally {
                idle.remove(t);
            }
        return ret;
    }

    /**
     * A thread in the thread pool.
     */
    public class WorkerThread extends Thread {

        public ThreadPool threadPool;

        /**
         * Constructs a thread for this particular ThreadPool.
         *
         * @param pool the ThreadPool containing this thread
         */
        public WorkerThread(ThreadPool pool) {
            threadPool = pool;
        }

        /**
         * Scan for and execute tasks.
         */
        @Override
        public void run() {
            while(true)
                try {
                    getJob(this).run();
                } catch(Exception e) {}
        }
    }
}
