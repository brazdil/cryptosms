package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.IOException;

import uk.ac.cam.db538.securesms.CustomAsserts;
import uk.ac.cam.db538.securesms.encryption.Encryption;

import junit.framework.TestCase;

public class SessionKeys_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();

		// delete the file before each test
		File file = new File(Database_Test.TESTING_FILE);
		if (file.exists())
			file.delete();
		
		// and free the singleton
		Database.freeSingleton();
		Database.initSingleton(Database_Test.TESTING_FILE);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSessionKeys() {
		SessionKeys keys;
		
		// ASSIGNMENT
		
		try {
			keys = new SessionKeys(120L);
			assertEquals(keys.getIndexEntry(), 120L);
		} catch (IndexOutOfBoundsException ex) {
			assertTrue(false);
		}
		
		// INDEX OUT OF BOUNDS
		
		try {
			keys = new SessionKeys(0x100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			keys = new SessionKeys(0L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			keys = new SessionKeys(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testSaveUpdate() throws DatabaseFileException, IOException {
		Database database = Database.getSingleton();
		
		boolean keysSent = true;
		boolean keysConfirmed = true;
		String phoneNumberConversation = "+123456789012";
		String phoneNumberSIM = "+123456789034";
		byte[] sessionKey_Out = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_Out = 0x12;
		byte[] sessionKey_In = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_In = 0x18;
		
		Conversation conv = database.createConversation(phoneNumberConversation);
		
		SessionKeys keysWrite = conv.newSessionKeys("");
		SessionKeys keysRead = new SessionKeys(keysWrite.getIndexEntry());
		
		keysWrite.setKeysSent(keysSent);
		keysWrite.setKeysConfirmed(keysConfirmed);
		keysWrite.setPhoneNumber(phoneNumberSIM);
		keysWrite.setSessionKey_Out(sessionKey_Out);
		keysWrite.setLastID_Out(lastID_Out);
		keysWrite.setSessionKey_In(sessionKey_In);
		keysWrite.setLastID_In(lastID_In);
		keysWrite.save();
		
		keysRead.update();
		assertEquals(keysSent, keysRead.getKeysSent());
		assertEquals(keysConfirmed, keysRead.getKeysConfirmed());
		assertEquals(phoneNumberSIM, keysRead.getPhoneNumber());
		CustomAsserts.assertArrayEquals(keysRead.getSessionKey_Out(), sessionKey_Out);
		assertEquals(lastID_Out, keysRead.getLastID_Out());
		CustomAsserts.assertArrayEquals(keysRead.getSessionKey_In(), sessionKey_In);
		assertEquals(lastID_In, keysRead.getLastID_In());
	}
}
