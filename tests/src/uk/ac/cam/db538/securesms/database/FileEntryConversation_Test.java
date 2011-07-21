package uk.ac.cam.db538.securesms.database;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.text.format.Time;
import junit.framework.TestCase;

public class FileEntryConversation_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

/*	public void testFileEntryConversation() {
		FileEntryConversation conv;
		
		// ASSIGNMENT
		
		String phoneNumber = "+123456789012";
		Time timeStamp = new Time(); timeStamp.setToNow();
		long indexKeys = 12L;
		long indexMessages = 5L;
		long indexPrev = 120L;
		long indexNext = 248L;
		
		conv = new FileEntryConversation(phoneNumber, timeStamp, indexKeys, indexMessages, indexPrev, indexNext);
		assertEquals(conv.getPhoneNumber(), phoneNumber);
		assertEquals(conv.getTimeStamp(), timeStamp);
		assertEquals(conv.getIndexSessionKeys(), indexKeys);
		assertEquals(conv.getIndexMessages(), indexMessages);
		assertEquals(conv.getIndexPrev(), indexPrev);
		assertEquals(conv.getIndexNext(), indexNext);
		
		// INDICES OUT OF BOUNDS
		
		// indexSessionKeys
		try {
			conv = new FileEntryConversation(phoneNumber, timeStamp, 0x0100000000L, indexMessages, indexPrev, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
		
		try {
			conv = new FileEntryConversation(phoneNumber, timeStamp, -1L, indexMessages, indexPrev, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// indexMessages
		try {
			conv = new FileEntryConversation(phoneNumber, timeStamp, indexKeys, 0x0100000000L, indexPrev, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
		
		try {
			conv = new FileEntryConversation(phoneNumber, timeStamp, indexKeys, -1L, indexPrev, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// indexPrev
		try {
			conv = new FileEntryConversation(phoneNumber, timeStamp, indexKeys, indexMessages, 0x0100000000L, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv = new FileEntryConversation(phoneNumber, timeStamp, indexKeys, indexMessages, -1L, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// indexNext
		try {
			conv = new FileEntryConversation(phoneNumber, timeStamp, indexKeys, indexMessages, indexPrev, 0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv = new FileEntryConversation(phoneNumber, timeStamp, indexKeys, indexMessages, indexPrev, -1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() {
		byte flags = 0;
		String phoneNumberLong = "+1234567890126549873sdfsat6ewrt987wet3df1g3s2g1e6r5t46wert4dfsgdfsg";
		String phoneNumberResult = "+1234567890126549873sdfsat6ewrt9";
		Time timeStamp = new Time(); timeStamp.setToNow();
		long indexKeys = 12L;
		long indexMessages = 5L;
		long indexPrev = 120L;
		long indexNext = 248L;

		// get the generated data
		FileEntryConversation conv = new FileEntryConversation(phoneNumberLong, timeStamp, indexKeys, indexMessages, indexPrev, indexNext);
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = FileEntryConversation.createData(conv);
		} catch (DatabaseFileException e) {
			assertTrue(e.getMessage(), false);
		}
		
		// chunk length
		assertEquals(dataEncrypted.length, Database_Old.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		// check the data
		assertEquals(flags, dataPlain[0]);
		assertEquals(Database_Old.fromLatin(dataPlain, 1, 32), phoneNumberResult);
		Time time = new Time(); time.parse3339(Database_Old.fromLatin(dataPlain, 33, 29));
		assertEquals(Time.compare(time, timeStamp), 0);
		assertEquals(Database_Old.getInt(dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 16), indexKeys);
		assertEquals(Database_Old.getInt(dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 12), indexMessages);
		assertEquals(Database_Old.getInt(dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 8), indexPrev);
		assertEquals(Database_Old.getInt(dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}

	public void testParseData() {
		byte flags = 0;
		String phoneNumber = "+123456789012";
		Time timeStamp = new Time(); timeStamp.setToNow();
		long indexKeys = 12L;
		long indexMessages = 5L;
		long indexPrev = 120L;
		long indexNext = 248L;

		// create plain data
		byte[] dataPlain = new byte[Database_Old.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Database_Old.toLatin(phoneNumber, 32), 0, dataPlain, 1, 32);
		System.arraycopy(Database_Old.toLatin(timeStamp.format3339(false), 29), 0, dataPlain, 33, 29);
		System.arraycopy(Database_Old.getBytes(indexKeys), 0, dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 16, 4);
		System.arraycopy(Database_Old.getBytes(indexMessages), 0, dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 12, 4);
		System.arraycopy(Database_Old.getBytes(indexPrev), 0, dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 8, 4);
		System.arraycopy(Database_Old.getBytes(indexNext), 0, dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());

		// have it parsed
		FileEntryConversation conv = null;
		try {
			conv = FileEntryConversation.parseData(dataEncrypted);
		} catch (DatabaseFileException ex) {
			assertTrue(false);
		}
		
		// check the indices
		assertEquals(phoneNumber, conv.getPhoneNumber());
		assertEquals(Time.compare(timeStamp, conv.getTimeStamp()), 0);
		assertEquals(indexKeys, conv.getIndexSessionKeys());
		assertEquals(indexMessages, conv.getIndexMessages());
		assertEquals(indexPrev, conv.getIndexPrev());
		assertEquals(indexNext, conv.getIndexNext());
	}*/

}
