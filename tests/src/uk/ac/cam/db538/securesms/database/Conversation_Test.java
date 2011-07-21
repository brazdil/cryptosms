package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.text.format.Time;

import junit.framework.TestCase;

public class Conversation_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		Common.clearFile();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

/*	public void testConversation() {
		Conversation_Old conv;
		FileEntryConversation entry = new FileEntryConversation("", null, 12L, 15L, 5L, 7L);
		// ASSIGNMENT
		
		try {
			conv = new Conversation_Old(120L, entry);
			assertEquals(conv.getEntryIndex(), 120L);
			assertEquals(conv.getEntry(), entry);
		} catch (IndexOutOfBoundsException ex) {
			assertTrue(false);
		}
		
		// INDEX OUT OF BOUNDS
		
		try {
			conv = new Conversation_Old(0x100000000L, entry);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv = new Conversation_Old(0L, entry);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv = new Conversation_Old(-1L, entry);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testSaveUpdate() throws DatabaseFileException, IOException {
		Database_Old database = Database_Old.getSingleton();
		
		String phoneNumber = "+123456789012";
		Time timeStamp = new Time(); timeStamp.set(5, 2, 1928);
		
		Conversation_Old convWrite = database.createConversation("", new Time());
		Conversation_Old convRead = new Conversation_Old(convWrite.getEntryIndex());
		
		convWrite.setPhoneNumber(phoneNumber);
		convWrite.setTimeStamp(timeStamp);
		convWrite.save();
		
		convRead.update();
		assertEquals(convRead.getPhoneNumber(), phoneNumber);
		assertEquals(Time.compare(convRead.getTimeStamp(), timeStamp), 0);
	}

	public void testCreateConversation() {
		Database history;
		try {
			history = Database.getSingleton();
			Time time = new Time(); time.setToNow();

			// check that it takes only one entry
			int countEmpty = history.getEmptyEntriesCount();
			for (int i = 0; i < Database.ALIGN_SIZE / Database.CHUNK_SIZE * 5; ++i)
			{
				history.createConversation("Shut the fuck up!", time);
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
	}

	public void testGetConversation() {
		Database history;
		try {
			history = Database.getSingleton();
			Time time = new Time(); time.setToNow();

			String phoneNumberUK = "07572458912";
			String phoneNumberInternational = "+447572458912";
			String phoneNumberDifferent = "458912";
			
			// conv1 should have the UK number stored
			Conversation conv1 = history.createConversation(phoneNumberUK);
			assertEquals(conv1.getPhoneNumber(), phoneNumberUK);
			
			// try to find it using the international number
			Conversation conv2 = history.getConversation(phoneNumberInternational);
			assertEquals(conv1.getEntryIndex(), conv2.getEntryIndex());
			
			// it should change the number to international
			assertEquals(conv2.getPhoneNumber(), phoneNumberInternational);
			conv1.update();
			assertEquals(conv1.getPhoneNumber(), phoneNumberInternational);
			
			// try finding a different number
			assertEquals(history.getConversation(phoneNumberDifferent), null);
			
			// check structure
			assertTrue(history.checkStructure());
		} catch (DatabaseFileException e) {
			assertTrue(e.getMessage(), false);
		} catch (IOException e) {
			assertTrue(e.getMessage(), false);
		}
	}

	public void testGetAllConversations() {
		Database history;
		try {
			history = Database.getSingleton();
			Time time1 = new Time(); time1.set(1, 1, 1998);
			Time time2 = new Time(); time2.set(1, 1, 1994);
			Time time3 = new Time(); time3.set(1, 1, 1996);
			Time time4 = new Time(); time4.set(1, 1, 1989);

			Conversation conv1 = history.createConversation("1");
			conv1.setTimeStamp(time1);
			conv1.save();
			Conversation conv2 = history.createConversation("2");
			conv2.setTimeStamp(time2);
			conv2.save();
			Conversation conv3 = history.createConversation("3");
			conv3.setTimeStamp(time3);
			conv3.save();
			Conversation conv4 = history.createConversation("4");
			conv4.setTimeStamp(time4);
			conv4.save();
			
			// get all conversations
			ArrayList<Conversation> list = history.getListOfConversations();
			
			// check that it is there and sorted
			assertEquals(list.get(0).getPhoneNumber(), "4");
			assertEquals(list.get(1).getPhoneNumber(), "2");
			assertEquals(list.get(2).getPhoneNumber(), "3");
			assertEquals(list.get(3).getPhoneNumber(), "1");
			
			// check structure
			assertTrue(history.checkStructure());
		} catch (DatabaseFileException e) {
			assertTrue(e.getMessage(), false);
		} catch (IOException e) {
			assertTrue(e.getMessage(), false);
		}
	}*/
}
