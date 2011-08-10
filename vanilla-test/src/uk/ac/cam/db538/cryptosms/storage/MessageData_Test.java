package uk.ac.cam.db538.cryptosms.storage;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.ISODateTimeFormat;

import uk.ac.cam.db538.cryptosms.CustomAsserts;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionNone;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.MessageData.MessageType;
import uk.ac.cam.db538.cryptosms.utils.Charset;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import junit.framework.TestCase;

public class MessageData_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		EncryptionNone.initEncryption();
		Common.clearStorageFile();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		Common.closeStorageFile();
	}
	
	private boolean deliveredPart = true;
	private boolean deliveredAll = true;
	private MessageType messageType = MessageType.OUTGOING;
	private boolean unread = true;
	private boolean compressed = true;
	private boolean ascii = true;
	private DateTime timeStamp = new DateTime(); 
	private String messageBody = "Testing body";
	private byte[] messageBodyData = messageBody.getBytes();
	private short messageBodyLength = (short) messageBodyData.length;
	private long indexParent = 127L;
	private long indexMessageParts = 120L;
	private long indexPrev = 225L;
	private long indexNext = 12L;
	
	private void setData(MessageData msg) {
		msg.setDeliveredPart(deliveredPart);
		msg.setDeliveredAll(deliveredAll);
		msg.setMessageType(messageType);
		msg.setUnread(unread);
		msg.setCompressed(compressed);
		msg.setAscii(ascii);
		msg.setTimeStamp(timeStamp);
		msg.setMessageBody(messageBodyData);
		msg.setIndexParent(indexParent);
		msg.setIndexMessageParts(indexMessageParts);
		msg.setIndexPrev(indexPrev);
		msg.setIndexNext(indexNext);
	}
	
	private void checkData(MessageData msg) {
		assertEquals(msg.getDeliveredPart(), deliveredPart);
		assertEquals(msg.getDeliveredAll(), deliveredAll);
		assertEquals(msg.getMessageType(), messageType);
		assertEquals(msg.getUnread(), unread);
		assertEquals(msg.getCompressed(), compressed);
		assertEquals(msg.getAscii(), ascii);
		assertEquals(DateTimeComparator.getInstance().compare(msg.getTimeStamp(), timeStamp), 0);
		assertEquals(msg.getMessageBody().length, messageBodyLength);
		CustomAsserts.assertArrayEquals(msg.getMessageBody(), messageBodyData);
		assertEquals(msg.getIndexParent(), indexParent);
		assertEquals(msg.getIndexMessageParts(), indexMessageParts);
		assertEquals(msg.getIndexPrev(), indexPrev);
		assertEquals(msg.getIndexNext(), indexNext);
	}
	
	public void testConstruction() throws StorageFileException, IOException {
		// create a Message entry
		Conversation conv = Conversation.createConversation();
		MessageData msg = MessageData.createMessageData(conv);

		// check structure
		assertTrue(Common.checkStructure());
		
		setData(msg);
		msg.saveToFile();
		long index = msg.getEntryIndex();
		
		// for it to be re-read
		MessageData.forceClearCache();
		msg = MessageData.getMessageData(index);
		
		checkData(msg);
	}
	
	public void testIndices() throws StorageFileException, IOException {
		// INDICES OUT OF BOUNDS
		Conversation conv = Conversation.createConversation();
		MessageData msg = MessageData.createMessageData(conv);
	
		// indexMessageParts
		try {
			msg.setIndexMessageParts(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
		
		try {
			msg.setIndexMessageParts(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	
		// indexPrev
		try {
			msg.setIndexPrev(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	
		try {
			msg.setIndexPrev(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	
		// indexNext
		try {
			msg.setIndexNext(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	
		try {
			msg.setIndexNext(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}
	
	public void testSetNumberOfParts() throws StorageFileException, IOException {
		// set data
		Conversation conv = Conversation.createConversation();
		MessageData msg = MessageData.createMessageData(conv);
		long index = msg.getEntryIndex();
		assertTrue(Common.checkStructure());
		
		msg = MessageData.getMessageData(index); // because checkStructure clears cache
		msg.setNumberOfParts(5);
		assertTrue(Common.checkStructure());
		assertNotNull(msg.getPartData(4));
		try {
			msg.getPartData(5);
			fail("Should not reach here!");
		} catch (IndexOutOfBoundsException e) {
		}
		
		msg = MessageData.getMessageData(index);
		msg.setNumberOfParts(7);
		assertTrue(Common.checkStructure());
		assertNotNull(msg.getPartData(2));
		assertNotNull(msg.getPartData(3));
		assertNotNull(msg.getPartData(4));
		assertNotNull(msg.getPartData(6));
		try {
			msg.getPartData(7);
			fail("Should not reach here!");
		} catch (IndexOutOfBoundsException e) {
		}

		msg = MessageData.getMessageData(index);
		msg.setNumberOfParts(4);
		assertTrue(Common.checkStructure());
		assertNotNull(msg.getPartData(0));
		assertNotNull(msg.getPartData(1));
		assertNotNull(msg.getPartData(2));
		assertNotNull(msg.getPartData(3));
		try {
			msg.getPartData(4);
			fail("Should not reach here!");
		} catch (IndexOutOfBoundsException e) {
		}

		msg = MessageData.getMessageData(index);
		msg.setNumberOfParts(1);
		assertTrue(Common.checkStructure());
		try {
			msg.getPartData(1);
			fail("Should not reach here!");
		} catch (IndexOutOfBoundsException e) {
		}
	}
	
	public void testSettingGettingPartData() throws StorageFileException, IOException {
		Conversation conv = Conversation.createConversation();
		MessageData msg = MessageData.createMessageData(conv);
		msg.setNumberOfParts(3);
		
		// indices out of bounds
		try {
			msg.getPartData(-1);
			fail("Should not reach here");
		} catch (IndexOutOfBoundsException e) {
		}

		try {
			msg.getPartData(3);
			fail("Should not reach here");
		} catch (IndexOutOfBoundsException e) {
		}

		try {
			msg.setPartData(-1, new byte[4]);
			fail("Should not reach here");
		} catch (IndexOutOfBoundsException e) {
		}

		try {
			msg.setPartData(3, new byte[4]);
			fail("Should not reach here");
		} catch (IndexOutOfBoundsException e) {
		}

		try {
			msg.getPartDelivered(-1);
			fail("Should not reach here");
		} catch (IndexOutOfBoundsException e) {
		}

		try {
			msg.getPartDelivered(3);
			fail("Should not reach here");
		} catch (IndexOutOfBoundsException e) {
		}

		try {
			msg.setPartDelivered(-1, false);
			fail("Should not reach here");
		} catch (IndexOutOfBoundsException e) {
		}

		try {
			msg.setPartDelivered(3, false);
			fail("Should not reach here");
		} catch (IndexOutOfBoundsException e) {
		}
		
		// setting/getting
		
		byte[] data = Encryption.getEncryption().generateRandomData(280);
		byte[] dataCut = LowLevel.cutData(data, 0, 133);
		
		msg.setPartData(2, data);
		assertEquals(0, msg.getPartData(0).length);
		assertEquals(0, msg.getPartData(1).length);
		CustomAsserts.assertArrayEquals(msg.getPartData(2), dataCut);
		
		msg.setPartData(0, data);
		CustomAsserts.assertArrayEquals(msg.getPartData(0), dataCut);
		assertEquals(0, msg.getPartData(1).length);
		CustomAsserts.assertArrayEquals(msg.getPartData(2), dataCut);

		msg.setPartDelivered(1, true);
		assertEquals(false, msg.getPartDelivered(0));
		assertEquals(true, msg.getPartDelivered(1));
		assertEquals(false, msg.getPartDelivered(2));

		msg.setPartDelivered(0, true);
		assertEquals(true, msg.getPartDelivered(0));
		assertEquals(true, msg.getPartDelivered(1));
		assertEquals(false, msg.getPartDelivered(2));

		msg.setPartDelivered(1, false);
		assertEquals(true, msg.getPartDelivered(0));
		assertEquals(false, msg.getPartDelivered(1));
		assertEquals(false, msg.getPartDelivered(2));
	}
	
	public void testCreateData() throws StorageFileException, IOException, EncryptionException {
		// set data
		Conversation conv = Conversation.createConversation();
		MessageData msg = MessageData.createMessageData(conv);
		setData(msg);
		msg.saveToFile();
		
		// compute expected values
		byte flags = 0;
		flags |= (deliveredPart) ? 0x80 : 0x00;
		flags |= (deliveredAll) ? 0x40 : 0x00;
		flags |= (messageType == MessageType.OUTGOING) ? 0x20 : 0x00;
		flags |= (unread) ? 0x10 : 0x00;
		flags |= (compressed) ? 0x08 : 0x00;
		flags |= (ascii) ? 0x04: 0x00;
		
		// get the generated data
		byte[] dataEncrypted = Storage.getStorage().getEntry(msg.getEntryIndex());
		
		// chunk length
		assertEquals(dataEncrypted.length, Storage.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.getEncryption().decryptSymmetricWithMasterKey(dataEncrypted);
		
		// check the data
		assertEquals(dataPlain[0], flags);
		DateTime time = ISODateTimeFormat.dateTimeParser().parseDateTime(Charset.fromAscii8(dataPlain, 1, 29));
		assertEquals(DateTimeComparator.getInstance().compare(time, timeStamp), 0);
		assertEquals(LowLevel.getUnsignedShort(dataPlain, 30), messageBodyLength);
		CustomAsserts.assertArrayEquals(LowLevel.cutData(dataPlain, 32, messageBodyLength), messageBodyData);
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 16), indexParent);
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 12), indexMessageParts);
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 8), indexPrev);
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}
	
	public void testParseData() throws StorageFileException, IOException, EncryptionException {
		Conversation conv = Conversation.createConversation();
		MessageData msg = MessageData.createMessageData(conv);
		long index = msg.getEntryIndex();
		
		// prepare stuff
		byte flags = 0;
		flags |= (deliveredPart) ? 0x80 : 0x00;
		flags |= (deliveredAll) ? 0x40 : 0x00;
		flags |= (messageType == MessageType.OUTGOING) ? 0x20 : 0x00;
		flags |= (unread) ? 0x10 : 0x00;
		flags |= (compressed) ? 0x08 : 0x00;
		flags |= (ascii) ? 0x04: 0x00;

		// create plain data
		byte[] dataPlain = new byte[Storage.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Charset.toAscii8(ISODateTimeFormat.dateTime().print(timeStamp), 29), 0, dataPlain, 1, 29);
		System.arraycopy(LowLevel.getBytesUnsignedShort(messageBodyLength), 0, dataPlain, 30, 2);
		System.arraycopy(LowLevel.wrapData(messageBodyData, 140), 0, dataPlain, 32, 140);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexParent), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 16, 4);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexMessageParts), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 12, 4);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexPrev), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 8, 4);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexNext), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.getEncryption().encryptSymmetricWithMasterKey(dataPlain);
	
		// inject it in the file
		Storage.getStorage().setEntry(index, dataEncrypted);

		// have it parsed
		MessageData.forceClearCache();
		msg = MessageData.getMessageData(index);
		
		// check the indices
		assertEquals(deliveredPart, msg.getDeliveredPart());
		assertEquals(deliveredAll, msg.getDeliveredAll());
		assertEquals(messageType, msg.getMessageType());
		assertEquals(unread, msg.getUnread());
		assertEquals(compressed, msg.getCompressed());
		assertEquals(ascii, msg.getAscii());
		assertEquals(DateTimeComparator.getInstance().compare(timeStamp, msg.getTimeStamp()), 0);
		assertEquals(messageBodyLength, msg.getMessageBody().length);
		CustomAsserts.assertArrayEquals(messageBodyData, msg.getMessageBody());
		assertEquals(indexParent, msg.getIndexParent());
		assertEquals(indexMessageParts, msg.getIndexMessageParts());
		assertEquals(indexPrev, msg.getIndexPrev());
		assertEquals(indexNext, msg.getIndexNext());
	}
}
