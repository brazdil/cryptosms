package uk.ac.cam.db538.cryptosms.storage;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.MessageDataPart;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.CustomAsserts;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionNone;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import junit.framework.TestCase;

public class MessageDataPart_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		EncryptionNone.initEncryption();
		Common.clearStorageFile();	}

	protected void tearDown() throws Exception {
		super.tearDown();
		Common.closeStorageFile();
	}

	private boolean deliveredPart = true;
	private String messageBody = "Testing body";
	private byte[] messageBodyData = messageBody.getBytes();
	private short messageBodyLength = (short) messageBodyData.length;
	private long indexNext = 12L;
	
	private void setData(MessageDataPart msgPart) {
		msgPart.setDeliveredPart(deliveredPart);
		msgPart.setMessageBody(messageBodyData);
		msgPart.setIndexNext(indexNext);
	}
	
	private void checkData(MessageDataPart msgPart) {
		assertEquals(deliveredPart, msgPart.getDeliveredPart());
		assertEquals(msgPart.getMessageBody().length, messageBodyLength);
		CustomAsserts.assertArrayEquals(msgPart.getMessageBody(), messageBodyData);
		assertEquals(indexNext, msgPart.getIndexNext());
	}

	public void testConstruction() throws StorageFileException, IOException {
		// Check that it is assigned to a proper message, etc...
		Conversation conv = Conversation.createConversation();
		MessageData msg = MessageData.createMessageData(conv);
		ArrayList<MessageDataPart> list = new ArrayList<MessageDataPart>(2);
		MessageDataPart msgPart1 = MessageDataPart.createMessageDataPart();
		MessageDataPart msgPart2 = MessageDataPart.createMessageDataPart();
		list.add(msgPart1);
		list.add(msgPart2);
		msg.assignMessageDataParts(list);
		
		assertSame(msg.getFirstMessageDataPart(), msgPart1);
		assertNotSame(msg.getFirstMessageDataPart(), msgPart2);

		assertTrue(Common.checkStructure());
		
		// Check that the data is saved properly
		setData(msgPart1);
		msgPart1.saveToFile();
		long index = msgPart1.getEntryIndex();
		
		// force it to be re-read
		MessageDataPart.forceClearCache();
		msgPart1 = MessageDataPart.getMessageDataPart(index);
		
		checkData(msgPart1);
	}
	
	public void testIndices() throws StorageFileException, IOException {
		// INDICES OUT OF BOUNDS
		MessageDataPart msgPart = MessageDataPart.createMessageDataPart();
		
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

	public void testCreateData() throws StorageFileException, IOException, EncryptionException {
		// compute expected values
		byte flags = 0;
		flags |= (deliveredPart) ? 0x80 : 0x00;
		
		// generate data
		MessageDataPart msgPart = MessageDataPart.createMessageDataPart();
		setData(msgPart);
		msgPart.saveToFile();
		
		// get the generated data
		byte[] dataEncrypted = Storage.getStorage().getEntry(msgPart.getEntryIndex());
		
		// chunk length
		assertEquals(dataEncrypted.length, Storage.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.getEncryption().decryptSymmetricWithMasterKey(dataEncrypted);
		
		// check the data
		assertEquals(dataPlain[0], flags);
		assertEquals(LowLevel.getUnsignedShort(dataPlain, 1), messageBodyLength);
		CustomAsserts.assertArrayEquals(LowLevel.cutData(dataPlain, 3, messageBodyLength), messageBodyData);
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}

	public void testParseData() throws StorageFileException, IOException, EncryptionException {
		MessageDataPart msgPart = MessageDataPart.createMessageDataPart();
		long index = msgPart.getEntryIndex();

		// prepare stuff
		byte flags = 0;
		flags |= (deliveredPart) ? 0x80 : 0x00;

		// create plain data
		byte[] dataPlain = new byte[Storage.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(LowLevel.getBytesUnsignedShort(messageBodyLength), 0, dataPlain, 1, 2);
		System.arraycopy(LowLevel.wrapData(messageBodyData, 140), 0, dataPlain, 3, 140);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexNext), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.getEncryption().encryptSymmetricWithMasterKey(dataPlain);

		// inject it into the file
		Storage.getStorage().setEntry(index, dataEncrypted);
		
		// have it parsed
		MessageDataPart.forceClearCache();
		msgPart = MessageDataPart.getMessageDataPart(index);
		
		// check the indices
		checkData(msgPart);
	}
}
