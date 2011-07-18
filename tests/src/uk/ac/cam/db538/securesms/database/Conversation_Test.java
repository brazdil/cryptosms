package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.IOException;

import uk.ac.cam.db538.securesms.CustomAsserts;
import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.text.format.Time;

import junit.framework.TestCase;

public class Conversation_Test extends TestCase {

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

	public void testConversation() {
		Conversation conv;
		
		// ASSIGNMENT
		
		try {
			conv = new Conversation(120L);
			assertEquals(conv.getIndexEntry(), 120L);
		} catch (IndexOutOfBoundsException ex) {
			assertTrue(false);
		}
		
		// INDEX OUT OF BOUNDS
		
		try {
			conv = new Conversation(0x100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv = new Conversation(0L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv = new Conversation(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testSaveUpdate() throws DatabaseFileException, IOException {
		Database database = Database.getSingleton();
		
		boolean keysExchanged = true;
		String phoneNumber = "+123456789012";
		Time timeStamp = new Time(); timeStamp.setToNow();
		byte[] sessionKey_Out = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_Out = 0x12;
		byte[] sessionKey_In = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_In = 0x53;
		
		Conversation convWrite = database.createConversation("", new Time());
		Conversation convRead = new Conversation(convWrite.getIndexEntry());
		
		convWrite.setKeysExchanged(keysExchanged);
		convWrite.setPhoneNumber(phoneNumber);
		convWrite.setTimeStamp(timeStamp);
		convWrite.setSessionKey_Out(sessionKey_Out);
		convWrite.setLastID_Out(lastID_Out);
		convWrite.setSessionKey_In(sessionKey_In);
		convWrite.setLastID_In(lastID_In);
		convWrite.save();
		
		convRead.update();
		assertEquals(convRead.getKeysExchanged(), keysExchanged);
		assertEquals(convRead.getPhoneNumber(), phoneNumber);
		assertEquals(Time.compare(convRead.getTimeStamp(), timeStamp), 0);
		CustomAsserts.assertArrayEquals(convRead.getSessionKey_Out(), sessionKey_Out);
		assertEquals(convRead.getLastID_Out(), lastID_Out);
		CustomAsserts.assertArrayEquals(convRead.getSessionKey_In(), sessionKey_In);
		assertEquals(convRead.getLastID_In(), lastID_In);
	}
}
