package uk.ac.cam.db538.cryptosms.storage;

import java.io.IOException;
import java.nio.ByteBuffer;

import uk.ac.cam.db538.cryptosms.storage.Header;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import junit.framework.TestCase;

public class Header_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		Common.clearStorageFile();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testConstruction() throws StorageFileException, IOException {
		// create a header
		Header header = Header.createHeader();
		header.setVersion(12);
		header.setIndexEmpty(13L);
		header.setIndexConversations(15L);
		header.saveToFile();

		// force it to be re-read from file
		Header.forceClearCache();
		header = Header.getHeader();
		
		assertEquals(header.getVersion(), 12);
		assertEquals(header.getIndexEmpty(), 13L);
		assertEquals(header.getIndexConversations(), 15L);
	}
	
	public void testIndices() throws StorageFileException, IOException {
		// INDICES OUT OF BOUNDS
		Header header = Header.createHeader();
		
		// indexFree
		try {
			header.setIndexEmpty(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
		
		try {
			header.setIndexEmpty(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// indexConversation
		try {
			header.setIndexConversations(0x0100000000L); 
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			header.setIndexConversations(-1L); 
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		// version
		try {
			header.setVersion(0x100);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() throws StorageFileException, IOException, EncryptionException {
		long indexFree = 25L;
		long indexConversation = 13L;
		int version = 32;
		
		Header header = Header.createHeader();
		header.setIndexEmpty(indexFree);
		header.setIndexConversations(indexConversation);
		header.setVersion(version);
		header.saveToFile();
		
		byte[] dataAll = Storage.getDatabase().getEntry(0);
		
		// chunk length
		assertEquals(dataAll.length, Storage.CHUNK_SIZE);
		
		// plain header
		assertEquals(dataAll[0], (byte) 0x53); // S
		assertEquals(dataAll[1], (byte) 0x4D); // M
		assertEquals(dataAll[2], (byte) 0x53); // S
		assertEquals(dataAll[3], (byte) version); // Version
		
		// decrypt the encoded part
		ByteBuffer buf = ByteBuffer.allocate(Storage.CHUNK_SIZE - 4);
		buf.put(dataAll, 4, Storage.CHUNK_SIZE - 4);
		byte[] dataPlain = Encryption.getEncryption().decryptSymmetricWithMasterKey(buf.array());
		
		// check the indices
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.CHUNK_SIZE - 4 - Encryption.ENCRYPTION_OVERHEAD - 8), indexFree);
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.CHUNK_SIZE - 4 - Encryption.ENCRYPTION_OVERHEAD - 4), indexConversation);
	}

	public void testParseData() throws IOException, StorageFileException, EncryptionException {
		long indexFree = 25L;
		long indexConversation = 13L;
		int version = 17;
		
		byte[] dataPlain = new byte[Storage.CHUNK_SIZE - 4 - Encryption.ENCRYPTION_OVERHEAD];
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexFree), 0, dataPlain, Storage.CHUNK_SIZE - 4 - Encryption.ENCRYPTION_OVERHEAD - 8, 4);
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexConversation), 0, dataPlain, Storage.CHUNK_SIZE - 4 - Encryption.ENCRYPTION_OVERHEAD - 4, 4);
		byte[] dataEncrypted = Encryption.getEncryption().encryptSymmetricWithMasterKey(dataPlain);
		
		byte[] dataAll = new byte[Storage.CHUNK_SIZE];
		System.arraycopy(dataEncrypted, 0, dataAll, 4, Storage.CHUNK_SIZE - 4);

		// wrong header (SMS)
		dataAll[0] = 0x53; // S
		dataAll[1] = 0x53; // S
		dataAll[2] = 0x53; // S
		try {
			// inject it in the file
			Storage.getDatabase().setEntry(0, dataAll);
			Header.forceClearCache();
			Header.getHeader();
			assertTrue(false);
		} catch (StorageFileException ex) {
		}

		// fixed, version set
		dataAll[1] = 0x4D; // M
		dataAll[3] = (byte) version;
		Storage.getDatabase().setEntry(0, dataAll);
		Header.forceClearCache();
		Header header = null;
		try {
			header = Header.getHeader();
			assertEquals(indexFree, header.getIndexEmpty());
			assertEquals(indexConversation, header.getIndexConversations());
			assertEquals(version, header.getVersion());
		} catch (StorageFileException ex) {
			assertTrue(false);
		}
	}
}
