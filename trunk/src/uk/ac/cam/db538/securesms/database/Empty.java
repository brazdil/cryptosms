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
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 */
	static Empty getEmpty(long index) throws DatabaseFileException, IOException {
		return getEmpty(index, true);
	}
	
	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 * @param lock		File lock allow
	 */
	static Empty getEmpty(long index, boolean lockAllow) throws DatabaseFileException, IOException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		for (Empty empty: cacheEmpty)
			if (empty.getEntryIndex() == index)
				return empty; 
		
		// create a new one
		return new Empty(index, true, lockAllow);
	}
	
	static long getEmptyIndex() throws DatabaseFileException, IOException {
		return getEmptyIndices(1)[0];
	}
	
	static long getEmptyIndex(boolean lock) throws DatabaseFileException, IOException {
		return getEmptyIndices(1)[0];
	}
	
	static long[] getEmptyIndices(int count) throws DatabaseFileException, IOException {
		return getEmptyIndices(count, true);
	}
	
	static long[] getEmptyIndices(int count, boolean lock) throws DatabaseFileException, IOException {
		Database db = Database.getDatabase();
		long[] indices = new long[count];
		
		db.lockFile(lock);
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
				header.setIndexFree(empty.getIndexNext());
				// remove from cache
				cacheEmpty.remove(empty);
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
			db.unlockFile(lock);
		}
		return indices;
	}
	
	static void addEmptyEntries(int count, boolean lock) throws IOException, DatabaseFileException {
		Database db = Database.getDatabase();
		db.lockFile(lock);
		try {
			Header header = Header.getHeader(false);
			// get first empty index
			long previousFree = header.getIndexFree();
			// figure out the indices offset of our entries
			long countEntries = db.getEntriesCount();
			Empty empty;
			for (int i = 0; i < count; ++i) {
				// create the empty entry
				empty = Empty.getEmpty(countEntries + i, false);
				// set pointer to the previous head of stack
				empty.setIndexNext(previousFree);
				// dump it into the file
				empty.saveToFile(false);
				// increase index
				previousFree = countEntries + i;
			}
			// update header
			header.setIndexFree(previousFree);
			// save it
			header.saveToFile(false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			db.unlockFile(lock);
		}
	}

	// INTERNAL FIELDS
	private long mEntryIndex; // READ ONLY
	private long mIndexNext;
	
	private Empty(long index, boolean readFromFile) throws DatabaseFileException, IOException {
		this(index, readFromFile, true);
	}
	
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

		cacheEmpty.add(this);
	}

	public void saveToFile() throws DatabaseFileException, IOException {
		saveToFile(true);
	}
	
	public void saveToFile(boolean lock) throws DatabaseFileException, IOException {
		ByteBuffer entryBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		entryBuffer.put(Encryption.generateRandomData(OFFSET_NEXTINDEX));
		entryBuffer.put(Database.getBytes(this.mIndexNext));
		byte[] dataEncrypted = Encryption.encryptSymmetric(entryBuffer.array(), Encryption.retreiveEncryptionKey());
		Database.getDatabase().setEntry(mEntryIndex, dataEncrypted, lock);
	}

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
	
	Empty getNextEmpty() throws DatabaseFileException, IOException {
		return Empty.getEmpty(mIndexNext);
	}
}
