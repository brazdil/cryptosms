package uk.ac.cam.db538.securesms.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.content.Context;

public final class Storage {
	private static final String FILE_NAME = "storage.db";
	
	static final int CHUNK_SIZE = 256;
	static final int ALIGN_SIZE = 256 * 32; // 8KB
	static final int ENCRYPTED_ENTRY_SIZE = CHUNK_SIZE - Encryption.ENCRYPTION_OVERHEAD;
	
	// SINGLETON STUFF
	
	private static Storage mSingleton = null;
	
	/**
	 * Returns the instance of the Database singleton class.
	 * Singleton has to be initialised beforehand with the initSingleton method.   
	 * @return instance of Database class
	 * @throws StorageFileException
	 */
	public static Storage getDatabase() throws StorageFileException {
		if (mSingleton == null) 
			throw new StorageFileException("Database not initialized yet");
		return mSingleton;
	}
	
	/**
	 * Initialises the Database singleton automatically based on the environment settings supplied by application context. 
	 * @param context		Application context. Used to get the path of a folder for package data.
	 * @throws IOException
	 * @throws StorageFileException
	 */
	public static void initSingleton(Context context) throws IOException, StorageFileException {
		String filename = context.getFilesDir().getAbsolutePath() + "/" + FILE_NAME;

		initSingleton(filename);
	}

	/**
	 * Initialises the Database singleton to use a specified file as the secure storage.
	 * @param filename 		Path to the secure storage file.
	 * @throws IOException
	 * @throws StorageFileException
	 */
	public static void initSingleton(String filename) throws IOException, StorageFileException {
		if (mSingleton != null) 
			return; //throw new DatabaseFileException("Database already initialized");
		
		new Storage(filename);
	}
	
	// LOW-LEVEL BIT MANIPULATION
	
	/**
	 * Expects four bytes and returns an unsigned integer stored in a long that's represented by the bytes.
	 */
	static long getInt(byte[] data) {
		return getInt(data, 0);
	}

	/**
	 * Expects four bytes (in an array at specified offset) and returns an unsigned integer stored in a long that's represented by the bytes.
	 */
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
	
	/**
	 * Expects an unsigned integer stored in the 4 low bytes of a long and returns an array of 4 bytes that represent them.
	 */
	static byte[] getBytes(long integer) {
		byte[] result = new byte[4];
		result[0] = (byte) ((integer >> 24) & 0xFF);
		result[1] = (byte) ((integer >> 16) & 0xFF);
		result[2] = (byte) ((integer >> 8) & 0xFF);
		result[3] = (byte) (integer & 0xFF);
		return result;
	}

	// FILE MANIPULATION
	
	private StorageFile smsFile;

	/**
	 * Constructor
	 * @param filename
	 * @throws IOException
	 * @throws StorageFileException
	 */
	private Storage(String filename) throws IOException, StorageFileException {
		mSingleton = this;
		
		boolean exists = new File(filename).exists();
		smsFile = new StorageFile(filename);
		if (!exists)
			createFile();
	}
	
	/**
	 * Creates empty file when there isn't any
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws StorageFileException
	 */
	private synchronized void createFile() throws IOException, StorageFileException {
		int countFreeEntries = ALIGN_SIZE / CHUNK_SIZE - 1;
		
		lockFile();
		try {
			// create an empty instance of the header
			Header.createHeader(false);
			// add some empty entries
			Empty.addEmptyEntries(countFreeEntries, false);
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
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
	public synchronized void lockFile(boolean condition) throws IOException {
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
	public synchronized void unlockFile(boolean condition) throws IOException {
		if (condition) smsFile.unlock();
	}
	
	/**
	 * Returns number of entries in the file based on the file size
	 * @return
	 * @throws IOException 
	 */
	public synchronized long getEntriesCount() throws IOException {
		return smsFile.mFile.length() / CHUNK_SIZE;
	}

	/**
	 * Reads data from specified entry index the file
	 * @param index
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	byte[] getEntry(long index) throws StorageFileException, IOException {
		return getEntry(index, true);
	}

	/**
	 * Reads data from specified entry index the file
	 * @param index
	 * @param lock
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	synchronized byte[] getEntry(long index, boolean lock) throws StorageFileException, IOException {
		long offset = index * CHUNK_SIZE;
		if (offset > smsFile.mFile.length() - CHUNK_SIZE)
			throw new StorageFileException("Index in history file out of bounds");
		
		lockFile(lock);
		byte[] data = new byte[CHUNK_SIZE];
		try {
			smsFile.mFile.seek(offset);
			smsFile.mFile.read(data);
		} catch (IOException ex) {
			throw ex;
		} finally {
			unlockFile(lock);
		}

		return data;
	}
	
	/**
	 * Saves data to specified entry index the file
	 * @param index
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	void setEntry(long index, byte[] data) throws StorageFileException, IOException {
		setEntry(index, data, true);
	}

	/**
	 * Saves data to specified entry index the file
	 * @param index
	 * @param lock
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	synchronized void setEntry(long index, byte[] data, boolean lock) throws StorageFileException, IOException {
		long offset = index * CHUNK_SIZE;
		long fileSize = smsFile.mFile.length();
		if (offset > fileSize)
			throw new StorageFileException("Index in history file out of bounds");

		lockFile(lock);
		try {
			smsFile.mFile.seek(offset);
			smsFile.mFile.write(data);
		} catch (IOException ex) {
			throw ex;
		} finally {
			unlockFile(lock);
		}
	}

	// FOR TESTING ONLY
	static void freeSingleton() {
		mSingleton = null;
	}
}
