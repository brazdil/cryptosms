package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.IOException;

import uk.ac.cam.db538.securesms.CustomAsserts;
import uk.ac.cam.db538.securesms.encryption.Encryption;

import junit.framework.TestCase;

public class SessionKeys_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		Common.clearFile();	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

/*	public void testSessionKeys() {
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
		Database_Old database = Database_Old.getSingleton();
		
		boolean keysSent = true;
		boolean keysConfirmed = true;
		String phoneNumberConversation = "+123456789012";
		String phoneNumberSIM = "+123456789034";
		byte[] sessionKey_Out = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_Out = 0x12;
		byte[] sessionKey_In = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_In = 0x18;
		
		Conversation_Old conv = database.createConversation(phoneNumberConversation);
		
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
	
	public void testCreateSessionKeys() {
		Database history;
		try {
			history = Database.getSingleton();
			Conversation conv = history.createConversation("phone number adfsdf");
			
			// check that it takes only one entry
			int countEmpty = history.getEmptyEntriesCount();
			for (int i = 0; i < Database.ALIGN_SIZE / Database.CHUNK_SIZE * 5; ++i)
			{
				history.createSessionKeys(conv, "phone number sfdgfsdg");
				if (countEmpty == 0)
					assertEquals(Database.ALIGN_SIZE / Database.CHUNK_SIZE - 1, (countEmpty = history.getEmptyEntriesCount()));
				else
					assertEquals(countEmpty - 1, (countEmpty = history.getEmptyEntriesCount()));
			}

			// check structure
			assertTrue(history.checkStructure());
		} catch (DatabaseFileException e) {
			assertTrue(e.getMessage(), false);
		} catch (IOException e) {
			assertTrue(e.getMessage(), false);
		}
	}*/
}
