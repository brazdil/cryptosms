package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.text.format.Time;
import junit.framework.TestCase;

public class Conversation_Test extends TestCase {

	// Testing data
	private String phoneNumber = "+447896512369";
	private String phoneNumberLong = "+1234567890126549873sdfsat6ewrt987wet3df1g3s2g1e6r5t46wert4dfsgdfsg";
	private String phoneNumberResult = "+1234567890126549873sdfsat6ewrt9";
	private Time timeStamp = new Time();
	private long indexSessionKeys = 122L;
	private long indexMessages = 120L;
	private long indexPrev = 12L;
	private long indexNext = 15L;

	private void setData(Conversation conv, boolean longPhoneNumber) {
		conv.setPhoneNumber((longPhoneNumber) ? phoneNumberLong : phoneNumber);
		conv.setTimeStamp(timeStamp);
		conv.setIndexSessionKeys(indexSessionKeys);
		conv.setIndexMessages(indexMessages);
		conv.setIndexPrev(indexPrev);
		conv.setIndexNext(indexNext);
	}
	
	private void checkData(Conversation conv, boolean longPhoneNumber) {
		assertEquals(conv.getPhoneNumber(), (longPhoneNumber) ? phoneNumberResult : phoneNumber);
		assertEquals(Time.compare(timeStamp, conv.getTimeStamp()), 0);
		assertEquals(conv.getIndexSessionKeys(), indexSessionKeys);
		assertEquals(conv.getIndexMessages(), indexMessages);
		assertEquals(conv.getIndexPrev(), indexPrev);
		assertEquals(conv.getIndexNext(), indexNext);
	}

	protected void setUp() throws Exception {
		super.setUp();
		Common.clearFile();
		
		timeStamp.set(12, 24, 5, 8, 2, 1985);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testConstruction() throws DatabaseFileException, IOException {
		Conversation conv = Conversation.createConversation();
		setData(conv, false);
		conv.saveToFile();
		long index = conv.getEntryIndex();
		
		// force it to be re-read from file
		Conversation.forceClearCache();
		conv = Conversation.getConversation(index);
		checkData(conv, false);
	}
	
	public void testIndices() throws DatabaseFileException, IOException {
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

	public void testCreateData() throws DatabaseFileException, IOException {
		byte flags = 0;

		Conversation conv = Conversation.createConversation() ;
		setData(conv, true);
		conv.saveToFile();
		
		// get the generated data
		byte[] dataEncrypted = Database.getDatabase().getEntry(conv.getEntryIndex()); 
		
		// chunk length
		assertEquals(dataEncrypted.length, Database.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		// check the data
		assertEquals(flags, dataPlain[0]);
		assertEquals(phoneNumberResult, Database.fromLatin(dataPlain, 1, 32));
		Time time = new Time(); time.parse3339(Database.fromLatin(dataPlain, 33, 29));
		assertEquals(Time.compare(time, timeStamp), 0);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 16), indexSessionKeys);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 12), indexMessages);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 8), indexPrev);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}

	public void testParseData() throws DatabaseFileException, IOException {
		byte flags = 0;
		
		Conversation conv = Conversation.createConversation();
		long index = conv.getEntryIndex();

		// create plain data
		byte[] dataPlain = new byte[Database.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Database.toLatin(phoneNumber, 32), 0, dataPlain, 1, 32);
		System.arraycopy(Database.toLatin(timeStamp.format3339(false), 29), 0, dataPlain, 33, 29);
		System.arraycopy(Database.getBytes(indexSessionKeys), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 16, 4);
		System.arraycopy(Database.getBytes(indexMessages), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 12, 4);
		System.arraycopy(Database.getBytes(indexPrev), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 8, 4);
		System.arraycopy(Database.getBytes(indexNext), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it and inject it into the file
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());
		Database.getDatabase().setEntry(index, dataEncrypted);

		// have it parsed
		Conversation.forceClearCache();
		conv = Conversation.getConversation(index);
		
		// check the indices
		checkData(conv, false);
	}
	
	public void testCreateConversation() throws DatabaseFileException, IOException {
		// check that it takes only one entry
		Header header = Header.getHeader();
		int countEmpty = Empty.getEmptyEntriesCount();
		for (int i = 0; i < Database.ALIGN_SIZE / Database.CHUNK_SIZE * 5; ++i)
		{
			header.attachConversation(Conversation.createConversation());
			if (countEmpty == 0)
				assertEquals(Database.ALIGN_SIZE / Database.CHUNK_SIZE - 1, (countEmpty = Empty.getEmptyEntriesCount()));
			else
				assertEquals(countEmpty - 1, (countEmpty = Empty.getEmptyEntriesCount()));
		}
		
		// check structure
		assertTrue(Common.checkStructure());
	}
}
