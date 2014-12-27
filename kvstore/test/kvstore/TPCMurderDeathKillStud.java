package kvstore;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import autograder.AGCategories.AGTestDetails;
import autograder.AGCategories.AG_PROJ4_CODE;

/* NOTES:
 * 
 * This class tests the behavior of your distributed Key Value store when slave servers asynchronously fail.
 * This is the student distribution. The tests provided are a SMALL SUBSET of the ones that will be used to grade your project. The setup is mostly the same.
 * We provide this file to you as a sanity check and to encourage you to test your own code. You may build off of this template if you'd like.
 * If your code fails this test, it may hang because it fails to rebuild and reregister. It should timeout and fail but it could just never finish.
 * 
 * The testing methods make extensive use of Mockito and the doAnswer methods. It is essentially equivalent to calling when() for void return types.
 * A lot of hacks went into making the tests behave the way I wanted them to. The comments should give you an idea of what's going on.
 * Slave deaths are simulated by starting a new thread that waits on a timer to revive the current slave before the slave has actually died. 
 * The slave essentially sets up its own respawn point before actually killing itself. This obviously doesn't happen in real systems.
 * 
 * The method startMockSlave is used to construct slaves for this tests. It's a bit misleading in name as this test is effectively an END TO END test.
 * All servers, handlers, etc. are REAL classes implemented by you. The only mocking that happens on the slaves occurs in the methods dieAfterLog (and dieBeforeLog).
 * When I construct the TPCLog in startMockSlave, I SPY on it, meaning it's sort of partially mocked. All methods that don't have a stub defined for it will default to its REAL class defined method rather than returning null/doing nothing.
 * Depending on the arguments appendAndFlush are called with, it may exhibit a mocked behavior (i.e. self destructing the slave)
 * 
 * As per usual, these tests don't come with any warranty and are presented as is. Meaning if you decide to base your own test cases off this setup and modify the file, there is no guarantee things will work as intended. Study the existing framework to increase your chances of success.
 * 
 * This test file is part of the KVStore Project
 * (c) CS162 University of California, Berkeley
 * Written by Isaac Tian, May 2014
 */

public class TPCMurderDeathKillStud {

    String hostname;
    KVClient client;
    TPCMaster master;
    ServerRunner masterClientRunner;
    ServerRunner masterSlaveRunner;
    HashMap<String, ServerRunner> slaveRunners;

    static final int CLIENTPORT = 8888;
    static final int SLAVEPORT = 9090;

    static final int NUMSLAVES = 4;

    static final long SLAVE1 = 4611686018427387903L;  // Long.MAX_VALUE/2
    static final long SLAVE2 = 9223372036854775807L;  // Long.MAX_VALUE
    static final long SLAVE3 = -4611686018427387903L; // Long.MIN_VALUE/2
    static final long SLAVE4 = -0000000000000000001;  // Long.MIN_VALUE

    static final String KEY1 = "6666666666666666666"; // 2846774474343087985
    static final String KEY2 = "9999999999999999999"; // 8204764838124603412
    static final String KEY3 = "0000000000000000000"; //-7869206253219942869
    static final String KEY4 = "3333333333333333333"; //-2511215889438427442


    KVMessage p1Death = new KVMessage(KVConstants.PUT_REQ);
    KVMessage delDeath = new KVMessage(KVConstants.DEL_REQ);
    KVMessage verify = new KVMessage(KVConstants.PUT_REQ);
    KVServer slave1 = null;
    File tempFile = null;

    File temp2 = null;
    File temp3 = null;
    File temp4 = null;

    TPCLog spyLog = null;

    String LOG = null;

    @Before
    public void setUp() throws Exception {

        hostname = InetAddress.getLocalHost().getHostAddress();
        p1Death.setKey(KEY1);
        p1Death.setValue("GOBEARS");
        verify.setKey("6666666666666666667");
        verify.setValue("demolition man");
        delDeath.setKey(KEY1);

        //required, otherwise files don't clean themselves up and subsequent runs rebuild with old logs and have undefined behavior.
        tempFile = File.createTempFile("TempLogMDK", ".txt");
        tempFile.deleteOnExit();
        temp2 = File.createTempFile("slaveLog2", ".txt");
        temp2.deleteOnExit();
        temp3 = File.createTempFile("slaveLog3", ".txt");
        temp3.deleteOnExit();
        temp4 = File.createTempFile("slaveLog4", ".txt");
        temp4.deleteOnExit();

        startMaster();

        slaveRunners = new HashMap<String, ServerRunner>();

        necromancy(SLAVE2, temp2.getPath());
        necromancy(SLAVE3, temp3.getPath());
        necromancy(SLAVE4, temp4.getPath());

        client = new KVClient(hostname, CLIENTPORT);

    }

    @After
    public void tearDown() throws InterruptedException {
        masterClientRunner.stop();
        masterSlaveRunner.stop();

        for (ServerRunner slaveRunner : slaveRunners.values()) {
            slaveRunner.stop();
        }

        client = null;
        master = null;
        slaveRunners = null;
    }

    protected void startMaster() throws Exception {
        master = new TPCMaster(NUMSLAVES, new KVCache(1,4));
        SocketServer clientSocketServer = new SocketServer(hostname, CLIENTPORT);
        clientSocketServer.addHandler(new TPCClientHandler(master));
        masterClientRunner = new ServerRunner(clientSocketServer, "masterClient");
        masterClientRunner.start();
        SocketServer slaveSocketServer = new SocketServer(hostname, SLAVEPORT);
        slaveSocketServer.addHandler(new TPCRegistrationHandler(master));
        masterSlaveRunner = new ServerRunner(slaveSocketServer, "masterSlave");
        masterSlaveRunner.start();
        Thread.sleep(100);
    }

    protected void stopSlave(String name) throws InterruptedException {
        ServerRunner sr = slaveRunners.get(name);
        if (sr == null) {
            throw new RuntimeException("Slave does not exist!");
        } else {
            sr.stop();
        }
    }


    public void setupLog(TPCLog log){
        KVMessage putGeorge = new KVMessage(KVConstants.PUT_REQ);
        putGeorge.setKey("george");
        putGeorge.setValue("yiu");
        KVMessage putRiyaz = new KVMessage(KVConstants.PUT_REQ);
        putRiyaz.setKey("riyaz");
        putRiyaz.setValue("faizullabhoy");
        KVMessage putIsaac = new KVMessage(KVConstants.PUT_REQ);
        putIsaac.setKey("isaac");
        putIsaac.setValue("tian");
        KVMessage putVaishaal = new KVMessage(KVConstants.PUT_REQ);
        putVaishaal.setKey("vaishaal");
        putVaishaal.setValue("shankar");
        KVMessage putKelvin = new KVMessage(KVConstants.PUT_REQ);
        putKelvin.setKey("kelvin");
        putKelvin.setValue("chou");
        KVMessage putNick = new KVMessage(KVConstants.PUT_REQ);
        putNick.setKey("nick");
        putNick.setValue("chang");

        KVMessage com = new KVMessage(KVConstants.COMMIT);

        log.appendAndFlush(putGeorge);
        log.appendAndFlush(com);
        log.appendAndFlush(putRiyaz);
        log.appendAndFlush(com);
        log.appendAndFlush(putIsaac);
        log.appendAndFlush(com);
        log.appendAndFlush(putVaishaal);
        log.appendAndFlush(com);
        log.appendAndFlush(putKelvin);
        log.appendAndFlush(com);
        log.appendAndFlush(putNick);
        log.appendAndFlush(com);

        //System.out.println("Finished flushing.");

    }

    public void checkBuild(){
        try{
            assertTrue(slave1.get("george").equals("yiu"));
        }
        catch (KVException e) {fail("Key 'george' not found on rebuild.");}
        try{
            assertTrue(slave1.get("riyaz").equals("faizullabhoy"));
        }
        catch (KVException e) {fail("Key 'riyaz' not found on rebuild.");}
        try{
            assertTrue(slave1.get("isaac").equals("tian"));
        }
        catch (KVException e) {fail("Key 'isaac' not found on rebuild.");}
        try{
            assertTrue(slave1.get("vaishaal").equals("shankar"));
        }
        catch (KVException e) {fail("Key 'vaishaal' not found on rebuild.");}
        try{
            assertTrue(slave1.get("nick").equals("chang"));
        }
        catch (KVException e) {fail("Key 'nick' not found on rebuild.");}
        try{
            assertTrue(slave1.get("kelvin").equals("chou"));
        }
        catch (KVException e) {fail("Key 'kelvin' not found on rebuild.");}

    }


    protected void startMockSlave(long slaveID, int deathOption) throws Exception {
        String name = new Long(slaveID).toString();
        ServerRunner sr = slaveRunners.get(name);
        if (sr != null) {
            sr.stop();
            slaveRunners.remove(sr);
        }

        SocketServer ss = new SocketServer(InetAddress.getLocalHost().getHostAddress(), 0);
        KVServer slaveKvs = new KVServer(100, 10);
        String logPath = tempFile.getPath();	//"bin/log." + slaveID + "@" + ss.getHostname();
        TPCLog log = new TPCLog(logPath,slaveKvs);
        setupLog(log);
        log = spy(new TPCLog(logPath, slaveKvs)); //spied for testing
        LOG = logPath;
        spyLog = log;


        TPCMasterHandler handler = new TPCMasterHandler(slaveID, slaveKvs, log);
        ss.addHandler(handler);
        ServerRunner slaveRunner = new ServerRunner(ss, name);

        switch (deathOption){
       
        case 1:
            doAnswer(dieAfterLog).when(log).appendAndFlush(argThat(new isPutDel1())); //kill in phase 1 after log
            break;

        default:
            System.out.println("WARNING: INVALID ARGUMENTS");
            break;
        }

        slaveRunner.start();
        slaveRunners.put(name, slaveRunner);

        handler.registerWithMaster(InetAddress.getLocalHost().getHostAddress(), ss);
    }

    //Used to allow the spied log to recognize if the message is a phase 1 TPC.
    class isPutDel1 extends ArgumentMatcher<KVMessage>{

        @Override
        public boolean matches(Object msg) {
            KVMessage m = (KVMessage) msg;
            if (m.getMsgType().equals(KVConstants.PUT_REQ) || m.getMsgType().equals(KVConstants.DEL_REQ)) return true;
            return false;
        }
    }

    //Used to allow the spied log to recognize if the message is a phase 2 commit. Don't care about aborts.
    class isPutDel2 extends ArgumentMatcher<KVMessage>{

        @Override
        public boolean matches(Object msg) {
            KVMessage m = (KVMessage) msg;
            if (m.getMsgType().equals(KVConstants.COMMIT)) return true;
            return false;
        }
    }

    //Force calling slave to block until after timeout, then terminate by killing the running thread without flushing request to log.

    @SuppressWarnings({"deprecation", "rawtypes"})
    Answer dieBeforeLog = new Answer(){
        @Override
        public Object answer(InvocationOnMock inv){
            //slave 1 is hardwired to die for convenience.

            Thread rebuild = new Thread(necromancer);
            try{Thread.sleep(2*TPCMaster.TIMEOUT);}catch (InterruptedException e){}
            rebuild.start();
            Thread.currentThread().stop(); //naughty. But it works
            System.out.println("don't get here");
            return null; //useless
        }
    };

    //Force calling slave to block until after timeout, then terminate by killing the running thread after flushing request to log.

    @SuppressWarnings({"deprecation", "rawtypes"})
    Answer dieAfterLog = new Answer() {
        @Override
        public Object answer(InvocationOnMock inv){
            try{inv.callRealMethod();}catch (Throwable e) { }//shouldn't happen
            Thread rebuild = new Thread(necromancer);
            try{Thread.sleep(2*TPCMaster.TIMEOUT);}catch (InterruptedException e){}
            rebuild.start();
            Thread.currentThread().stop(); //naughty. But it works
            return null;
        }

    };

    //Rebuild and reregister the slave with ID SLAVEID using log with path oldLog.
    protected void necromancy(long slaveID, String oldLog) throws Exception {

        try{Thread.sleep(TPCMaster.TIMEOUT);} catch (InterruptedException e){System.out.println("Did not sleep");}
        String name = new Long(slaveID).toString();
        ServerRunner sr = slaveRunners.get(name);
        if (sr != null) {
            sr.stop();
            //sr.getServer().stop(); //cleans up the server but doesn't guarantee a stop
            slaveRunners.remove(sr);
        }

        SocketServer ss = new SocketServer(InetAddress.getLocalHost().getHostAddress(), 0);
        KVServer slaveKvs = new KVServer(100, 10);

        TPCLog log = spy(new TPCLog(oldLog, slaveKvs));
        spyLog = log;
        TPCMasterHandler handler = new TPCMasterHandler(slaveID, slaveKvs, log);
        slave1 = slaveKvs;
        ss.addHandler(handler);
        ServerRunner slaveRunner = new ServerRunner(ss, name);
        slaveRunner.start();
        slaveRunners.put(name, slaveRunner);

        handler.registerWithMaster(InetAddress.getLocalHost().getHostAddress(), ss);
    }

    Runnable necromancer = new Runnable() {
        @Override
        public void run(){
            try{necromancy(SLAVE1, LOG);}catch (Exception e) {fail("COULD NOT REBUILD");}
        }

    };

    /* BEGIN TEST CASE*/
    
    @Test(timeout = 30000)
    @Category(AG_PROJ4_CODE.class)
    @AGTestDetails(points = 2, desc = "Kills the slave during phase 1 after flushing PUT request to log and rebuilds. Checks that the PUT request was aborted.")
    public void testP1DeathAfterLog(){
        try{startMockSlave(SLAVE1, 1);} catch (Exception e) {fail("can't start slave");}
        try{
            master.handleTPCRequest(p1Death, true);
            fail("Shouldn't succeed");
        } catch (KVException e){

        }
        checkBuild();
        try{
            slave1.get(KEY1);
            fail("Key was put when it should have failed.");
        }
        catch(KVException e){
            assertTrue(e.getKVMessage().getMessage().equals(KVConstants.ERROR_NO_SUCH_KEY));
        }

        //Verify log integrity by putting a key successfully, then killing and rebuilding slave.
        try{
            master.handleTPCRequest(verify,true);
            assertTrue(slave1.get("6666666666666666667").equals("demolition man"));
            verify(spyLog, atLeast(2)).appendAndFlush((KVMessage) anyObject());
        } catch (KVException e){
            fail("Put on live slave shouldn't fail");
        }

        try {necromancy(SLAVE1, LOG);} catch (Exception e) {fail("Could not rebuild slave.");}
        checkBuild();
        try{
            assertTrue(slave1.get("6666666666666666667").equals("demolition man"));
        } catch (KVException e){
            fail("Server not properly rebuilt.");
        }
        try{
            System.out.println(slave1.get(KEY1));
            fail("Key was put when it should have failed.");
        }
        catch(KVException e){
            assertTrue(e.getKVMessage().getMessage().equals(KVConstants.ERROR_NO_SUCH_KEY));
        }

    }

  
}
