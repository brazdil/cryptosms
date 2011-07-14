package uk.ac.cam.db538.securesms.database;

import java.io.*;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.content.Context;

public class SmsHistory {
	
	private static final String FILE_NAME = "history.enc";
	
	static final int CHUNK_SIZE = 256;
	static final int ALIGN_SIZE = 256 * 32; // 8KB
	static final int ENCRYPTED_ENTRY_SIZE = CHUNK_SIZE - Encryption.ENCRYPTION_OVERHEAD;
	
	private static SmsHistory mSingleton = null;
	
	public static SmsHistory getSingleton(Context context) throws IOException, HistoryFileException {
		if (mSingleton == null)
			mSingleton = new SmsHistory(context);
		return mSingleton;
	}
	
	private SmsHistoryFile smsFile;
	
	private SmsHistory(Context context) throws IOException, HistoryFileException {
		String filename = context.getFilesDir().getAbsolutePath() + "/" + FILE_NAME;
		boolean exists = new File(filename).exists();
		smsFile = new SmsHistoryFile(filename);
		if (!exists)
			createFile();
	}
	
	public int getFileVersion() {
		return 1;
	}
	
	public void createFile() throws FileNotFoundException, IOException, HistoryFileException {
		int countFreeEntries = ALIGN_SIZE / CHUNK_SIZE - 1;
		byte[] headerEncoded = SmsHistoryHeader.createData(new SmsHistoryHeader(0, 0));
		setEntry(headerEncoded, 0);
		addFreeEntries(countFreeEntries);
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
		smsFile.mFile.seek(offset);
		smsFile.mFile.read(data);
		if (lock) smsFile.unlock();
		return data;
	}
	
	private void setEntry(byte[] data, long index) throws HistoryFileException, IOException {
		setEntry(data, index, true);
	}
	
	private void setEntry(byte[] data, long index, boolean lock) throws HistoryFileException, IOException {
		long offset = index * CHUNK_SIZE;
		if (offset > smsFile.mFile.length())
			throw new HistoryFileException("Index in history file out of bounds");

		if (lock) smsFile.lock();
		smsFile.mFile.seek(offset);
		smsFile.mFile.write(data);
		if (lock) smsFile.unlock();
	}

	private long getFreeEntry() throws HistoryFileException, IOException {
		SmsHistoryHeader header = SmsHistoryHeader.parseData(getEntry(0));
		long previousFree = header.getIndexFree();
		if (previousFree == 0) {
			// there are no free entries left
			addFreeEntries(ALIGN_SIZE / CHUNK_SIZE);
			return getFreeEntry();
		}
		else {
			// remove the entry from stack
			SmsHistoryFree free = SmsHistoryFree.parseData(getEntry(previousFree));
			header.setIndexFree(free.getIndexNext());
			// update header
			setEntry(SmsHistoryHeader.createData(header), 0);
			// return the index of the freed entry
			return previousFree;
		}
	}
	
	public void addFreeEntries(int count) throws IOException, HistoryFileException {
		SmsHistoryHeader header = SmsHistoryHeader.parseData(getEntry(0));
		long previousFree = header.getIndexFree();
		long countEntries = smsFile.mFile.length() / CHUNK_SIZE;
		byte[][] entriesEncoded = new byte[count][];
		for (int i = 0; i < count; ++i) {
			entriesEncoded[i] = SmsHistoryFree.createData(new SmsHistoryFree(previousFree));
			previousFree = countEntries + i;
		}
		header.setIndexFree(previousFree);
		byte[] headerEncoded = SmsHistoryHeader.createData(header);
		
		smsFile.lock();
		for (int i = 0; i < count; ++i)
			setEntry(entriesEncoded[i], countEntries + i, false);
		setEntry(headerEncoded, 0, false);
		smsFile.unlock();
	}

	static long getInt(byte[] data, int offset) {
		if (offset > data.length - 4)
			return 0L;

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
}
