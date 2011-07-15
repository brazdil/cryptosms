package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import junit.framework.TestCase;

public class SmsHistory_Free_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSmsHistory_Free() {
		SmsHistory_Free free;
		
		// ASSIGNMENT
		
		free = new SmsHistory_Free(15L);
		assertEquals(free.getIndexNext(), 15L);
		
		// INDICES OUT OF BOUNDS
		
		try {
			free = new SmsHistory_Free(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
		
	}

	public void testCreateData() {
		long indexNext = 36L;
		
		SmsHistory_Free free = new SmsHistory_Free(indexNext);
		byte[] dataEncrypted = SmsHistory_Free.createData(free);
		
		// chunk length
		assertEquals(dataEncrypted.length, SmsHistory.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted);
		
		// check the indices
		assertEquals(SmsHistory.getInt(dataPlain, SmsHistory.CHUNK_SIZE - Encryption.ENCRYPTION_OVERHEAD - 4), indexNext);
	}

	public void testParseData() {
		long indexNext = 25L;

		// create plain data
		byte[] dataPlain = new byte[SmsHistory.ENCRYPTED_ENTRY_SIZE];
		System.arraycopy(SmsHistory.getBytes(indexNext), 0, dataPlain, SmsHistory.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain);

		// have it parsed
		SmsHistory_Free free = SmsHistory_Free.parseData(dataEncrypted);
		
		// check the indices
		assertEquals(indexNext, free.getIndexNext());
	}
}
