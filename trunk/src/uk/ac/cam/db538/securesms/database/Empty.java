package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing an empty entry in the secure storage file.
 * 
 * @author David Brazdil
 *
 */
class Empty {
	// FILE FORMAT
	private static final int OFFSET_NEXTINDEX = Database.ENCRYPTED_ENTRY_SIZE - 4;
	
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
	static Empty createEmpty() throws DatabaseFileException, IOException {
		return createEmpty(true);
	}

	/**
	 * Returns an instance of Empty class at the end of the file
	 * @param index		Index in file
	 * @param lock		File lock allow
	 */
	static Empty createEmpty(boolean lockAllow) throws DatabaseFileException, IOException {
		// create a new one at the end of the file
		Empty empty = new Empty(Database.getDatabase().getEntriesCount(), false, lockAllow);
		Header.getHeader(lockAllow).attachEmpty(empty, lockAllow);
		return empty;
	}

	/**
	 * Returns an instance of Empty class with given index in file. Reads it from the file if not cached.
	 * @param index		Index in file
	 */
	static Empty getEmpty(long index) throws DatabaseFileException, IOException {
		return getEmpty(index, true);
	}
	
	/**
	 * Returns an instance of Empty class with given index in file. Reads it from the file if not cached.
	 * @param index		Index in file
	 * @param lock		File lock allow
	 */
	static Empty getEmpty(long index, boolean lockAllow) throws DatabaseFileException, IOException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheEmpty) {
			for (Empty empty: cacheEmpty)
				if (empty.getEntryIndex() == index)
					return empty; 
		}
		
		// create a new one
		return new Empty(index, true, lockAllow);
	}
	
	/**
	 * Creates a new Empty class at the index of an already existing element.
	 * This old element has to make sure that it there are no pointers pointing to it before it asks to be written over.
	 * @param index		Index in the file
	 * @return	
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	static Empty replaceWithEmpty(long index) throws DatabaseFileException, IOException {
		return replaceWithEmpty(index, true);
	}
	
	/**
	 * Creates a new Empty class at the index of an already existing element.
	 * This old element has to make sure that it there are no pointers pointing to it before it asks to be written over.
	 * @param index			Index in the file
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	static Empty replaceWithEmpty(long index, boolean lockAllow) throws DatabaseFileException, IOException {
		Empty empty = new Empty(index, false, lockAllow);
		Header.getHeader(lockAllow).attachEmpty(empty, lockAllow);
		return empty;
	}
	
	/**
	 * Returns an index of a single entry that was removed from the linked list of empty entries and is now available to be replaced by useful data entry.
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	static long getEmptyIndex() throws DatabaseFileException, IOException {
		return getEmptyIndices(1)[0];
	}
	
	/**
	 * Returns an index of a single entry that was removed from the linked list of empty entries and is now available to be replaced by useful data entry.
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	static long getEmptyIndex(boolean lockAllow) throws DatabaseFileException, IOException {
		return getEmptyIndices(1, lockAllow)[0];
	}
	
	/**
	 * Returns an index of several entries that were removed from the linked list of empty entries and are now available to be replaced by useful data entry.
	 * @param count		Number of entries requested
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	static long[] getEmptyIndices(int count) throws DatabaseFileException, IOException {
		return getEmptyIndices(count, true);
	}
	
	/**
	 * Returns an index of several entries that were removed from the linked list of empty entries and are now available to be replaced by useful data entry.
	 * @param count			Number of entries requested
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	static long[] getEmptyIndices(int count, boolean lockAllow) throws DatabaseFileException, IOException {
		Database db = Database.getDatabase();
		long[] indices = new long[count];
		
		db.lockFile(lockAllow);
		try {
			Header header = Header.getHeader();
			for (int i = 0; i < count; ++i) {
				Empty empty;
				while ((empty = header.getFirstEmpty()) == null) {
					// there are no free entries left
					// => add some
					addEmptyEntries(Database.ALIGN_SIZE / Database.CHUNK_SIZE, false);
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
			header.saveToFile(false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			db.unlockFile(lockAllow);
		}
		return indices;
	}
	
	/**
	 * Appends new empty entries to the storage file
	 * @param count		Number of entries requested
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	static void addEmptyEntries(int count) throws IOException, DatabaseFileException {
		addEmptyEntries(count, true);
	}

	/**
	 * Appends new empty entries to the storage file
	 * @param count		Number of entries requested
	 * @param lockAllow		Allow the file to be locked
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	static void addEmptyEntries(int count, boolean lockAllow) throws IOException, DatabaseFileException {
		Database db = Database.getDatabase();
		db.lockFile(lockAllow);
		try {
			for (int i = 0; i < count; ++i) {
				// create the empty entry
				Empty.createEmpty(false);
			}
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			db.unlockFile(lockAllow);
		}
	}

	/**
	 * Count the number of empty entries available
	 * NOTE: Will cache all of them! It is intended to be used only by the testing classes.
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	static int getEmptyEntriesCount() throws DatabaseFileException, IOException {
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
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	private Empty(long index, boolean readFromFile) throws DatabaseFileException, IOException {
		this(index, readFromFile, true);
	}
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @param lockAllow		Allow the file to be locked
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	private Empty(long index, boolean readFromFile, boolean lockAllow) throws DatabaseFileException, IOException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Database.getDatabase().getEntry(index, lockAllow);
			byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
			setIndexNext(Database.getInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			setIndexNext(0L);
			
			saveToFile(lockAllow);
		}

		synchronized (cacheEmpty) {
			cacheEmpty.add(this);
		}
	}

	// FUNCTIONS
	
	/**
	 * Saves contents of the class to the storage file
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void saveToFile() throws DatabaseFileException, IOException {
		saveToFile(true);
	}
	
	/**
	 * Saves contents of the class to the storage file
	 * @param lockAllow		Allow the file to be locked
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void saveToFile(boolean lockAllow) throws DatabaseFileException, IOException {
		ByteBuffer entryBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		entryBuffer.put(Encryption.generateRandomData(OFFSET_NEXTINDEX));
		entryBuffer.put(Database.getBytes(this.mIndexNext));
		byte[] dataEncrypted = Encryption.encryptSymmetric(entryBuffer.array(), Encryption.retreiveEncryptionKey());
		Database.getDatabase().setEntry(mEntryIndex, dataEncrypted, lockAllow);
	}

	/**
	 * Return an instance of the next Empty entry in the linked list, or null if there isn't any.
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	Empty getNextEmpty() throws DatabaseFileException, IOException {
		return getNextEmpty(true);
	}

	/**
	 * Return an instance of the next Empty entry in the linked list, or null if there isn't any.
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	Empty getNextEmpty(boolean lockAllow) throws DatabaseFileException, IOException {
		return Empty.getEmpty(mIndexNext, lockAllow);
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
