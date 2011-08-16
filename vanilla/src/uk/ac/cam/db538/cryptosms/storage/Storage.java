package uk.ac.cam.db538.cryptosms.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;

public final class Storage {
	static final int CHUNK_SIZE = 256;
	static final int ALIGN_SIZE = 256 * 32; // 8KB
	static final int ENCRYPTED_ENTRY_SIZE = CHUNK_SIZE - Encryption.SYM_OVERHEAD;
	
	// SINGLETON STUFF
	
	private static Storage mSingleton = null;
	private static String mFilename = null;
	
	/**
	 * Returns the instance of the Database singleton class.
	 * Singleton has to be initialised beforehand with the initSingleton method.   
	 * @return instance of Database class
	 * @throws StorageFileException
	 * @throws IOException 
	 */
	public static Storage getStorage() throws StorageFileException {
		if (mSingleton == null) 
			mSingleton = new Storage();
		return mSingleton;
	}
	
	/**
	 * Initialises the Database singleton to use a specified file as the secure storage.
	 * @param filename 		Path to the secure storage file.
	 * @throws IOException
	 * @throws StorageFileException
	 */
	public static void setFilename(String filename) {
		mFilename = filename;
	}
	
	// FILE MANIPULATION
	
	private StorageFile smsFile;

	/**
	 * Constructor
	 * @param filename
	 * @throws IOException
	 * @throws StorageFileException
	 */
	private Storage() throws StorageFileException {
		if (mFilename == null)
			throw new StorageFileException("No filename was set");
		
		boolean exists = true;
		try {
			exists = new File(mFilename).exists();
			smsFile = new StorageFile(mFilename);
		} catch (IOException ex) {
			throw new StorageFileException(ex);
		}
		
		if (!exists)
			createFile();
	}
	
	/**
	 * Creates empty file when there isn't any
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws StorageFileException
	 */
	private synchronized void createFile() throws StorageFileException {
		int countFreeEntries = ALIGN_SIZE / CHUNK_SIZE - 1;
		// create an empty instance of the header
		Header.createHeader();
		// add some empty entries
		Empty.addEmptyEntries(countFreeEntries);
	}
	
	public synchronized void closeFile() throws StorageFileException {
		try {
			smsFile.mFile.close();
		} catch (IOException ex) {
			throw new StorageFileException(ex);
		}
	}
	
	/**
	 * Returns number of entries in the file based on the file size
	 * @return
	 * @throws StorageFileException 
	 */
	public synchronized long getEntriesCount() throws StorageFileException {
		try {
			return smsFile.mFile.length() / CHUNK_SIZE;
		} catch (IOException ex) {
			throw new StorageFileException(ex);
		}
	}

	/**
	 * Reads data from specified entry index the file
	 * @param index
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	byte[] getEntry(long index) throws StorageFileException {
		try {
			long offset = index * CHUNK_SIZE;
			if (offset > smsFile.mFile.length() - CHUNK_SIZE)
				throw new StorageFileException("Index in history file out of bounds");
			
			byte[] data = new byte[CHUNK_SIZE];
			smsFile.mFile.seek(offset);
			smsFile.mFile.read(data);
			return data;
		} catch (IOException ex) {
			throw new StorageFileException(ex);
		}
	}
	
	/**
	 * Saves data to specified entry index the file
	 * @param index
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	void setEntry(long index, byte[] data) throws StorageFileException {
		try {
			long offset = index * CHUNK_SIZE;
			long fileSize = smsFile.mFile.length();
			if (offset > fileSize)
				throw new StorageFileException("Index in history file out of bounds");
	
			smsFile.mFile.seek(offset);
			smsFile.mFile.write(data);
		} catch (IOException ex) {
			throw new StorageFileException(ex);
		}
	}

	// FOR TESTING ONLY
	static void freeSingleton() {
		if (mSingleton != null)
			try {
				mSingleton.smsFile.mFile.close();
			} catch (Exception e) {
			}
		mSingleton = null;
	}
}
