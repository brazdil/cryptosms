package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.IOException;

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
		
		String phoneNumber = "+123456789012";
		Time timeStamp = new Time(); timeStamp.set(5, 2, 1928);
		
		Conversation convWrite = database.createConversation("", new Time());
		Conversation convRead = new Conversation(convWrite.getIndexEntry());
		
		convWrite.setPhoneNumber(phoneNumber);
		convWrite.setTimeStamp(timeStamp);
		convWrite.save();
		
		convRead.update();
		assertEquals(convRead.getPhoneNumber(), phoneNumber);
		assertEquals(Time.compare(convRead.getTimeStamp(), timeStamp), 0);
	}
}
