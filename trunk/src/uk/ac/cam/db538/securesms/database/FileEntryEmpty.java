package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing an empty entry in the secure storage file.
 * Not to be used outside the package.
 * 
 * @author David Brazdil
 *
 */
class FileEntryEmpty {
	private static final int OFFSET_NEXTINDEX = Database.ENCRYPTED_ENTRY_SIZE - 4;

	private long mIndexNext;
	
	FileEntryEmpty(long indexNext) {
		setIndexNext(indexNext);
	}

	static byte[] createData(FileEntryEmpty free) {
		ByteBuffer entryBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		entryBuffer.put(Encryption.generateRandomData(OFFSET_NEXTINDEX));
		entryBuffer.put(Database.getBytes(free.mIndexNext));
		return Encryption.encryptSymmetric(entryBuffer.array(), Encryption.retreiveEncryptionKey());
	}

	static FileEntryEmpty parseData(byte[] dataEncrypted) {
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		return new FileEntryEmpty(Database.getInt(dataPlain, OFFSET_NEXTINDEX));
	}

	long getIndexNext() {
		return mIndexNext;
	}

	void setIndexNext(long indexNext) {
		if (indexNext > 0xFFFFFFFFL || indexNext < 0L) 
			throw new IndexOutOfBoundsException();

		this.mIndexNext = indexNext;
	}
}
