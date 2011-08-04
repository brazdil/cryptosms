package uk.ac.cam.db538.securesms.storage;

import java.io.IOException;

import uk.ac.cam.db538.securesms.Encryption;
import uk.ac.cam.db538.securesms.data.LowLevel;
import uk.ac.cam.db538.securesms.storage.Storage;
import uk.ac.cam.db538.securesms.storage.StorageFileException;
import uk.ac.cam.db538.securesms.storage.Empty;
import junit.framework.TestCase;

public class Empty_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		Common.clearStorageFile();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testConstruction() throws StorageFileException, IOException {
		// create a free entry
		Empty free = Empty.createEmpty();

		assertTrue(Common.checkStructure());
		
		free.setIndexNext(15L);
		free.saveToFile();
		long index = free.getEntryIndex();
		
		// force it to be re-read from file
		Empty.forceClearCache();
		free = Empty.getEmpty(index);
		
		assertEquals(free.getIndexNext(), 15L);
	}
	
	public void testIndices() throws StorageFileException, IOException {
		// INDICES OUT OF BOUNDS
		
		Empty free = Empty.createEmpty() ;
		try {
			free.setIndexNext(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			free.setIndexNext(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() throws StorageFileException, IOException {
		long indexNext = 36L;
		
		Empty free = Empty.createEmpty() ;
		free.setIndexNext(indexNext);
		free.saveToFile();

		byte[] dataEncrypted = Storage.getDatabase().getEntry(free.getEntryIndex());
		
		// chunk length
		assertEquals(dataEncrypted.length, Storage.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		// check the indices
		assertEquals(LowLevel.getUnsignedInt(dataPlain, Storage.CHUNK_SIZE - Encryption.ENCRYPTION_OVERHEAD - 4), indexNext);
	}

	public void testParseData() throws StorageFileException, IOException {
		long indexNext = 25L;
		
		Empty free = Empty.createEmpty();
		long index = free.getEntryIndex();
		
		// create plain data
		byte[] dataPlain = new byte[Storage.ENCRYPTED_ENTRY_SIZE];
		System.arraycopy(LowLevel.getBytesUnsignedInt(indexNext), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it and insert it in the file
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());
		Storage.getDatabase().setEntry(index, dataEncrypted);
		
		// have it parsed
		Empty.forceClearCache();
		free = Empty.getEmpty(index);
		
		// check the indices
		assertEquals(indexNext, free.getIndexNext());
	}

	public void testAddEmptyEntries() {
		try {
/*			// tests whether the number of added free entries fits
			int countFree = Empty.getEmptyEntriesCount();
			Empty.addEmptyEntries(10);
			assertEquals(countFree + 10, Empty.getEmptyEntriesCount());*/

			// check structure
			assertTrue(Common.checkStructure());
		} catch (StorageFileException e) {
			assertTrue(e.getMessage(), false);
		} catch (IOException e) {
			assertTrue(e.getMessage(), false);
		}
	}
}
