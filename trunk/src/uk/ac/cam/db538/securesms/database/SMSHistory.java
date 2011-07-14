package uk.ac.cam.db538.securesms.database;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.content.Context;

public class SMSHistory {
	
	private static final String FILE_NAME = "history.enc";
	private static final int CHUNK_SIZE = 256;
	private static final int ALIGN_SIZE = 256 * 32; // 8KB
	private static final int ENCRYPTED_ENTRY_SIZE = CHUNK_SIZE - Encryption.ENCRYPTION_OVERHEAD;
	private static final int PLAIN_HEADER_SIZE = 4;
	private static final int ENCRYPTED_HEADER_SIZE = ENCRYPTED_ENTRY_SIZE - PLAIN_HEADER_SIZE;
	
	private static final int HEADER_OFFSET_CONVINDEX = ENCRYPTED_HEADER_SIZE - 4;
	private static final int HEADER_OFFSET_FREEINDEX = HEADER_OFFSET_CONVINDEX - 4;
	
	private static final int FREE_OFFSET_NEXTINDEX = ENCRYPTED_ENTRY_SIZE - 4;
	
	private static class SMSHistoryFile {
		private RandomAccessFile mFile;
		private FileLock mLock;
		
		public SMSHistoryFile(String filename) throws IOException {
			mFile = new RandomAccessFile(filename, "rw");
			mLock = null;
		}

		public void lock() throws IOException {
			mLock = mFile.getChannel().lock();
		}
		
		public void unlock() {
			try {
				if (mLock.isValid())
					mLock.release();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static SMSHistory mSingleton = null;
	
	public static SMSHistory getSingleton(Context context) throws IOException, HistoryFileException {
		if (mSingleton == null)
			mSingleton = new SMSHistory(context);
		return mSingleton;
	}
	
	private SMSHistoryFile smsFile;
	
	private SMSHistory(Context context) throws IOException, HistoryFileException {
		smsFile = new SMSHistoryFile(context.getFilesDir().getAbsolutePath() + "/" + FILE_NAME);
		createFile();
	}
	
	public int getFileVersion() {
		return 1;
	}
	
	private byte[] createHeader(long indexFree, long indexConversations) {
		ByteBuffer headerBuffer = ByteBuffer.allocate(ENCRYPTED_HEADER_SIZE);
		headerBuffer.put(Encryption.getRandomData(ENCRYPTED_HEADER_SIZE - 8));
		headerBuffer.put(getBytes(indexFree)); 
		headerBuffer.put(getBytes(indexConversations));
		
		ByteBuffer headerBufferEncrypted = ByteBuffer.allocate(CHUNK_SIZE);
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) 0x4D); // M
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) 0x01); // Version 1
		headerBufferEncrypted.put(Encryption.encodeWithPassphrase(headerBuffer.array()));
		
		return headerBufferEncrypted.array();
	}
	
	private byte[] createFree(long indexNext) {
		ByteBuffer entryBuffer = ByteBuffer.allocate(ENCRYPTED_ENTRY_SIZE);
		entryBuffer.put(Encryption.getRandomData(FREE_OFFSET_NEXTINDEX));
		entryBuffer.put(getBytes(indexNext));
		return Encryption.encodeWithPassphrase(entryBuffer.array());
	}
	
	public void createFile() throws FileNotFoundException, IOException, HistoryFileException {
		int countFreeEntries = ALIGN_SIZE / CHUNK_SIZE - 1;
		byte[][] freeEncoded = new byte[countFreeEntries][];
		long previousIndex = 0;
		for (int i = countFreeEntries - 1; i >= 0; --i) {
			freeEncoded[i] = createFree(previousIndex);
			previousIndex = i + 1;
		}
		byte[] headerEncoded = createHeader(previousIndex, 0);
		
		smsFile.lock();
		smsFile.mFile.seek(0);
		smsFile.mFile.write(headerEncoded);
		for (int i = 0; i < countFreeEntries; ++i)
			smsFile.mFile.write(freeEncoded[i]);
		smsFile.unlock();
	}
	
	private byte[] getEntry(long index) throws HistoryFileException, IOException {
		long offset = index * CHUNK_SIZE;
		if (offset < PLAIN_HEADER_SIZE) offset = PLAIN_HEADER_SIZE;
		
		if (offset >= smsFile.mFile.length())
			throw new HistoryFileException("Index in history file out of bounds");
		
		smsFile.lock();
		byte[] data = new byte[(offset == PLAIN_HEADER_SIZE) ? CHUNK_SIZE - PLAIN_HEADER_SIZE : CHUNK_SIZE];
		smsFile.mFile.seek(offset);
		smsFile.mFile.read(data);
		smsFile.unlock();
		return data;
	}
	
	public void addEmptyEntry() throws IOException, HistoryFileException {
		byte[] header = Encryption.decodeWithPassphrase(getEntry(0));
		long indexFree = getInt(header, HEADER_OFFSET_FREEINDEX);
		
		byte[] entryEncoded = createFree(indexFree);
		
		smsFile.lock();
		smsFile.unlock();
	}
	
	private long getInt(byte[] data, int offset) {
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
	
	private byte[] getBytes(long integer) {
		byte[] result = new byte[4];
		result[0] = (byte) ((integer >> 24) & 0xFF);
		result[1] = (byte) ((integer >> 16) & 0xFF);
		result[2] = (byte) ((integer >> 8) & 0xFF);
		result[3] = (byte) (integer & 0xFF);
		return result;
	}
}
