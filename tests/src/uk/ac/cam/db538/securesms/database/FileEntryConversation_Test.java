package uk.ac.cam.db538.securesms.database;

import uk.ac.cam.db538.securesms.CustomAsserts;
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

	public void testFileEntryConversation() {
		FileEntryConversation conv;
		
		// ASSIGNMENT
		
		boolean keysExchanged = true;
		String phoneNumber = "+123456789012";
		Time timeStamp = new Time(); timeStamp.setToNow();
		byte[] sessionKey_Out = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte[] sessionKey_In = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		long indexMessages = 5L;
		long indexPrev = 120L;
		long indexNext = 248L;
		
		conv = new FileEntryConversation(keysExchanged, phoneNumber, timeStamp, sessionKey_Out, sessionKey_In, indexMessages, indexPrev, indexNext);
		assertEquals(conv.getKeysExchanged(), keysExchanged);
		assertEquals(conv.getPhoneNumber(), phoneNumber);
		assertEquals(conv.getTimeStamp(), timeStamp);
		CustomAsserts.assertArrayEquals(conv.getSessionKey_Out(), sessionKey_Out);
		CustomAsserts.assertArrayEquals(conv.getSessionKey_In(), sessionKey_In);
		assertEquals(conv.getIndexMessages(), indexMessages);
		assertEquals(conv.getIndexPrev(), indexPrev);
		assertEquals(conv.getIndexNext(), indexNext);
		
		// INDICES OUT OF BOUNDS
		
		// indexMessages
		try {
			conv = new FileEntryConversation(keysExchanged, phoneNumber, timeStamp, sessionKey_Out, sessionKey_In, 0x0100000000L, indexPrev, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
		
		try {
			conv = new FileEntryConversation(keysExchanged, phoneNumber, timeStamp, sessionKey_Out, sessionKey_In, -1L, indexPrev, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// indexPrev
		try {
			conv = new FileEntryConversation(keysExchanged, phoneNumber, timeStamp, sessionKey_Out, sessionKey_In, indexMessages, 0x0100000000L, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv = new FileEntryConversation(keysExchanged, phoneNumber, timeStamp, sessionKey_Out, sessionKey_In, indexMessages, -1L, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// indexNext
		try {
			conv = new FileEntryConversation(keysExchanged, phoneNumber, timeStamp, sessionKey_Out, sessionKey_In, indexMessages, indexPrev, 0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv = new FileEntryConversation(keysExchanged, phoneNumber, timeStamp, sessionKey_Out, sessionKey_In, indexMessages, indexPrev, -1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() {
		boolean keysExchanged = true;
		String phoneNumberLong = "+1234567890126549873sdfsat6ewrt987wet3df1g3s2g1e6r5t46wert4dfsgdfsg";
		String phoneNumberResult = "+1234567890126549873sdfsat6ewrt9";
		Time timeStamp = new Time(); timeStamp.setToNow();
		byte[] sessionKey_Out = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte[] sessionKey_In = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		long indexMessages = 5L;
		long indexPrev = 120L;
		long indexNext = 248L;

		// compute expected values
		byte flags = 0;
		flags |= (keysExchanged) ? 0x80 : 0x00;
		
		// get the generated data
		FileEntryConversation conv = new FileEntryConversation(keysExchanged, phoneNumberLong, timeStamp, sessionKey_Out, sessionKey_In, indexMessages, indexPrev, indexNext);
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = FileEntryConversation.createData(conv);
		} catch (DatabaseFileException e) {
			assertTrue(e.getMessage(), false);
		}
		
		// chunk length
		assertEquals(dataEncrypted.length, Database.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		// check the data
		assertEquals(dataPlain[0], flags);
		assertEquals(Database.fromLatin(dataPlain, 1, 32), phoneNumberResult);
		Time time = new Time(); time.parse3339(Database.fromLatin(dataPlain, 33, 29));
		assertEquals(Time.compare(time, timeStamp), 0);
		CustomAsserts.assertArrayEquals(dataPlain, 62, sessionKey_Out, Encryption.KEY_LENGTH);
		CustomAsserts.assertArrayEquals(dataPlain, 94, sessionKey_In, Encryption.KEY_LENGTH);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 12), indexMessages);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 8), indexPrev);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}

	public void testParseData() {
		boolean keysExchanged = true;
		String phoneNumber = "+123456789012";
		Time timeStamp = new Time(); timeStamp.setToNow();
		byte[] sessionKey_Out = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte[] sessionKey_In = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		long indexMessages = 5L;
		long indexPrev = 120L;
		long indexNext = 248L;

		// prepare stuff
		byte flags = 0;
		flags |= (keysExchanged) ? 0x80 : 0x00;

		// create plain data
		byte[] dataPlain = new byte[Database.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Database.toLatin(phoneNumber, 32), 0, dataPlain, 1, 32);
		System.arraycopy(Database.toLatin(timeStamp.format3339(false), 29), 0, dataPlain, 33, 29);
		System.arraycopy(sessionKey_Out, 0, dataPlain, 62, 32);
		System.arraycopy(sessionKey_In, 0, dataPlain, 94, 32);
		System.arraycopy(Database.getBytes(indexMessages), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 12, 4);
		System.arraycopy(Database.getBytes(indexPrev), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 8, 4);
		System.arraycopy(Database.getBytes(indexNext), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
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
		assertEquals(keysExchanged, conv.getKeysExchanged());
		assertEquals(phoneNumber, conv.getPhoneNumber());
		assertEquals(Time.compare(timeStamp, conv.getTimeStamp()), 0);
		CustomAsserts.assertArrayEquals(sessionKey_Out, conv.getSessionKey_Out());
		CustomAsserts.assertArrayEquals(sessionKey_In, conv.getSessionKey_In());
		assertEquals(indexMessages, conv.getIndexMessages());
		assertEquals(indexPrev, conv.getIndexPrev());
		assertEquals(indexNext, conv.getIndexNext());
	}

}
