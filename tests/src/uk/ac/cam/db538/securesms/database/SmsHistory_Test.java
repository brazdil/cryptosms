package uk.ac.cam.db538.securesms.database;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import uk.ac.cam.db538.securesms.database.SmsHistory;

public class SmsHistory_Test {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetFileVersion() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateFile() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddFreeEntries() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetInt() {
		byte[] data;
		long result;
		
		// CONVERSION TESTS
		
		data = new byte[] { 0x01, 0x00, 0x00, 0x00 };
		result = SmsHistory.getInt(data);
		assertEquals(16777216L, result);
		
		data = new byte[] { 0x00, 0x00, 0x00, 0x01 };
		result = SmsHistory.getInt(data);
		assertEquals(1L, result);
		
		data = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x89 };
		result = SmsHistory.getInt(data);
		assertEquals(2882400137L, result);
	
		data = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
		result = SmsHistory.getInt(data);
		assertEquals(4294967295L, result);

		// OFFSET TESTS

		// data somewhere in the bytes
		data = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x89, 0x00, 0x00, 0x00, 0x01 };
		result = SmsHistory.getInt(data, 4);
		assertEquals(1L, result);

		// index greater than limit
		try {
			data = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x89, 0x00, 0x00, 0x00, 0x01 };
			result = SmsHistory.getInt(data, 5);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
			assertTrue(true);
		}
		
		// negative index
		try {
			data = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x89, 0x00, 0x00, 0x00, 0x01 };
			result = SmsHistory.getInt(data, -1);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
			assertTrue(true);
		}
	}

	@Test
	public void testGetBytes() {
		long number;
		byte[] result, expected;
		
		// CONVERSION TESTS
		
		expected = new byte[] { (byte) 0x84, (byte) 0xD2, (byte) 0xC3, (byte) 0x6E };
		number = 2228405102L;
		result = SmsHistory.getBytes(number);
		assertArrayEquals(expected, result);
		
		expected = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
		number = 4294967295L;
		result = SmsHistory.getBytes(number);
		assertArrayEquals(expected, result);
	}

}
