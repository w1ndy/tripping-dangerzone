package kvstore;

import static org.junit.Assert.*;
import static kvstore.KVConstants.*;

import java.net.Socket;

import org.junit.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.api.mockito.PowerMockito;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Socket.class, KVMessage.class, TPCMasterHandler.class, TPCSlaveInfo.class, TPCMaster.class})
public class TPCMasterTest {

	TPCMaster master;
	KVCache masterCache;
    static final long SLAVE1 = 4611686018427387903L;  // Long.MAX_VALUE/2
    static final long SLAVE2 = 9223372036854775807L;  // Long.MAX_VALUE
    static final long SLAVE3 = -4611686018427387903L; // Long.MIN_VALUE/2
    static final long SLAVE4 = -0000000000000000001;  // Long.MIN_VALUE
    static final long SLAVE5 = 6230492013836775123L;  // Arbitrary long value

    TPCSlaveInfo slave1;
    TPCSlaveInfo slave2;
    TPCSlaveInfo slave3;
    TPCSlaveInfo slave4;
    TPCSlaveInfo slave5;

	@Before
	public void setupMaster() throws KVException {
		masterCache = new KVCache(5, 5);
		master = new TPCMaster(4, masterCache);

		slave1 = new TPCSlaveInfo(SLAVE1 + "@111.111.111.111:1");
		slave2 = new TPCSlaveInfo(SLAVE2 + "@111.111.111.111:2");
		slave3 = new TPCSlaveInfo(SLAVE3 + "@111.111.111.111:3");
		slave4 = new TPCSlaveInfo(SLAVE4 + "@111.111.111.111:4");
		slave5 = new TPCSlaveInfo(SLAVE5 + "@111.111.111.111:5");
	}

	@Test
	public void testMaxSlaves() throws KVException {
		master.registerSlave(slave1);
		master.registerSlave(slave2);
		master.registerSlave(slave3);
		master.registerSlave(slave4);
		assertTrue(master.getNumRegisteredSlaves() == 4);
	}

	@Test
	public void testMoreThanMaxSlaves() throws KVException {
		master.registerSlave(slave1);
		master.registerSlave(slave2);
		master.registerSlave(slave3);
		master.registerSlave(slave4);
		master.registerSlave(slave5);
		assertTrue(master.getNumRegisteredSlaves() == 4);
		assertEquals(master.getSlave(SLAVE5), null);
	}

	@Test
	public void testReconnectSlave() throws KVException {
		master.registerSlave(slave1);
		master.registerSlave(slave2);
		master.registerSlave(slave3);
		master.registerSlave(slave4);

		assertTrue(master.getNumRegisteredSlaves() == 4);

		slave1 = new TPCSlaveInfo(SLAVE1 + "@111.111.111.111:8080");
		master.registerSlave(slave1);

		assertTrue(master.getNumRegisteredSlaves() == 4);
	}

	@Test
	public void testFindFirstReplica() throws KVException {
		master.registerSlave(slave1);
		master.registerSlave(slave2);
		master.registerSlave(slave3);
		master.registerSlave(slave4);

		String key = "6666666666666666666";
		assertEquals(master.hashTo64bit(key), 2846774474343087985L);
		String key2 = "6666666666666666665";
		assertEquals(master.hashTo64bit(key2), 2846774474343087984L);
		TPCSlaveInfo firstReplica = master.findFirstReplica(key);
		assertEquals(firstReplica, slave1);
		TPCSlaveInfo firstReplica2 = master.findFirstReplica(key2);
		assertEquals(firstReplica2, slave1);
	}

	@Test
	public void testFindSuccessor() throws KVException {
		master.registerSlave(slave1);
		master.registerSlave(slave2);
		master.registerSlave(slave3);
		master.registerSlave(slave4);

		assertEquals(master.findSuccessor(slave1), slave2);
		assertEquals(master.findSuccessor(slave2), slave3);
		assertEquals(master.findSuccessor(slave3), slave4);
		assertEquals(master.findSuccessor(slave4), slave1);
	}

	@Test
	public void testFindInvalidSuccessor() throws KVException {
		master.registerSlave(slave1);
		master.registerSlave(slave2);
		master.registerSlave(slave3);
		master.registerSlave(slave4);

		assertEquals(master.findSuccessor(slave5), slave2);

	}

	@Test
	public void testSimpleHandleGet() {
	    try {
	        //Setting up Master
	        masterCache = new KVCache(5, 5);
	        // masterCache = mock(KVCache.class);
	        master = new TPCMaster(2, masterCache);
	        slave1 = mock(TPCSlaveInfo.class);
	        slave2 = mock(TPCSlaveInfo.class);

	        //Mocking!!
	        Socket sockMock = mock(Socket.class);
	        KVMessage kvmGetMock = mock(KVMessage.class);
	        KVMessage kvmRespMock = mock(KVMessage.class);
	        TPCSlaveInfo slaveInfoMock = mock(TPCSlaveInfo.class);

            PowerMockito.whenNew(Socket.class).withAnyArguments().thenReturn(sockMock);
            PowerMockito.whenNew(TPCSlaveInfo.class).withAnyArguments().thenReturn(slaveInfoMock);
            PowerMockito.whenNew(KVMessage.class).withArguments(GET_REQ).thenReturn(kvmGetMock);
            PowerMockito.whenNew(KVMessage.class).withArguments(RESP).thenReturn(kvmRespMock);
            PowerMockito.whenNew(KVMessage.class).withArguments(any(Socket.class), any(Integer.class)).thenReturn(kvmRespMock);

            when(slave1.connectHost(any(Integer.class))).thenReturn(sockMock);
            when(kvmGetMock.getKey()).thenReturn("I'm kvmRespMock key!");
            doNothing().when(kvmGetMock).setKey(any(String.class));
            doNothing().when(kvmGetMock).sendMessage(any(Socket.class));
            doNothing().when(kvmRespMock).sendMessage(any(Socket.class));
            when(slave1.getSlaveID()).thenReturn(1L);
            when(slave2.getSlaveID()).thenReturn(2L);

            when(kvmRespMock.getMsgType()).thenReturn(RESP);
            when(kvmRespMock.getKey()).thenReturn("I'm kvmRespMock key!");
            when(kvmRespMock.getValue()).thenReturn("I'm kvmRespMock value!");

            master.registerSlave(slave1);
            master.registerSlave(slave2);
	        KVMessage msg = new KVMessage(GET_REQ);
	        msg.setKey("I'm kvmRespMock key!");
	        assertEquals(master.handleGet(msg), "I'm kvmRespMock value!");
	        //Test to see that phase 1 wasn't entered
	        verify(kvmRespMock, times(1)).getValue();
	        verify(kvmRespMock, times(0)).getKey();
	    } catch (Exception e) {
	        e.printStackTrace();
	        fail("This shouldn't fail");
	    }
	}
	

}