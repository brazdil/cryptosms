package uk.ac.cam.db538.securesms.database;

import uk.ac.cam.db538.securesms.database.Message.MessageType;
import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.text.format.Time;
import junit.framework.TestCase;

public class FileEntryMessage_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testFileEntryMessage() {
		FileEntryMessage message;
		
		// ASSIGNMENT
		
		boolean deliveredPart = true;
		boolean deliveredAll = true;
		MessageType messageType = MessageType.OUTGOING;
		Time timeStamp = new Time(); timeStamp.setToNow();
		String messageBody = "Testing body";
		long indexMessageParts = 120L;
		long indexPrev = 225L;
		long indexNext = 12L;
		
		message = new FileEntryMessage(deliveredPart, deliveredAll, messageType, timeStamp, messageBody, indexMessageParts, indexPrev, indexNext);
		assertEquals(message.getDeliveredPart(), deliveredPart);
		assertEquals(message.getDeliveredAll(), deliveredAll);
		assertEquals(message.getMessageType(), messageType);
		assertEquals(Time.compare(message.getTimeStamp(), timeStamp), 0);
		assertEquals(message.getMessageBody(), messageBody);
		assertEquals(message.getIndexMessageParts(), indexMessageParts);
		assertEquals(message.getIndexPrev(), indexPrev);
		assertEquals(message.getIndexNext(), indexNext);
		
		// INDICES OUT OF BOUNDS
		
		// indexMessageParts
		try {
			message = new FileEntryMessage(deliveredPart, deliveredAll, messageType, timeStamp, messageBody, 0x0100000000L, indexPrev, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
		
		try {
			message = new FileEntryMessage(deliveredPart, deliveredAll, messageType, timeStamp, messageBody, -1L, indexPrev, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// indexPrev
		try {
			message = new FileEntryMessage(deliveredPart, deliveredAll, messageType, timeStamp, messageBody, indexMessageParts, 0x0100000000L, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			message = new FileEntryMessage(deliveredPart, deliveredAll, messageType, timeStamp, messageBody, indexMessageParts, -1L, indexNext);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// indexNext
		try {
			message = new FileEntryMessage(deliveredPart, deliveredAll, messageType, timeStamp, messageBody, indexMessageParts, indexPrev, 0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			message = new FileEntryMessage(deliveredPart, deliveredAll, messageType, timeStamp, messageBody, indexMessageParts, indexPrev, -1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() {
		boolean deliveredPart = true;
		boolean deliveredAll = true;
		MessageType messageType = MessageType.OUTGOING;
		Time timeStamp = new Time(); timeStamp.setToNow();
		String messageBody = "Testing body";
		long indexMessageParts = 120L;
		long indexPrev = 225L;
		long indexNext = 12L;
		
		// compute expected values
		byte flags = 0;
		flags |= (deliveredPart) ? 0x80 : 0x00;
		flags |= (deliveredAll) ? 0x40 : 0x00;
		flags |= (messageType == MessageType.OUTGOING) ? 0x20 : 0x00;
		
		// get the generated data
		FileEntryMessage message = new FileEntryMessage(deliveredPart, deliveredAll, messageType, timeStamp, messageBody, indexMessageParts, indexPrev, indexNext);
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = FileEntryMessage.createData(message);
		} catch (DatabaseFileException e) {
			assertTrue(e.getMessage(), false);
		}
		
		// chunk length
		assertEquals(dataEncrypted.length, Database.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		// check the data
		assertEquals(dataPlain[0], flags);
		Time time = new Time(); time.parse3339(Database.fromLatin(dataPlain, 1, 29));
		assertEquals(Time.compare(time, timeStamp), 0);
		assertEquals(Database.fromLatin(dataPlain, 30, 140), messageBody);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 12), indexMessageParts);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 8), indexPrev);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}

	public void testParseData() {
		boolean deliveredPart = true;
		boolean deliveredAll = true;
		MessageType messageType = MessageType.OUTGOING;
		Time timeStamp = new Time(); timeStamp.setToNow();
		String messageBody = "Testing body";
		long indexMessageParts = 120L;
		long indexPrev = 225L;
		long indexNext = 12L;

		// prepare stuff
		byte flags = 0;
		flags |= (deliveredPart) ? 0x80 : 0x00;
		flags |= (deliveredAll) ? 0x40 : 0x00;
		flags |= (messageType == MessageType.OUTGOING) ? 0x20 : 0x00;

		// create plain data
		byte[] dataPlain = new byte[Database.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Database.toLatin(timeStamp.format3339(false), 29), 0, dataPlain, 1, 29);
		System.arraycopy(Database.toLatin(messageBody, 140), 0, dataPlain, 30, 140);
		System.arraycopy(Database.getBytes(indexMessageParts), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 12, 4);
		System.arraycopy(Database.getBytes(indexPrev), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 8, 4);
		System.arraycopy(Database.getBytes(indexNext), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());

		// have it parsed
		FileEntryMessage message = null;
		try {
			message = FileEntryMessage.parseData(dataEncrypted);
		} catch (DatabaseFileException ex) {
			assertTrue(false);
		}
		
		// check the indices
		assertEquals(deliveredPart, message.getDeliveredPart());
		assertEquals(deliveredAll, message.getDeliveredAll());
		assertEquals(messageType, message.getMessageType());
		assertEquals(Time.compare(timeStamp, message.getTimeStamp()), 0);
		assertEquals(messageBody, message.getMessageBody());
		assertEquals(indexMessageParts, message.getIndexMessageParts());
		assertEquals(indexPrev, message.getIndexPrev());
		assertEquals(indexNext, message.getIndexNext());
	}

}
