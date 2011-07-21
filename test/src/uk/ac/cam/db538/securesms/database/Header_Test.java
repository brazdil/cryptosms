package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import junit.framework.TestCase;

public class Header_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		Common.clearFile();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testConstructor() throws DatabaseFileException, IOException {
		// ASSIGNMENT
		
		// create a header
		Header header = Header.createHeader();
		header.setVersion(12);
		header.setIndexFree(13L);
		header.setIndexConversations(15L);
		header.saveToFile();

		// force it to be re-read from file
		Header.forceClearCache();
		header = Header.getHeader();
		
		assertEquals(header.getVersion(), 12);
		assertEquals(header.getIndexFree(), 13L);
		assertEquals(header.getIndexConversations(), 15L);
	}
	
	public void testIndices() throws DatabaseFileException, IOException {
		// INDICES OUT OF BOUNDS
		Header header = Header.createHeader();
		
		// indexFree
		try {
			header.setIndexFree(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
		
		try {
			header.setIndexFree(-1L);
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

	public void testCreateData() throws DatabaseFileException, IOException {
		long indexFree = 25L;
		long indexConversation = 13L;
		int version = 32;
		
		Header header = Header.createHeader();
		header.setIndexFree(indexFree);
		header.setIndexConversations(indexConversation);
		header.setVersion(version);
		header.saveToFile();
		
		byte[] dataAll = Database.getDatabase().getEntry(0);
		
		// chunk length
		assertEquals(dataAll.length, Database.CHUNK_SIZE);
		
		// plain header
		assertEquals(dataAll[0], (byte) 0x53); // S
		assertEquals(dataAll[1], (byte) 0x4D); // M
		assertEquals(dataAll[2], (byte) 0x53); // S
		assertEquals(dataAll[3], (byte) version); // Version
		
		// decrypt the encoded part
		ByteBuffer buf = ByteBuffer.allocate(Database.CHUNK_SIZE - 4);
		buf.put(dataAll, 4, Database.CHUNK_SIZE - 4);
		byte[] dataPlain = Encryption.decryptSymmetric(buf.array(), Encryption.retreiveEncryptionKey());
		
		// check the indices
		assertEquals(Database.getInt(dataPlain, Database.CHUNK_SIZE - 4 - Encryption.ENCRYPTION_OVERHEAD - 8), indexFree);
		assertEquals(Database.getInt(dataPlain, Database.CHUNK_SIZE - 4 - Encryption.ENCRYPTION_OVERHEAD - 4), indexConversation);
	}

	public void testParseData() throws IOException, DatabaseFileException {
		long indexFree = 25L;
		long indexConversation = 13L;
		int version = 17;
		
		byte[] dataPlain = new byte[Database.CHUNK_SIZE - 4 - Encryption.ENCRYPTION_OVERHEAD];
		System.arraycopy(Database.getBytes(indexFree), 0, dataPlain, Database.CHUNK_SIZE - 4 - Encryption.ENCRYPTION_OVERHEAD - 8, 4);
		System.arraycopy(Database.getBytes(indexConversation), 0, dataPlain, Database.CHUNK_SIZE - 4 - Encryption.ENCRYPTION_OVERHEAD - 4, 4);
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());
		
		byte[] dataAll = new byte[Database.CHUNK_SIZE];
		System.arraycopy(dataEncrypted, 0, dataAll, 4, Database.CHUNK_SIZE - 4);

		// wrong header (SMS)
		dataAll[0] = 0x53; // S
		dataAll[1] = 0x53; // S
		dataAll[2] = 0x53; // S
		try {
			// inject it in the file
			Database.getDatabase().setEntry(0, dataAll);
			Header.forceClearCache();
			Header.getHeader();
			assertTrue(false);
		} catch (DatabaseFileException ex) {
		}

		// fixed, version set
		dataAll[1] = 0x4D; // M
		dataAll[3] = (byte) version;
		Database.getDatabase().setEntry(0, dataAll);
		Header.forceClearCache();
		Header header = null;
		try {
			header = Header.getHeader();
			assertEquals(indexFree, header.getIndexFree());
			assertEquals(indexConversation, header.getIndexConversations());
			assertEquals(version, header.getVersion());
		} catch (DatabaseFileException ex) {
			assertTrue(false);
		}
	}
}
