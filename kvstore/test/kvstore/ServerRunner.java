package kvstore;

public class ServerRunner implements Runnable {

    public static final int THREAD_STOP_TIMEOUT = 1000 * 10;

    public ServerRunner(SocketServer socs, String name) {
        sockserver = socs;
        runnerName = name;
    }

    private final SocketServer sockserver;
    public final String runnerName;

    private Thread serverThread = null;
    boolean isRunning = false;

    @Override
    public void run() {
        try {
            sockserver.connect();
            synchronized (this) {
                isRunning = true;
                notifyAll();
            }
            sockserver.start();
            synchronized (this) {
                isRunning = false;
                notifyAll();
            }
        } catch (Exception e) {
            System.out.println(String.format("SERVER-SIDE: Error from %s", runnerName));
            e.printStackTrace();
            System.out.println("Encountered fatal error during autograder run.");
            System.exit(-1);
        }
    }

    public void start() throws InterruptedException {
        if (isRunning) {
            return;
        }
        if (serverThread == null) {
            serverThread = new Thread(this, runnerName);
            serverThread.setDaemon(true);
            serverThread.start();

            while (!isRunning) {
                try {
                    synchronized (this) {
                        this.wait(100);
                    }
                } catch (InterruptedException e) {
                }
            }
            Thread.sleep(100);
        }
    }

    public void stop() throws InterruptedException {
        if (sockserver != null) {
            sockserver.stop();
        }
        if (serverThread != null) {
            try {
                serverThread.join(THREAD_STOP_TIMEOUT);
            } catch (InterruptedException e) {
                System.out.format("[!!!!!] ERROR ServerRunner: "
                        + "Failed to stop Server (%s), giving up.%n",
                        runnerName);
            }
        }
        isRunning = false;
        serverThread = null;
        Thread.sleep(100);
    }
    public SocketServer getServer(){
    	return sockserver;
    }
}
