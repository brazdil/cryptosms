package uk.ac.cam.db538.cryptosms.storage;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionNone;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;

public class Storage_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();

		EncryptionNone.initEncryption();
		Common.clearStorageFile();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		Common.closeStorageFile();
	}

	public void testCreateFile() {
		try {
			// and file's size should be aligned as specified
			assertEquals(new File(Common.TESTING_FILE).length(), Storage.ALIGN_SIZE);

			// check structure
			assertTrue(Common.checkStructure());
		} catch (StorageFileException e) {
			assertTrue(e.getMessage(), false);
		} catch (IOException e) {
			assertTrue(e.getMessage(), false);
		}
	}
}
