package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.text.format.Time;

import junit.framework.TestCase;

import uk.ac.cam.db538.securesms.CustomAsserts;
import uk.ac.cam.db538.securesms.database.Database;

public class Database_Test extends TestCase {

	static final String TESTING_FILE = "/data/data/uk.ac.cam.db538.securesms/files/testing.db";
	
	protected void setUp() throws Exception {
		super.setUp();

		// delete the file before each test
		File file = new File(TESTING_FILE);
		if (file.exists())
			file.delete();
		
		// and free the singleton
		Database.freeSingleton();
		Database.initSingleton(TESTING_FILE);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testGetInt() {
		byte[] data;
		long result;
		
		// CONVERSION TESTS
		
		data = new byte[] { 0x01, 0x00, 0x00, 0x00 };
		result = Database.getInt(data);
		assertEquals(16777216L, result);
		
		data = new byte[] { 0x00, 0x00, 0x00, 0x01 };
		result = Database.getInt(data);
		assertEquals(1L, result);
		
		data = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x89 };
		result = Database.getInt(data);
		assertEquals(2882400137L, result);
	
		data = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
		result = Database.getInt(data);
		assertEquals(4294967295L, result);

		// OFFSET TESTS

		// data somewhere in the bytes
		data = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x89, 0x00, 0x00, 0x00, 0x01 };
		result = Database.getInt(data, 4);
		assertEquals(1L, result);

		// index greater than limit
		try {
			data = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x89, 0x00, 0x00, 0x00, 0x01 };
			result = Database.getInt(data, 5);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
		
		// negative index
		try {
			data = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x89, 0x00, 0x00, 0x00, 0x01 };
			result = Database.getInt(data, -1);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}
	
	public void testGetBytes() {
		long number;
		byte[] result, expected;
		
		// CONVERSION TESTS
		
		expected = new byte[] { (byte) 0x84, (byte) 0xD2, (byte) 0xC3, (byte) 0x6E };
		number = 2228405102L;
		result = Database.getBytes(number);
		CustomAsserts.assertArrayEquals(expected, result);
		
		expected = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
		number = 4294967295L;
		result = Database.getBytes(number);
		CustomAsserts.assertArrayEquals(expected, result);
	}

	public void testToLatin() {
		// should trim after 5 bytes
		String strABCDE = "ABCDEFG";
		byte[] byteABCDE = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44, (byte) 0x45 };
		CustomAsserts.assertArrayEquals(Database.toLatin(strABCDE, 5), byteABCDE);

		// check only first four bytes - fifth is random
		String strABC = "ABC";
		byte[] byteABC = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x00, (byte) 0x00 };
		CustomAsserts.assertArrayEquals(Database.toLatin(strABC, 5), 0, byteABC, 4);
	}

	public void testFromLatin() {
		// simple
		byte[] byteABC = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x00, (byte) 0x00 };
		assertEquals(Database.fromLatin(byteABC), "ABC");

		// with offset
		byte[] byteABC2 = new byte[] { (byte) 0x00, (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x00, (byte) 0x00 };
		assertEquals(Database.fromLatin(byteABC2, 1, 5), "ABC");
		
		// full
		byte[] byteABCDE = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44, (byte) 0x45 };
		assertEquals(Database.fromLatin(byteABCDE), "ABCDE");
	}

	public void testCreateFile() {
		Database history;
		try {
			// delete the file before each test
			File file = new File(TESTING_FILE);
			if (file.exists())
				file.delete();
			
			// and free the singleton
			Database.freeSingleton();

			// file shouldn't exist before
			assertFalse(new File(TESTING_FILE).exists());
			
			// should be created during the initialisation
			Database.initSingleton(TESTING_FILE);
			// then it should exist
			assertTrue(new File(TESTING_FILE).exists());

			// now we can get the singleton
			history = Database.getSingleton();
			
			// and its size should be aligned as specified
			assertEquals(new File(TESTING_FILE).length(), Database.ALIGN_SIZE);

			// check structure
			assertTrue(history.checkStructure());
		} catch (DatabaseFileException e) {
			assertTrue(e.getMessage(), false);
		} catch (IOException e) {
			assertTrue(e.getMessage(), false);
		}
	}
	
	public void testAddFreeEntries() {
		Database history;
		try {
			// tests whether the number of added free entries fits
			history = Database.getSingleton();
			int countFree = history.getEmptyEntriesCount();
			history.addEmptyEntries(10);
			assertEquals(countFree + 10, history.getEmptyEntriesCount());

			// check structure
			assertTrue(history.checkStructure());
		} catch (DatabaseFileException e) {
			assertTrue(e.getMessage(), false);
		} catch (IOException e) {
			assertTrue(e.getMessage(), false);
		}
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
			assertEquals(conv1.getIndexEntry(), conv2.getIndexEntry());
			
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
	}

}
