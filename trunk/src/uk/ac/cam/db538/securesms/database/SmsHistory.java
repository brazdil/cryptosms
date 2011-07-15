package uk.ac.cam.db538.securesms.database;

import java.io.*;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.content.Context;

public final class SmsHistory {
	
	private static final String FILE_NAME = "history.enc";
	
	static final int CHUNK_SIZE = 256;
	static final int ALIGN_SIZE = 256 * 32; // 8KB
	static final int ENCRYPTED_ENTRY_SIZE = CHUNK_SIZE - Encryption.ENCRYPTION_OVERHEAD;
	
	private static SmsHistory mSingleton = null;
	
	public static SmsHistory getSingleton(Context context) throws IOException, HistoryFileException {
		String filename = context.getFilesDir().getAbsolutePath() + "/" + FILE_NAME;
		return getSingleton(filename);
	}
	
	public static SmsHistory getSingleton(String filename) throws IOException, HistoryFileException {
		if (mSingleton == null)
			mSingleton = new SmsHistory(filename);
		return mSingleton;
	}
	
	private SmsHistory_File smsFile;
	
	private SmsHistory(String filename) throws IOException, HistoryFileException {
		boolean exists = new File(filename).exists();
		smsFile = new SmsHistory_File(filename);
		if (!exists)
			createFile();
	}
	
	public int getFileVersion() throws HistoryFileException, IOException {
		return getHeader().getVersion();
	}
	
	public void createFile() throws FileNotFoundException, IOException, HistoryFileException {
		int countFreeEntries = ALIGN_SIZE / CHUNK_SIZE - 1;
		byte[] headerEncoded = SmsHistory_Header.createData(new SmsHistory_Header(0, 0));
		
		smsFile.lock();
		try {
			setEntry(0, headerEncoded, false);
			addFreeEntries(countFreeEntries, false);
		} catch (HistoryFileException ex) {
			throw new HistoryFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			smsFile.unlock();
		}
	}
	
	private byte[] getEntry(long index) throws HistoryFileException, IOException {
		return getEntry(index, true);
	}
	
	private byte[] getEntry(long index, boolean lock) throws HistoryFileException, IOException {
		long offset = index * CHUNK_SIZE;
		if (offset > smsFile.mFile.length() - CHUNK_SIZE)
			throw new HistoryFileException("Index in history file out of bounds");
		
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
	
	private void setEntry(long index, byte[] data) throws HistoryFileException, IOException {
		setEntry(index, data, true);
	}
	
	private void setEntry(long index, byte[] data, boolean lock) throws HistoryFileException, IOException {
		long offset = index * CHUNK_SIZE;
		long fileSize = smsFile.mFile.length();
		if (offset > fileSize)
			throw new HistoryFileException("Index in history file out of bounds");

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
	
	private SmsHistory_Header getHeader() throws HistoryFileException, IOException {
		return getHeader(true);
	}

	private SmsHistory_Header getHeader(boolean lock) throws HistoryFileException, IOException {
		return SmsHistory_Header.parseData(getEntry(0, lock));
	}
	
	private long getFreeEntry() throws HistoryFileException, IOException { 
		return getFreeEntry(true);
	}
	
	private long getFreeEntry(boolean lock) throws HistoryFileException, IOException {
		if (lock) smsFile.lock();
		long result = 0;
		try {
			SmsHistory_Header header = getHeader(false);
			long previousFree = header.getIndexFree();
			if (previousFree == 0) {
				// there are no free entries left
				// add some
				addFreeEntries(ALIGN_SIZE / CHUNK_SIZE, false);
				// recursively get a free entry
				result = getFreeEntry(false);
			}
			else {
				// remove the entry from stack
				SmsHistory_Free free = SmsHistory_Free.parseData(getEntry(previousFree, false));
				header.setIndexFree(free.getIndexNext());
				// update header
				setEntry(0, SmsHistory_Header.createData(header), false);
				// return the index of the freed entry
				result = previousFree;
			}
		} catch (HistoryFileException ex) {
			throw new HistoryFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			if (lock) smsFile.unlock();
		}
		return result;
	}
	
	public void addFreeEntries(int count) throws IOException, HistoryFileException { 
		addFreeEntries(count, true);
	}
	
	public void addFreeEntries(int count, boolean lock) throws IOException, HistoryFileException {
		if (lock) smsFile.lock();
		try {
			SmsHistory_Header header = getHeader(false);
			long previousFree = header.getIndexFree();
			long countEntries = smsFile.mFile.length() / CHUNK_SIZE;
			byte[][] entriesEncoded = new byte[count][];
			for (int i = 0; i < count; ++i) {
				entriesEncoded[i] = SmsHistory_Free.createData(new SmsHistory_Free(previousFree));
				previousFree = countEntries + i;
			}
			header.setIndexFree(previousFree);
			byte[] headerEncoded = SmsHistory_Header.createData(header);
			
			setEntry(0, headerEncoded, false);
			for (int i = 0; i < count; ++i)
				setEntry(countEntries + i, entriesEncoded[i], false);
		} catch (HistoryFileException ex) {
			throw new HistoryFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			if (lock) smsFile.unlock();
		}
	}

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
	
	// FOR TESTING PURPOSES

	int getFreeEntriesCount() throws HistoryFileException, IOException {
		return getFreeEntriesCount(true);
	}

	int getFreeEntriesCount(boolean lock) throws HistoryFileException, IOException {
		int count = 0;

		if (lock) smsFile.lock();
		try {
			SmsHistory_Header header = getHeader(false);
			SmsHistory_Free free;
			long indexNext = header.getIndexFree();
			
			while (indexNext != 0) {
				free = SmsHistory_Free.parseData(getEntry(indexNext, false));
				++count;
				indexNext = free.getIndexNext();
			}
		} catch (HistoryFileException ex) {
			throw new HistoryFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			if (lock) smsFile.unlock();
		}
		
		return count;
	}
	
	boolean checkStructure() throws IOException, HistoryFileException {
		return checkStructure(true);
	}
	
	boolean checkStructure(boolean lock) throws IOException, HistoryFileException {
		boolean visitedAll = true;

		if (lock) smsFile.lock();
		try {
			if (smsFile.mFile.length() % CHUNK_SIZE != 0)
				throw new HistoryFileException("File is not aligned to " + CHUNK_SIZE + "-byte chunks!");
			
			int countEntries = (int) smsFile.mFile.length() / CHUNK_SIZE;
			boolean[] visitedEntries = new boolean[countEntries];
			for (int i = 0; i < countEntries; ++i)
				visitedEntries[i] = false;
			
			// get the header (entry 0)
			SmsHistory_Header header = getHeader(false);
			visitedEntries[0] = true;
			
			// go through all the free entries
			SmsHistory_Free free;
			long indexNext = header.getIndexFree();
			while (indexNext != 0) {
				free = SmsHistory_Free.parseData(getEntry(indexNext, false));
				visitedEntries[(int) indexNext] = true;
				indexNext = free.getIndexNext();
			}
			
			// now check that all have been visited
			for (int i = 0; i < countEntries; ++i)
				visitedAll &= visitedEntries[i];
		} catch (HistoryFileException ex) {
			throw new HistoryFileException(ex.getMessage());
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
