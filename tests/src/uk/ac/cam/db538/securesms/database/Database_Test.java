package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import uk.ac.cam.db538.securesms.CustomAsserts;
import uk.ac.cam.db538.securesms.database.Database;

public class Database_Test extends TestCase {

	private static final String TESTING_FILE = "/data/data/uk.ac.cam.db538.securesms/files/testing.db";
	
	protected void setUp() throws Exception {
		super.setUp();

		// delete the file before each test
		File file = new File(TESTING_FILE);
		if (file.exists())
			file.delete();
		
		// and free the singleton
		Database.freeSingleton();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testCreateFile() {
		Database history;
		try {
			// file shouldn't exist before
			assertFalse(new File(TESTING_FILE).exists());
			
			// should be created during getting of the singleton
			history = Database.getSingleton(TESTING_FILE);
			
			// then it should exist
			assertTrue(new File(TESTING_FILE).exists());
			
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
			history = Database.getSingleton(TESTING_FILE);
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
}
