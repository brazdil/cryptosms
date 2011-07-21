package uk.ac.cam.db538.securesms.database;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import junit.framework.TestCase;

public class FileEntryMessagePart_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
/*	public void testFileEntryMessagePart() {
		FileEntryMessagePart msgPart;
		
		// ASSIGNMENT
		
		boolean deliveredPart = true;
		String messageBody = "Testing body";
		long indexNext = 12L;
		
		msgPart = new FileEntryMessagePart(deliveredPart, messageBody, indexNext);
		assertEquals(msgPart.getDeliveredPart(), deliveredPart);
		assertEquals(msgPart.getMessageBody(), messageBody);
		assertEquals(msgPart.getIndexNext(), indexNext);
		
		// INDICES OUT OF BOUNDS
		
		// indexNext
		try {
			msgPart = new FileEntryMessagePart(deliveredPart, messageBody, 0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			msgPart = new FileEntryMessagePart(deliveredPart, messageBody, -1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() {
		boolean deliveredPart = true;
		String messageBody = "Testing body";
		long indexNext = 12L;
		
		// compute expected values
		byte flags = (byte) 0x80;
		
		// get the generated data
		FileEntryMessagePart message = new FileEntryMessagePart(deliveredPart, messageBody, indexNext);
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = FileEntryMessagePart.createData(message);
		} catch (DatabaseFileException e) {
			assertTrue(e.getMessage(), false);
		}
		
		// chunk length
		assertEquals(dataEncrypted.length, Database_Old.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		// check the data
		assertEquals(dataPlain[0], flags);
		assertEquals(Database_Old.fromLatin(dataPlain, 1, 140), messageBody);
		assertEquals(Database_Old.getInt(dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}

	public void testParseData() {
		boolean deliveredPart = true;
		String messageBody = "Testing body";
		long indexNext = 12L;

		// prepare stuff
		byte flags = (byte) 0x80;

		// create plain data
		byte[] dataPlain = new byte[Database_Old.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Database_Old.toLatin(messageBody, 140), 0, dataPlain, 1, 140);
		System.arraycopy(Database_Old.getBytes(indexNext), 0, dataPlain, Database_Old.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());

		// have it parsed
		FileEntryMessagePart message = null;
		try {
			message = FileEntryMessagePart.parseData(dataEncrypted);
		} catch (DatabaseFileException ex) {
			assertTrue(false);
		}
		
		// check the indices
		assertEquals(deliveredPart, message.getDeliveredPart());
		assertEquals(messageBody, message.getMessageBody());
		assertEquals(indexNext, message.getIndexNext());
	}
*/
}
