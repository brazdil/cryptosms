package uk.ac.cam.db538.securesms.storage;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.Charset;
import uk.ac.cam.db538.securesms.Encryption;
import uk.ac.cam.db538.securesms.data.LowLevel;
import uk.ac.cam.db538.securesms.storage.Conversation;
import uk.ac.cam.db538.securesms.storage.Storage;
import uk.ac.cam.db538.securesms.storage.StorageFileException;
import uk.ac.cam.db538.securesms.storage.Empty;
import uk.ac.cam.db538.securesms.storage.Header;
import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.MessageDataPart;
import uk.ac.cam.db538.securesms.storage.SessionKeys;
import junit.framework.TestCase;

public class Conversation_Test extends TestCase {

	// Testing data
	private String phoneNumber = "+447896512369";
	private String phoneNumberLong = "+1234567890126549873sdfsat6ewrt987wet3df1g3s2g1e6r5t46wert4dfsgdfsg";
	private String phoneNumberResult = "+1234567890126549873sdfsat6ewrt9";
	private long indexSessionKeys = 122L;
	private long indexMessages = 120L;
	private long indexPrev = 12L;
	private long indexNext = 15L;

	private void setData(Conversation conv, boolean longPhoneNumber) {
		conv.setPhoneNumber((longPhoneNumber) ? phoneNumberLong : phoneNumber);
		conv.setIndexSessionKeys(indexSessionKeys);
		conv.setIndexMessages(indexMessages);
		conv.setIndexPrev(indexPrev);
		conv.setIndexNext(indexNext);
	}
	
	private void checkData(Conversation conv, boolean longPhoneNumber) {
		assertEquals(conv.getPhoneNumber(), (longPhoneNumber) ? phoneNumberResult : phoneNumber);
		assertEquals(conv.getIndexSessionKeys(), indexSessionKeys);
		assertEquals(conv.getIndexMessages(), indexMessages);
		assertEquals(conv.getIndexPrev(), indexPrev);
		assertEquals(conv.getIndexNext(), indexNext);
	}

	protected void setUp() throws Exception {
		super.setUp();
		Common.clearStorageFile();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testConstruction() throws StorageFileException, IOException {
		Conversation conv = Conversation.createConversation();
		assertTrue(Common.checkStructure());

		setData(conv, false);
		conv.saveToFile();
		long index = conv.getEntryIndex();
		
		// force it to be re-read from file
		Conversation.forceClearCache();
		conv = Conversation.getConversation(index);
		checkData(conv, false);
	}
	
	public void testDelete() throws StorageFileException, IOException {
		for (int i = 0; i < 5; i++)
		{
			Conversation conv = Conversation.createConversation();
			
			for (int j = 0; j < 10; ++j)
			{
				MessageData msg = MessageData.createMessageData(conv);
				{
					MessageDataPart part1 = MessageDataPart.createMessageDataPart();
					MessageDataPart part2 = MessageDataPart.createMessageDataPart();
					MessageDataPart part3 = MessageDataPart.createMessageDataPart();
					MessageDataPart part4 = MessageDataPart.createMessageDataPart();
					MessageDataPart part5 = MessageDataPart.createMessageDataPart();
					
					ArrayList<MessageDataPart> list1 = new ArrayList<MessageDataPart>();
					ArrayList<MessageDataPart> list2 = new ArrayList<MessageDataPart>();
					
					list1.add(part1);
					list1.add(part2);
					list1.add(part3);
					msg.assignMessageDataParts(list1);
					assertSame(msg.getFirstMessageDataPart(), part1);
					
					list2.add(part4);
					list2.add(part5);
					msg.assignMessageDataParts(list2);
					
					assertSame(msg.getFirstMessageDataPart(), part4);
				}
			}
			
			for (int k = 0; k < 4; ++k)
				SessionKeys.createSessionKeys(conv);
		}
		
		assertTrue(Common.checkStructure());
		
		Conversation conv;
		while ((conv = Header.getHeader().getFirstConversation()) != null) {
			conv.delete();
			assertTrue(Common.checkStructure());
		}
	}

	public void testIndices() throws StorageFileException, IOException {
		// INDEX OUT OF BOUNDS
		Conversation conv = Conversation.createConversation();
		
		// indexSessionKeys
		try {
			conv.setIndexSessionKeys(0x100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv.setIndexSessionKeys(0L);
		} catch (IndexOutOfBoundsException ex) {
			assertTrue(false);
		}

		try {
			conv.setIndexSessionKeys(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// indexMessages
		try {
			conv.setIndexMessages(0x100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv.setIndexMessages(0L);
		} catch (IndexOutOfBoundsException ex) {
			assertTrue(false);
		}

		try {
			conv.setIndexMessages(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// indexPrev
		try {
			conv.setIndexPrev(0x100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv.setIndexPrev(0L);
		} catch (IndexOutOfBoundsException ex) {
			assertTrue(false);
		}

		try {
			conv.setIndexPrev(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// indexNext
		try {
			conv.setIndexNext(0x100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			conv.setIndexNext(0L);
		} catch (IndexOutOfBoundsException ex) {
			assertTrue(false);
		}

		try {
			conv.setIndexNext(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() throws StorageFileException, IOException {
		byte flags = 0;

		Conversation conv = Conversation.createConversation() ;
		setData(conv, true);
		conv.saveToFile();
		
		// get the generated data
		byte[] dataEncrypted = Storage.getDatabase().getEntry(conv.getEntryIndex()); 
		
		// chunk length
		assertEquals(dataEncrypted.length, Storage.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		// check the data
		assertEquals(flags, dataPlain[0]);
		assertEquals(phoneNumberResult, Charset.fromAscii8(dataPlain, 1, 32));
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 16), indexSessionKeys);
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 12), indexMessages);
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 8), indexPrev);
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}

	public void testParseData() throws StorageFileException, IOException {
		byte flags = 0;
		
		Conversation conv = Conversation.createConversation();
		long index = conv.getEntryIndex();

		// create plain data
		byte[] dataPlain = new byte[Storage.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Charset.toAscii8(phoneNumber, 32), 0, dataPlain, 1, 32);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexSessionKeys), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 16, 4);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexMessages), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 12, 4);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexPrev), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 8, 4);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexNext), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it and inject it into the file
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());
		Storage.getDatabase().setEntry(index, dataEncrypted);

		// have it parsed
		Conversation.forceClearCache();
		conv = Conversation.getConversation(index);
		
		// check the indices
		checkData(conv, false);
	}
	
	public void testCreateConversation() throws StorageFileException, IOException {
		// check that it takes only one entry
		int countEmpty = Empty.getEmptyEntriesCount();
		for (int i = 0; i < Storage.ALIGN_SIZE / Storage.CHUNK_SIZE * 5; ++i)
		{
			Conversation.createConversation();
			if (countEmpty == 0)
				assertEquals(Storage.ALIGN_SIZE / Storage.CHUNK_SIZE - 1, (countEmpty = Empty.getEmptyEntriesCount()));
			else
				assertEquals(countEmpty - 1, (countEmpty = Empty.getEmptyEntriesCount()));
		}
		
		// check structure
		assertTrue(Common.checkStructure());
	}
}
