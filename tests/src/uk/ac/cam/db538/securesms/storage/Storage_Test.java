package uk.ac.cam.db538.securesms.storage;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import uk.ac.cam.db538.securesms.storage.Storage;
import uk.ac.cam.db538.securesms.storage.StorageFileException;

public class Storage_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();

		// delete the file before each test
		Common.clearStorageFile();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testCreateFile() {
		try {
			// delete the file
			File file = new File(Common.TESTING_FILE);
			if (file.exists())
				file.delete();
			
			// and free the singleton
			Storage.freeSingleton();

			// file shouldn't exist now
			assertFalse(new File(Common.TESTING_FILE).exists());
			
			// should be created during the initialisation
			Storage.initSingleton(Common.TESTING_FILE);
			// then it should exist
			assertTrue(new File(Common.TESTING_FILE).exists());

			// now we can get the singleton
			Storage.getDatabase();
			
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
