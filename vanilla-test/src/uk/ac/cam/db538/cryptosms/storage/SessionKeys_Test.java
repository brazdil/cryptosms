package uk.ac.cam.db538.cryptosms.storage;

import java.io.IOException;

import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.utils.Charset;
import uk.ac.cam.db538.cryptosms.utils.SimNumber;
import uk.ac.cam.db538.cryptosms.CustomAsserts;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionNone;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import junit.framework.TestCase;

public class SessionKeys_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		EncryptionNone.initEncryption();
		Common.clearStorageFile();
		
		sessionKey_Out = Encryption.getEncryption().generateRandomData(Encryption.SYM_KEY_LENGTH);
		sessionKey_In = Encryption.getEncryption().generateRandomData(Encryption.SYM_KEY_LENGTH);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		Common.closeStorageFile();
	}

	private boolean keysSent = true;
	private boolean keysConfirmed = true;
	private boolean simSerial = true;
	private String simNumber = "+123456789012";
	private byte[] sessionKey_Out = null;
	private byte lastID_Out = 0x12;
	private byte[] sessionKey_In = null;
	private byte lastID_In = 0x18;
	private long indexParent = 246L;
	private long indexPrev = 247L;
	private long indexNext = 248L;
	private String simNumberLong = "+1234567890126549873sdfsat6ewrt987wet3df1g3s2g1e6r5t46wert4dfsgdfsg";
	private String simNumberResult = "+1234567890126549873sdfsat6ewrt9";
	private byte flags = (byte) 0xE0;

	private void setData(SessionKeys keys, boolean longer) {
		keys.setKeysSent(keysSent);
		keys.setKeysConfirmed(keysConfirmed);
		keys.setSimNumber(new SimNumber((longer) ? simNumberLong : simNumber, simSerial));
		keys.setSessionKey_Out(sessionKey_Out);
		keys.setNextID_Out(lastID_Out);
		keys.setSessionKey_In(sessionKey_In);
		keys.setLastID_In(lastID_In);
		keys.setIndexParent(indexParent);
		keys.setIndexPrev(indexPrev);
		keys.setIndexNext(indexNext);
	}
	
	private void checkData(SessionKeys keys, boolean longer) {
		assertEquals(keysSent, keys.getKeysSent());
		assertEquals(keysConfirmed, keys.getKeysConfirmed());
		assertEquals(simSerial, keys.getSimNumber().isSerial());
		assertEquals((longer) ? simNumberResult : simNumber, keys.getSimNumber().getNumber());
		CustomAsserts.assertArrayEquals(keys.getSessionKey_Out(), sessionKey_Out);
		assertEquals(lastID_Out, keys.getNextID_Out());
		CustomAsserts.assertArrayEquals(keys.getSessionKey_In(), sessionKey_In);
		assertEquals(lastID_In, keys.getLastID_In());
		assertEquals(indexParent, keys.getIndexParent());
		assertEquals(indexPrev, keys.getIndexPrev());
		assertEquals(indexNext, keys.getIndexNext());
	}
	
	public void testConstruction() throws StorageFileException, IOException {
		Conversation conv = Conversation.createConversation();
		SessionKeys keys = SessionKeys.createSessionKeys(conv);
		
		assertTrue(Common.checkStructure());

		setData(keys, false);
		keys.saveToFile();
		long index = keys.getEntryIndex();

		// force to be re-read
		SessionKeys.forceClearCache();
		keys = SessionKeys.getSessionKeys(index);
		
		checkData(keys, false);
	}
			
	public void testIndices() throws StorageFileException, IOException {
		// INDICES OUT OF BOUNDS
		Conversation conv = Conversation.createConversation();
		SessionKeys keys = SessionKeys.createSessionKeys(conv);
		// indexNext
		try {
			keys.setIndexNext(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			keys.setIndexNext(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() throws StorageFileException, IOException, EncryptionException {
		Conversation conv = Conversation.createConversation();
		SessionKeys keys = SessionKeys.createSessionKeys(conv);
		setData(keys, true);
		keys.saveToFile();
		
		// get the generated data
		byte[] dataEncrypted = Storage.getStorage().getEntry(keys.getEntryIndex());
		
		// chunk length
		assertEquals(dataEncrypted.length, Storage.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.getEncryption().decryptSymmetricWithMasterKey(dataEncrypted);
		
		// check the data
		assertEquals(flags, dataPlain[0]);
		assertEquals(Charset.fromAscii8(dataPlain, 1, 32), simNumberResult);
		CustomAsserts.assertArrayEquals(dataPlain, 33, sessionKey_Out, 0, 32);
		assertEquals(lastID_Out, dataPlain[65]);
		CustomAsserts.assertArrayEquals(dataPlain, 66, sessionKey_In, 0, 32);
		assertEquals(lastID_In, dataPlain[98]);
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 12), indexParent);
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 8), indexPrev);		
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}

	public void testParseData() throws StorageFileException, IOException, EncryptionException {
		Conversation conv = Conversation.createConversation();
		SessionKeys keys = SessionKeys.createSessionKeys(conv);
		long index = keys.getEntryIndex();
		
		// create plain data
		byte[] dataPlain = new byte[Storage.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Charset.toAscii8(simNumber, 32), 0, dataPlain, 1, 32);
		System.arraycopy(sessionKey_Out, 0, dataPlain, 33, 32);
		dataPlain[65] = lastID_Out;
		System.arraycopy(sessionKey_In, 0, dataPlain, 66, 32);
		dataPlain[98] = lastID_In;
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexParent), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 12, 4);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexPrev), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 8, 4);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexNext), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.getEncryption().encryptSymmetricWithMasterKey(dataPlain);

		// inject it into the file
		Storage.getStorage().setEntry(index, dataEncrypted);
		
		// have it parsed
		SessionKeys.forceClearCache();
		keys = SessionKeys.getSessionKeys(index);
		
		// check the indices
		checkData(keys, false);
	}
}
