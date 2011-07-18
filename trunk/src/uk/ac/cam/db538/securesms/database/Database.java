package uk.ac.cam.db538.securesms.database;

import java.io.*;
import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.content.Context;
import android.text.format.Time;

public final class Database {
	
	private static final String FILE_NAME = "data.db";
	
	private static final String CHARSET_LATIN = "ISO-8859-1";
	static final int CHUNK_SIZE = 256;
	static final int ALIGN_SIZE = 256 * 32; // 8KB
	static final int ENCRYPTED_ENTRY_SIZE = CHUNK_SIZE - Encryption.ENCRYPTION_OVERHEAD;
	
	// SINGLETON STUFF
	
	private static Database mSingleton = null;
	
	public static Database getSingleton() throws DatabaseFileException {
		if (mSingleton == null) 
			throw new DatabaseFileException("Database not initialized yet");
		return mSingleton;
	}
	
	public static void initSingleton(Context context) throws IOException, DatabaseFileException {
		String filename = context.getFilesDir().getAbsolutePath() + "/" + FILE_NAME;
		initSingleton(filename);
	}
	
	public static void initSingleton(String filename) throws IOException, DatabaseFileException {
		mSingleton = new Database(filename);
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
	
	static byte[] toLatin(String text, int bufferLength) {
		ByteBuffer buffer = ByteBuffer.allocate(bufferLength);

		byte[] latin = null;
		try {
			latin = text.getBytes(CHARSET_LATIN);
		} catch (UnsupportedEncodingException e) {
		}
		
		if (latin.length < bufferLength) {
			buffer.put(latin);
			buffer.put((byte) 0x00);
			buffer.put(Encryption.generateRandomData(bufferLength - latin.length - 1));
		}
		else
			buffer.put(latin, 0, bufferLength);
		
		return buffer.array();		
	}
	
	static String fromLatin(byte[] latinData) {
		return fromLatin(latinData, 0, latinData.length);
	}
	
	static String fromLatin(byte[] latinData, int offset, int len) {
		int length = 0;
		while (length < len && latinData[offset + length] != 0)
			++length;
		
		try {
			return new String(latinData, offset, Math.min(len, length), CHARSET_LATIN);
		} catch (UnsupportedEncodingException ex) {
			return null;
		}		
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
	
	private void setHeader(FileEntryHeader entryHeader) throws DatabaseFileException, IOException {
		setHeader(entryHeader, true);
	}

	private void setHeader(FileEntryHeader entryHeader, boolean lock) throws DatabaseFileException, IOException {
		setEntry(0, FileEntryHeader.createData(entryHeader), lock);
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
	
	private FileEntryConversation getConversation(long indexEntry) throws DatabaseFileException, IOException {
		return getConversation(indexEntry, true);
	}
	
	private FileEntryConversation getConversation(long indexEntry, boolean lock) throws DatabaseFileException, IOException {
		return FileEntryConversation.parseData(getEntry(indexEntry, lock));
	}
	
	private void setConversation(long indexEntry, FileEntryConversation entryConversation) throws DatabaseFileException, IOException {
		setConversation(indexEntry, entryConversation, true);
	}
	
	private void setConversation(long indexEntry, FileEntryConversation entryConversation, boolean lock) throws DatabaseFileException, IOException {
		setEntry(indexEntry, FileEntryConversation.createData(entryConversation), lock);		
	}
	
	// HIGH-LEVEL STUFF

	public int getFileVersion() throws DatabaseFileException, IOException {
		return getHeader().getVersion();
	}

	public void addEmptyEntries(int count) throws IOException, DatabaseFileException { 
		addEmptyEntries(count, true);
	}
	
	public Conversation createConversation(String phoneNumber, Time timeStamp) throws DatabaseFileException, IOException {
		Conversation conv = null;
		
		smsFile.lock();
		try {
			// allocate entry for the new conversation
			// has to be first, because it changes the header
			long indexNew = getEmptyEntry(false);
			
			// get the header
			FileEntryHeader entryHeader = getHeader(false);
			
			// get the index of first conversation in list
			long indexFirst = entryHeader.getIndexConversations();
			// if it exists, update it and save it
			if (indexFirst > 0) {
				FileEntryConversation convFirst = getConversation(indexFirst, false);
				convFirst.setIndexPrev(indexNew);
				setConversation(indexFirst, convFirst, false);
			}

			// generate a session key
			byte[] sessionKey = Encryption.generateRandomData(Encryption.KEY_LENGTH);
			
			// create new entry and save it
			FileEntryConversation entryConversation = new FileEntryConversation(false,
			                                                                    phoneNumber, 
			                                                                    timeStamp, 
			                                                                    sessionKey, 
			                                                                    sessionKey, 
			                                                                    0L, 
			                                                                    0L, 
			                                                                    indexFirst);
			setConversation(indexNew, entryConversation, false);
			
			// update index in header
			entryHeader.setIndexConversations(indexNew);
			setHeader(entryHeader, false);
			
			// set the real world class to hold its index
			conv = new Conversation(indexNew);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			smsFile.unlock();
		}
		
		return conv;
	}
	
	void updateConversation(Conversation conv) throws DatabaseFileException, IOException {
		FileEntryConversation entryConversation = getConversation(conv.getIndexEntry());
		conv.setKeysExchanged(entryConversation.getKeysExchanged());
		conv.setPhoneNumber(entryConversation.getPhoneNumber());
		conv.setTimeStamp(entryConversation.getTimeStamp());
		conv.setSessionKey_Out(entryConversation.getSessionKey_Out());
		conv.setSessionKey_In(entryConversation.getSessionKey_In());
	}

	void saveConversation(Conversation conv) throws DatabaseFileException, IOException {
		smsFile.lock();
		try {
			FileEntryConversation entryConversation = getConversation(conv.getIndexEntry(), false);
			entryConversation.setKeysExchanged(conv.getKeysExchanged());
			entryConversation.setPhoneNumber(conv.getPhoneNumber());
			entryConversation.setTimeStamp(conv.getTimeStamp());
			entryConversation.setSessionKey_Out(conv.getSessionKey_Out());
			entryConversation.setSessionKey_In(conv.getSessionKey_In());
			setConversation(conv.getIndexEntry(), entryConversation, false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			smsFile.unlock();
		}
	}

	// FOR TESTING PURPOSES

	int getEmptyEntriesCount() throws DatabaseFileException, IOException {
		return getEmptyEntriesCount(true);
	}

	private int getEmptyEntriesCount(boolean lock) throws DatabaseFileException, IOException {
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
	
	private boolean checkStructure(boolean lock) throws IOException, DatabaseFileException {
		boolean visitedAll = true;
		boolean corruptedPointers = false;

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
			
			// go through all the conversations
			FileEntryConversation conv;
			indexNext = header.getIndexConversations();
			long indexPrev = 0;
			while (indexNext != 0) {
				conv = getConversation(indexNext, false);
				visitedEntries[(int) indexNext] = true;
				if (conv.getIndexPrev() != indexPrev)
					corruptedPointers = true;
				indexPrev = indexNext;
				indexNext = conv.getIndexNext();
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
		
		return (visitedAll && !corruptedPointers);
	}
	
	static void freeSingleton() {
		mSingleton = null;
	}
}
