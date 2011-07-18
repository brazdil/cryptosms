package uk.ac.cam.db538.securesms.database;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import junit.framework.TestCase;

public class FileEntryEmpty_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testConstructor() {
		FileEntryEmpty free;
		
		// ASSIGNMENT
		
		free = new FileEntryEmpty(15L);
		assertEquals(free.getIndexNext(), 15L);
		
		// INDICES OUT OF BOUNDS
		
		try {
			free = new FileEntryEmpty(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			free = new FileEntryEmpty(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() {
		long indexNext = 36L;
		
		FileEntryEmpty free = new FileEntryEmpty(indexNext);
		byte[] dataEncrypted = FileEntryEmpty.createData(free);
		
		// chunk length
		assertEquals(dataEncrypted.length, Database.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		// check the indices
		assertEquals(Database.getInt(dataPlain, Database.CHUNK_SIZE - Encryption.ENCRYPTION_OVERHEAD - 4), indexNext);
	}

	public void testParseData() {
		long indexNext = 25L;

		// create plain data
		byte[] dataPlain = new byte[Database.ENCRYPTED_ENTRY_SIZE];
		System.arraycopy(Database.getBytes(indexNext), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());

		// have it parsed
		FileEntryEmpty free = FileEntryEmpty.parseData(dataEncrypted);
		
		// check the indices
		assertEquals(indexNext, free.getIndexNext());
	}
}
