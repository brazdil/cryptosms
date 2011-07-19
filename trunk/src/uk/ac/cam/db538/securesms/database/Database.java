package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.text.format.Time;

/**
 * 
 * Singleton class for operations on the secure storage file
 * 
 * @author David Brazdil
 *
 */
public final class Database {
	
	private static final String FILE_NAME = "data.db";
	
	private static final String CHARSET_LATIN = "ISO-8859-1";
	static final int CHUNK_SIZE = 256;
	static final int ALIGN_SIZE = 256 * 32; // 8KB
	static final int ENCRYPTED_ENTRY_SIZE = CHUNK_SIZE - Encryption.ENCRYPTION_OVERHEAD;
	
	// SINGLETON STUFF
	
	private static Database mSingleton = null;
	
	/**
	 * Returns the instance of the Database singleton class.
	 * Singleton has to be initialised beforehand with the initSingleton method.   
	 * @return instance of Database class
	 * @throws DatabaseFileException
	 */
	public static Database getSingleton() throws DatabaseFileException {
		if (mSingleton == null) 
			throw new DatabaseFileException("Database not initialized yet");
		return mSingleton;
	}
	
	/**
	 * Initialises the Database singleton automatically based on the environment settings supplied by application context. 
	 * @param context		Application context. Used to get the path of a folder for package data.
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	public static void initSingleton(Context context) throws IOException, DatabaseFileException {
		String filename = context.getFilesDir().getAbsolutePath() + "/" + FILE_NAME;
		initSingleton(filename);
	}

	/**
	 * Initialises the Database singleton to use a specified file as the secure storage.
	 * @param filename 		Path to the secure storage file.
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	public static void initSingleton(String filename) throws IOException, DatabaseFileException {
		if (mSingleton != null) 
			throw new DatabaseFileException("Database already initialized");
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
	
	/**
	 * Locks the secure storage file.
	 * @throws IOException
	 */
	public void lockFile() throws IOException {
		lockFile(true);
	}

	/**
	 * Locks the secure storage file if the condition is set to true.
	 * @param condition		Lock condition
	 * @throws IOException
	 */
	public void lockFile(boolean condition) throws IOException {
		if (condition) smsFile.lock();
	}
	
	/**
	 * Unlocks the secure storage file.
	 * Doesn't do anything if the file is not locked or the lock is invalid.
	 * @throws IOException
	 */
	public void unlockFile() throws IOException {
		unlockFile(true);
	}

	/**
	 * Unlocks the secure storage file provided the condition parameter is set to true.
	 * Doesn't do anything if the file is not locked or the lock is invalid.
	 * @param condition		Unlock condition.
	 * @throws IOException
	 */
	public void unlockFile(boolean condition) throws IOException {
		if (condition) smsFile.unlock();
	}

	private void createFile() throws FileNotFoundException, IOException, DatabaseFileException {
		int countFreeEntries = ALIGN_SIZE / CHUNK_SIZE - 1;
		byte[] headerEncoded = FileEntryHeader.createData(new FileEntryHeader(0, 0));
		
		lockFile();
		try {
			setEntry(0, headerEncoded, false);
			addEmptyEntries(countFreeEntries, false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			unlockFile();
		}
	}
	
	private byte[] getEntry(long index) throws DatabaseFileException, IOException {
		return getEntry(index, true);
	}
	
	private byte[] getEntry(long index, boolean lock) throws DatabaseFileException, IOException {
		long offset = index * CHUNK_SIZE;
		if (offset > smsFile.mFile.length() - CHUNK_SIZE)
			throw new DatabaseFileException("Index in history file out of bounds");
		
		lockFile(lock);
		byte[] data = new byte[CHUNK_SIZE];
		try {
			smsFile.mFile.seek(offset);
			smsFile.mFile.read(data);
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			unlockFile(lock);
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

		lockFile(lock);
		try {
			smsFile.mFile.seek(offset);
			smsFile.mFile.write(data);
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			unlockFile(lock);
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
		lockFile(lock);
		long result = 0;
		try {
			FileEntryHeader header = getHeader(false);
			long previousFree = header.getIndexFree();
			if (previousFree == 0) {
				// there are no free entries left
				// => add some
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
			unlockFile(lock);
		}
		return result;
	}
	
	private void addEmptyEntries(int count, boolean lock) throws IOException, DatabaseFileException {
		lockFile(lock);
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
			unlockFile(lock);
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

	/**
	 * Returns the secure storage file version from the header
	 */
	public int getFileVersion() throws DatabaseFileException, IOException {
		return getHeader().getVersion();
	}

	/**
	 * Adds new free entries into the file, increasing its size.
	 * @param count		Number of new free entries.
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	public void addEmptyEntries(int count) throws IOException, DatabaseFileException { 
		addEmptyEntries(count, true);
	}
	
	/**
	 * Takes a free entry from the storage file and turns it into a conversation.
	 * If there are no free entries left, it creates new ones.
	 * Session keys are generated randomly.
	 * Time stamps is set to current time. 
	 * @param phoneNumber		Phone number of the recipient.
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation createConversation(String phoneNumber) throws DatabaseFileException, IOException {
		Time timeStamp = new Time();
		timeStamp.setToNow();
		return createConversation(phoneNumber, timeStamp);
	}
	
	/**
	 * Takes a free entry from the storage file and turns it into a conversation.
	 * If there are no free entries left, it creates new ones.
	 * Session keys are generated randomly. 
	 * @param phoneNumber		Phone number of the recipient
	 * @param timeStamp			Time stamp
	 * @return					New Conversation class
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation createConversation(String phoneNumber, Time timeStamp) throws DatabaseFileException, IOException {
		Conversation conv = null;
		
		lockFile();
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

			// create new entry and save it
			FileEntryConversation entryConversation = new FileEntryConversation(phoneNumber, 
			                                                                    timeStamp,
			                                                                    0L,
			                                                                    0L, 
			                                                                    0L, 
			                                                                    indexFirst);
			setConversation(indexNew, entryConversation, false);
			
			// update index in header
			entryHeader.setIndexConversations(indexNew);
			setHeader(entryHeader, false);
			
			// set the real world class to hold its index
			conv = new Conversation(indexNew);
			conv.update(false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			unlockFile();
		}
		
		return conv;
	}
	
	/**
	 * Finds a conversation in the secure file based on the phone number of the recipient.
	 * 
	 * @param phoneNumber		Phone number of the recipient.
	 * @return					Conversation class. Null if not found.
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	public Conversation getConversation(String phoneNumber) throws IOException, DatabaseFileException {
		return getConversation(phoneNumber, true);
	}
	
	private Conversation getConversation(String phoneNumber, boolean lock) throws IOException, DatabaseFileException {
		Conversation conv = null;

		lockFile(lock);
		try {
			FileEntryHeader entryHeader = getHeader(false);
			FileEntryConversation entryConv;
			long indexNext = entryHeader.getIndexConversations();
			while (indexNext != 0) {
				entryConv = getConversation(indexNext, false);
				
				if (PhoneNumberUtils.compare(phoneNumber, entryConv.getPhoneNumber())) {
					// phone numbers are the same (or similar enough)
					
					if (entryConv.getPhoneNumber().length() != 13 && !entryConv.getPhoneNumber().startsWith("+") &&
						phoneNumber.length() == 13 && phoneNumber.startsWith("+")
						) {
						// the new one should be in the international format, while the old should not
						// replace it
						entryConv.setPhoneNumber(phoneNumber);
						setConversation(indexNext, entryConv, false);
					}
					
					// create the Conversation class
					conv = new Conversation(indexNext);
					conv.update(false);
					
					// stop searching
					indexNext = 0;
				}
				else {
					indexNext = entryConv.getIndexNext();
				}
			}
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			unlockFile(lock);
		}
		
		return conv;
	}
	
	/**
	 * Tries to find the conversation in the secure file using the getConversation method. If it fails, it creates a new one with the createConversation method.
	 * @param phoneNumber		Phone number of the recipient.
	 * @return					Instance of Conversation class. 
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation getOrCreateConversation(String phoneNumber) throws DatabaseFileException, IOException {
		return getOrCreateConversation(phoneNumber, true);
	}
	
	private Conversation getOrCreateConversation(String phoneNumber, boolean lock) throws DatabaseFileException, IOException {
		Conversation conv = getConversation(phoneNumber);
		if (conv == null)
			return createConversation(phoneNumber);
		else
			return conv;
	}
	
	/*
	 * Returns a list of all the conversations in the storage file.
	 * Conversations are sorted by their time stamps from oldest to newest.
	 */
	public ArrayList<Conversation> getListOfConversations() throws DatabaseFileException, IOException {
		return getListOfConversations(true);
	}

	private ArrayList<Conversation> getListOfConversations(Boolean lock) throws DatabaseFileException, IOException {		
		ArrayList<Conversation> list = new ArrayList<Conversation>();
		Conversation conv;
		
		lockFile(lock);
		try {
			FileEntryHeader entryHeader = getHeader(false);
			FileEntryConversation entryConv;
			long indexNext = entryHeader.getIndexConversations();
			while (indexNext != 0) {
				entryConv = getConversation(indexNext, false);
				
				// create the Conversation class
				conv = new Conversation(indexNext);
				conv.update(false);
				
				// put it in the list
				list.add(conv);
				
				indexNext = entryConv.getIndexNext();
			}
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			unlockFile(lock);
		}

		Collections.sort(list);
		return list;
	}
	
	void updateConversation(Conversation conv) throws DatabaseFileException, IOException {
		updateConversation(conv, true);
	}
	
	void updateConversation(Conversation conv, boolean lock) throws DatabaseFileException, IOException {
		FileEntryConversation entryConversation = getConversation(conv.getIndexEntry(), lock);
		conv.setPhoneNumber(entryConversation.getPhoneNumber());
		conv.setTimeStamp(entryConversation.getTimeStamp());
	}
	
	void saveConversation(Conversation conv) throws DatabaseFileException, IOException {
		saveConversation(conv, true);
	}

	void saveConversation(Conversation conv, boolean lock) throws DatabaseFileException, IOException {
		lockFile(lock);
		try {
			FileEntryConversation entryConversation = getConversation(conv.getIndexEntry(), false);
			entryConversation.setPhoneNumber(conv.getPhoneNumber());
			entryConversation.setTimeStamp(conv.getTimeStamp());
			setConversation(conv.getIndexEntry(), entryConversation, false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			unlockFile(lock);
		}
	}

	// FOR TESTING PURPOSES

	int getEmptyEntriesCount() throws DatabaseFileException, IOException {
		return getEmptyEntriesCount(true);
	}

	private int getEmptyEntriesCount(boolean lock) throws DatabaseFileException, IOException {
		int count = 0;

		lockFile(lock);
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
			unlockFile(lock);
		}
		
		return count;
	}
	
	boolean checkStructure() throws IOException, DatabaseFileException {
		return checkStructure(true);
	}
	
	private boolean checkStructure(boolean lock) throws IOException, DatabaseFileException {
		boolean visitedAll = true;
		boolean corruptedPointers = false;

		lockFile(lock);
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
			unlockFile(lock);
		}
		
		return (visitedAll && !corruptedPointers);
	}
	
	static void freeSingleton() {
		mSingleton = null;
	}
}
