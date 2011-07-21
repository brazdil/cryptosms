package uk.ac.cam.db538.securesms.database;

import uk.ac.cam.db538.securesms.CustomAsserts;
import uk.ac.cam.db538.securesms.encryption.Encryption;
import junit.framework.TestCase;

public class FileEntrySessionKeys_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

/*	public void testFileEntryConversation() {
		FileEntrySessionKeys keys;
		
		// ASSIGNMENT
		
		boolean keysSent = true;
		boolean keysConfirmed = true;
		String phoneNumber = "+123456789012";
		byte[] sessionKey_Out = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_Out = 0x12;
		byte[] sessionKey_In = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_In = 0x18;
		long indexNext = 248L;
		
		keys = new FileEntrySessionKeys(keysSent, keysConfirmed, phoneNumber, sessionKey_Out, lastID_Out, sessionKey_In, lastID_In, indexNext);
		assertEquals(keysSent, keys.getKeysSent());
		assertEquals(keysConfirmed, keys.getKeysConfirmed());
		assertEquals(phoneNumber, keys.getPhoneNumber());
		CustomAsserts.assertArrayEquals(keys.getSessionKey_Out(), sessionKey_Out);
		assertEquals(lastID_Out, keys.getLastID_Out());
		CustomAsserts.assertArrayEquals(keys.getSessionKey_In(), sessionKey_In);
		assertEquals(lastID_In, keys.getLastID_In());
		
		// INDICES OUT OF BOUNDS

		// indexNext
		try {
			keys = new FileEntrySessionKeys(keysSent, keysConfirmed, phoneNumber, sessionKey_Out, lastID_Out, sessionKey_In, lastID_In, -1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			keys = new FileEntrySessionKeys(keysSent, keysConfirmed, phoneNumber, sessionKey_Out, lastID_Out, sessionKey_In, lastID_In, 0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() {
		boolean keysSent = true;
		boolean keysConfirmed = true;
		byte flags = (byte) 0xC0;
		String phoneNumberLong = "+1234567890126549873sdfsat6ewrt987wet3df1g3s2g1e6r5t46wert4dfsgdfsg";
		String phoneNumberResult = "+1234567890126549873sdfsat6ewrt9";
		byte[] sessionKey_Out = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_Out = 0x12;
		byte[] sessionKey_In = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_In = 0x18;
		long indexNext = 248L;

		// get the generated data
		FileEntrySessionKeys keys = new FileEntrySessionKeys(keysSent, keysConfirmed, phoneNumberLong, sessionKey_Out, lastID_Out, sessionKey_In, lastID_In, indexNext);
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = FileEntrySessionKeys.createData(keys);
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
		CustomAsserts.assertArrayEquals(dataPlain, 33, sessionKey_Out, 0, 32);
		assertEquals(lastID_Out, dataPlain[65]);
		CustomAsserts.assertArrayEquals(dataPlain, 66, sessionKey_In, 0, 32);
		assertEquals(lastID_In, dataPlain[98]);
		assertEquals(Database_Old.getInt(dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}

	public void testParseData() {
		boolean keysSent = true;
		boolean keysConfirmed = true;
		byte flags = (byte) 0xC0;
		String phoneNumber = "+1234567890126549873sdfsat6ewrt9";
		byte[] sessionKey_Out = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_Out = 0x12;
		byte[] sessionKey_In = Encryption.generateRandomData(Encryption.KEY_LENGTH);
		byte lastID_In = 0x18;
		long indexNext = 248L;

		// create plain data
		byte[] dataPlain = new byte[Database_Old.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Database_Old.toLatin(phoneNumber, 32), 0, dataPlain, 1, 32);
		System.arraycopy(sessionKey_Out, 0, dataPlain, 33, 32);
		dataPlain[65] = lastID_Out;
		System.arraycopy(sessionKey_In, 0, dataPlain, 66, 32);
		dataPlain[98] = lastID_In;
		System.arraycopy(Database_Old.getBytes(indexNext), 0, dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());

		// have it parsed
		FileEntrySessionKeys keys = null;
		try {
			keys = FileEntrySessionKeys.parseData(dataEncrypted);
		} catch (DatabaseFileException ex) {
			assertTrue(false);
		}
		
		// check the indices
		assertEquals(keysSent, keys.getKeysSent());
		assertEquals(keysConfirmed, keys.getKeysConfirmed());
		assertEquals(phoneNumber, keys.getPhoneNumber());
		CustomAsserts.assertArrayEquals(keys.getSessionKey_Out(), sessionKey_Out);
		assertEquals(lastID_Out, keys.getLastID_Out());
		CustomAsserts.assertArrayEquals(keys.getSessionKey_In(), sessionKey_In);
		assertEquals(lastID_In, keys.getLastID_In());
		assertEquals(indexNext, keys.getIndexNext());
	}
*/
}
