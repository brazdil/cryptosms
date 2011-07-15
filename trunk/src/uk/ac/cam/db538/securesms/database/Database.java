package uk.ac.cam.db538.securesms.database;

import java.io.*;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.content.Context;

public final class Database {
	
	private static final String FILE_NAME = "data.db";
	
	static final int CHUNK_SIZE = 256;
	static final int ALIGN_SIZE = 256 * 32; // 8KB
	static final int ENCRYPTED_ENTRY_SIZE = CHUNK_SIZE - Encryption.ENCRYPTION_OVERHEAD;
	
	// SINGLETON STUFF
	
	private static Database mSingleton = null;
	
	public static Database getSingleton(Context context) throws IOException, DatabaseFileException {
		String filename = context.getFilesDir().getAbsolutePath() + "/" + FILE_NAME;
		return getSingleton(filename);
	}
	
	public static Database getSingleton(String filename) throws IOException, DatabaseFileException {
		if (mSingleton == null)
			mSingleton = new Database(filename);
		return mSingleton;
	}
	
	// LOW-LEVEL BIT MANIPULATION
	
	static long getInt(byte[] data) {
		return getInt(data, 0);
	}

	static long getInt(byte[] data, int offset) {
		if (offset > data.length - 4)
			throw new IndexOutOfBoundsException();

		long result = data[offset] & 0xFF;
		result <<= 8;
		result |= (data[offset + 1] & 0xFF);
		result <<= 8;
		result |= (data[offset + 2] & 0xFF);
		result <<= 8;
		result |= data[offset + 3] & 0xFF;
		return result;
	}
	
	static byte[] getBytes(long integer) {
		byte[] result = new byte[4];
		result[0] = (byte) ((integer >> 24) & 0xFF);
		result[1] = (byte) ((integer >> 16) & 0xFF);
		result[2] = (byte) ((integer >> 8) & 0xFF);
		result[3] = (byte) (integer & 0xFF);
		return result;
	}
	
	// FILE MANIPULATION

	private DatabaseFile smsFile;
	
	private Database(String filename) throws IOException, DatabaseFileException {
		boolean exists = new File(filename).exists();
		smsFile = new DatabaseFile(filename);
		if (!exists)
			createFile();
	}
	
	private void createFile() throws FileNotFoundException, IOException, DatabaseFileException {
		int countFreeEntries = ALIGN_SIZE / CHUNK_SIZE - 1;
		byte[] headerEncoded = FileEntryHeader.createData(new FileEntryHeader(0, 0));
		
		smsFile.lock();
		try {
			setEntry(0, headerEncoded, false);
			addEmptyEntries(countFreeEntries, false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			smsFile.unlock();
		}
	}
	
	private byte[] getEntry(long index) throws DatabaseFileException, IOException {
		return getEntry(index, true);
	}
	
	private byte[] getEntry(long index, boolean lock) throws DatabaseFileException, IOException {
		long offset = index * CHUNK_SIZE;
		if (offset > smsFile.mFile.length() - CHUNK_SIZE)
			throw new DatabaseFileException("Index in history file out of bounds");
		
		if (lock) smsFile.lock();
		byte[] data = new byte[CHUNK_SIZE];
		try {
			smsFile.mFile.seek(offset);
			smsFile.mFile.read(data);
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			if (lock) smsFile.unlock();
		}

		return data;
	}
	
	private void setEntry(long index, byte[] data) throws DatabaseFileException, IOException {
		setEntry(index, data, true);
	}
	
	private void setEntry(long index, byte[] data, boolean lock) throws DatabaseFileException, IOException {
		long offset = index * CHUNK_SIZE;
		long fileSize = smsFile.mFile.length();
		if (offset > fileSize)
			throw new DatabaseFileException("Index in history file out of bounds");

		if (lock) smsFile.lock();
		try {
			smsFile.mFile.seek(offset);
			smsFile.mFile.write(data);
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			if (lock) smsFile.unlock();
		}
	}
	
	private FileEntryHeader getHeader() throws DatabaseFileException, IOException {
		return getHeader(true);
	}

	private FileEntryHeader getHeader(boolean lock) throws DatabaseFileException, IOException {
		return FileEntryHeader.parseData(getEntry(0, lock));
	}
	
	private long getEmptyEntry() throws DatabaseFileException, IOException { 
		return getEmptyEntry(true);
	}
	
	private long getEmptyEntry(boolean lock) throws DatabaseFileException, IOException {
		if (lock) smsFile.lock();
		long result = 0;
		try {
			FileEntryHeader header = getHeader(false);
			long previousFree = header.getIndexFree();
			if (previousFree == 0) {
				// there are no free entries left
				// add some
				addEmptyEntries(ALIGN_SIZE / CHUNK_SIZE, false);
				// recursively get a free entry
				result = getEmptyEntry(false);
			}
			else {
				// remove the entry from stack
				FileEntryEmpty free = FileEntryEmpty.parseData(getEntry(previousFree, false));
				header.setIndexFree(free.getIndexNext());
				// update header
				setEntry(0, FileEntryHeader.createData(header), false);
				// return the index of the freed entry
				result = previousFree;
			}
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			if (lock) smsFile.unlock();
		}
		return result;
	}
	
	private void addEmptyEntries(int count, boolean lock) throws IOException, DatabaseFileException {
		if (lock) smsFile.lock();
		try {
			FileEntryHeader header = getHeader(false);
			long previousFree = header.getIndexFree();
			long countEntries = smsFile.mFile.length() / CHUNK_SIZE;
			byte[][] entriesEncoded = new byte[count][];
			for (int i = 0; i < count; ++i) {
				entriesEncoded[i] = FileEntryEmpty.createData(new FileEntryEmpty(previousFree));
				previousFree = countEntries + i;
			}
			header.setIndexFree(previousFree);
			byte[] headerEncoded = FileEntryHeader.createData(header);
			
			setEntry(0, headerEncoded, false);
			for (int i = 0; i < count; ++i)
				setEntry(countEntries + i, entriesEncoded[i], false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			if (lock) smsFile.unlock();
		}
	}
	
	// HIGH-LEVEL STUFF

	public int getFileVersion() throws DatabaseFileException, IOException {
		return getHeader().getVersion();
	}

	public void addEmptyEntries(int count) throws IOException, DatabaseFileException { 
		addEmptyEntries(count, true);
	}
	
	public void createConversation() {
		
	}

	// FOR TESTING PURPOSES

	int getEmptyEntriesCount() throws DatabaseFileException, IOException {
		return getEmptyEntriesCount(true);
	}

	int getEmptyEntriesCount(boolean lock) throws DatabaseFileException, IOException {
		int count = 0;

		if (lock) smsFile.lock();
		try {
			FileEntryHeader header = getHeader(false);
			FileEntryEmpty free;
			long indexNext = header.getIndexFree();
			
			while (indexNext != 0) {
				free = FileEntryEmpty.parseData(getEntry(indexNext, false));
				++count;
				indexNext = free.getIndexNext();
			}
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			if (lock) smsFile.unlock();
		}
		
		return count;
	}
	
	boolean checkStructure() throws IOException, DatabaseFileException {
		return checkStructure(true);
	}
	
	boolean checkStructure(boolean lock) throws IOException, DatabaseFileException {
		boolean visitedAll = true;

		if (lock) smsFile.lock();
		try {
			if (smsFile.mFile.length() % CHUNK_SIZE != 0)
				throw new DatabaseFileException("File is not aligned to " + CHUNK_SIZE + "-byte chunks!");
			
			int countEntries = (int) smsFile.mFile.length() / CHUNK_SIZE;
			boolean[] visitedEntries = new boolean[countEntries];
			for (int i = 0; i < countEntries; ++i)
				visitedEntries[i] = false;
			
			// get the header (entry 0)
			FileEntryHeader header = getHeader(false);
			visitedEntries[0] = true;
			
			// go through all the free entries
			FileEntryEmpty free;
			long indexNext = header.getIndexFree();
			while (indexNext != 0) {
				free = FileEntryEmpty.parseData(getEntry(indexNext, false));
				visitedEntries[(int) indexNext] = true;
				indexNext = free.getIndexNext();
			}
			
			// now check that all have been visited
			for (int i = 0; i < countEntries; ++i)
				visitedAll &= visitedEntries[i];
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			if (lock) smsFile.unlock();
		}
		
		return visitedAll;
	}
	
	static void freeSingleton() {
		mSingleton = null;
	}
}
