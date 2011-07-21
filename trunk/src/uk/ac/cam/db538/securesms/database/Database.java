package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.content.Context;

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
	public static Database getDatabase() throws DatabaseFileException {
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
			return; //throw new DatabaseFileException("Database already initialized");
		
		//TODO: Just For Testing!!! 
		File file = new File(filename);
		if (file.exists())
			file.delete();

		new Database(filename);

		//TODO: Just For Testing!!!
		Conversation conv1 = Conversation.createConversation();
		conv1.setPhoneNumber("+420605219051");
		conv1.saveToFile();
		Conversation conv2 = Conversation.createConversation();
		conv2.setPhoneNumber("+20104544366");
		conv2.saveToFile();
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
		mSingleton = this;
		
		boolean exists = new File(filename).exists();
		smsFile = new DatabaseFile(filename);
		if (!exists)
			createFile();
	}
	
	/**
	 * Creates empty file when there isn't any
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	private void createFile() throws FileNotFoundException, IOException, DatabaseFileException {
		int countFreeEntries = ALIGN_SIZE / CHUNK_SIZE - 1;
		
		lockFile();
		try {
			// create an empty instance of the header
			Header.createHeader(false);
			// add some empty entries
			Empty.addEmptyEntries(countFreeEntries, false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			unlockFile();
		}
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
	
	/**
	 * Returns number of entries in the file based on the file size
	 * @return
	 * @throws IOException 
	 */
	public long getEntriesCount() throws IOException {
		return smsFile.mFile.length() / CHUNK_SIZE;
	}

	/**
	 * Reads data from specified entry index the file
	 * @param index
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	byte[] getEntry(long index) throws DatabaseFileException, IOException {
		return getEntry(index, true);
	}

	/**
	 * Reads data from specified entry index the file
	 * @param index
	 * @param lock
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	byte[] getEntry(long index, boolean lock) throws DatabaseFileException, IOException {
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
	
	/**
	 * Saves data to specified entry index the file
	 * @param index
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	void setEntry(long index, byte[] data) throws DatabaseFileException, IOException {
		setEntry(index, data, true);
	}

	/**
	 * Saves data to specified entry index the file
	 * @param index
	 * @param lock
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	void setEntry(long index, byte[] data, boolean lock) throws DatabaseFileException, IOException {
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

	// FOR TESTING ONLY
	static void freeSingleton() {
		mSingleton = null;
	}
}
