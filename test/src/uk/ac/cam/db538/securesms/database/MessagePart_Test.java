package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.database.Message.MessageType;
import uk.ac.cam.db538.securesms.encryption.Encryption;
import junit.framework.TestCase;

public class MessagePart_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		Common.clearFile();	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private boolean deliveredPart = true;
	private String messageBody = "Testing body";
	private long indexNext = 12L;
	
	private void setData(MessagePart msgPart) {
		msgPart.setDeliveredPart(deliveredPart);
		msgPart.setMessageBody(messageBody);
		msgPart.setIndexNext(indexNext);
	}
	
	private void checkData(MessagePart msgPart) {
		assertEquals(deliveredPart, msgPart.getDeliveredPart());
		assertEquals(messageBody, msgPart.getMessageBody());
		assertEquals(indexNext, msgPart.getIndexNext());
	}

	public void testConstruction() throws DatabaseFileException, IOException {
		// Check that it is assigned to a proper message, etc...
		Conversation conv = Conversation.createConversation();
		Message msg = Message.createMessage();
		conv.attachMessage(msg);
		
		ArrayList<MessagePart> list = new ArrayList<MessagePart>(2);
		MessagePart msgPart1 = MessagePart.createMessagePart();
		MessagePart msgPart2 = MessagePart.createMessagePart();
		list.add(msgPart1);
		list.add(msgPart2);
		msg.assignMessageParts(list);
		
		assertSame(msg.getFirstMessagePart(), msgPart1);
		assertNotSame(msg.getFirstMessagePart(), msgPart2);
		
		// Check that the data is saved properly
		setData(msgPart1);
		msgPart1.saveToFile();
		long index = msgPart1.getEntryIndex();
		
		// force it to be re-read
		MessagePart.forceClearCache();
		msgPart1 = MessagePart.getMessagePart(index);
		
		checkData(msgPart1);
		assertTrue(Common.checkStructure());
	}
	
	public void testIndices() throws DatabaseFileException, IOException {
		// INDICES OUT OF BOUNDS
		MessagePart msgPart = MessagePart.createMessagePart();
		
		// indexNext
		try {
			msgPart.setIndexNext(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			msgPart.setIndexNext(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() throws DatabaseFileException, IOException {
		// compute expected values
		byte flags = 0;
		flags |= (deliveredPart) ? 0x80 : 0x00;
		
		// generate data
		MessagePart msgPart = MessagePart.createMessagePart();
		setData(msgPart);
		msgPart.saveToFile();
		
		// get the generated data
		byte[] dataEncrypted = Database.getDatabase().getEntry(msgPart.getEntryIndex());
		
		// chunk length
		assertEquals(dataEncrypted.length, Database.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		// check the data
		assertEquals(dataPlain[0], flags);
		assertEquals(Database.fromLatin(dataPlain, 1, 140), messageBody);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}

	public void testParseData() throws DatabaseFileException, IOException {
		MessagePart msgPart = MessagePart.createMessagePart();
		long index = msgPart.getEntryIndex();

		// prepare stuff
		byte flags = 0;
		flags |= (deliveredPart) ? 0x80 : 0x00;

		// create plain data
		byte[] dataPlain = new byte[Database.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Database.toLatin(messageBody, 140), 0, dataPlain, 1, 140);
		System.arraycopy(Database.getBytes(indexNext), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());

		// inject it into the file
		Database.getDatabase().setEntry(index, dataEncrypted);
		
		// have it parsed
		MessagePart.forceClearCache();
		msgPart = MessagePart.getMessagePart(index);
		
		// check the indices
		checkData(msgPart);
	}
}
