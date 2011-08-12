package uk.ac.cam.db538.cryptosms.storage;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

/**
 * 
 * Class representing an empty entry in the secure storage file.
 * 
 * @author David Brazdil
 *
 */
class Empty {
	// FILE FORMAT
	private static final int OFFSET_NEXTINDEX = Storage.ENCRYPTED_ENTRY_SIZE - 4;
	
	// STATIC
	
	private static ArrayList<Empty> cacheEmpty = new ArrayList<Empty>();

	/**
	 * Removes all instances from the list of cached objects.
	 * Be sure you don't use the instances afterwards.
	 */
	public static void forceClearCache() {
		synchronized (cacheEmpty) {
			cacheEmpty = new ArrayList<Empty>();
		}
	}
	
	/**
	 * Returns an instance of Empty class at the end of the file
	 * @param index		Index in file
	 */
	static Empty createEmpty() throws StorageFileException {
		// create a new one at the end of the file
		Empty empty = new Empty(Storage.getStorage().getEntriesCount(), false);
		Header.getHeader().attachEmpty(empty);
		return empty;
	}

	/**
	 * Returns an instance of Empty class with given index in file. Reads it from the file if not cached.
	 * @param index		Index in file
	 */
	static Empty getEmpty(long index) throws StorageFileException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheEmpty) {
			for (Empty empty: cacheEmpty)
				if (empty.getEntryIndex() == index)
					return empty; 
		}
		
		// create a new one
		return new Empty(index, true);
	}
	
	/**
	 * Creates a new Empty class at the index of an already existing element.
	 * This old element has to make sure that it there are no pointers pointing to it before it asks to be written over.
	 * @param index		Index in the file
	 * @return	
	 * @throws StorageFileException
	 */
	static Empty replaceWithEmpty(long index) throws StorageFileException {
		Empty empty = new Empty(index, false);
		Header.getHeader().attachEmpty(empty);
		return empty;
	}
	
	/**
	 * Returns an index of a single entry that was removed from the linked list of empty entries and is now available to be replaced by useful data entry.
	 * @return
	 * @throws StorageFileException
	 */
	static long getEmptyIndex() throws StorageFileException {
		return getEmptyIndices(1)[0];
	}
	
	/**
	 * Returns an index of several entries that were removed from the linked list of empty entries and are now available to be replaced by useful data entry.
	 * @param count		Number of entries requested
	 * @return
	 * @throws StorageFileException
	 */
	static long[] getEmptyIndices(int count) throws StorageFileException {
		long[] indices = new long[count];

		Header header = Header.getHeader();
		for (int i = 0; i < count; ++i) {
			Empty empty;
			while ((empty = header.getFirstEmpty()) == null) {
				// there are no free entries left
				// => add some
				addEmptyEntries(Storage.ALIGN_SIZE / Storage.CHUNK_SIZE);
			}
			// remove the entry from stack
			header.setIndexEmpty(empty.getIndexNext());
			// remove from cache
			synchronized (cacheEmpty) {				
				cacheEmpty.remove(empty);
			}
			// return the index of the freed entry
			indices[i] = empty.getEntryIndex();
		}
		// save header
		header.saveToFile();
		
		return indices;
	}
	
	/**
	 * Appends new empty entries to the storage file
	 * @param count		Number of entries requested
	 * @throws StorageFileException
	 */
	static void addEmptyEntries(int count) throws StorageFileException {
		for (int i = 0; i < count; ++i) {
			// create the empty entry
			Empty.createEmpty();
		}
	}

	/**
	 * Count the number of empty entries available
	 * NOTE: Will cache all of them! It is intended to be used only by the testing classes.
	 * @return
	 * @throws StorageFileException
	 */
	static int getEmptyEntriesCount() throws StorageFileException {
		int count = 0;
		
		Empty free = Header.getHeader().getFirstEmpty();
		while (free != null) {
			++count;
			free = free.getNextEmpty();
		}
		
		return count;
	}	
	
	// INTERNAL FIELDS
	private long mEntryIndex; // READ ONLY
	private long mIndexNext;
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @throws StorageFileException
	 */
	private Empty(long index, boolean readFromFile) throws StorageFileException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Storage.getStorage().getEntry(index);
			byte[] dataPlain;
			try {
				dataPlain = Encryption.getEncryption().decryptSymmetricWithMasterKey(dataEncrypted);
			} catch (EncryptionException e) {
				throw new StorageFileException(e);
			}
			setIndexNext(LowLevel.getUnsignedInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			setIndexNext(0L);
			
			saveToFile();
		}

		synchronized (cacheEmpty) {
			cacheEmpty.add(this);
		}
	}

	// FUNCTIONS
	
	/**
	 * Saves contents of the class to the storage file
	 * @throws StorageFileException
	 */
	public void saveToFile() throws StorageFileException {
		ByteBuffer entryBuffer = ByteBuffer.allocate(Storage.ENCRYPTED_ENTRY_SIZE);
		entryBuffer.put(Encryption.getEncryption().generateRandomData(OFFSET_NEXTINDEX));
		entryBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexNext));
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = Encryption.getEncryption().encryptSymmetricWithMasterKey(entryBuffer.array());
		} catch (EncryptionException e) {
			throw new StorageFileException(e);
		}
		Storage.getStorage().setEntry(mEntryIndex, dataEncrypted);
	}

	/**
	 * Return an instance of the next Empty entry in the linked list, or null if there isn't any.
	 * @return
	 * @throws StorageFileException
	 */
	Empty getNextEmpty() throws StorageFileException {
		return Empty.getEmpty(mIndexNext);
	}

	// GETTERS / SETTERS
	
	long getEntryIndex() {
		return mEntryIndex;
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
