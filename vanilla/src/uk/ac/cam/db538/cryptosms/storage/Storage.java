/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package uk.ac.cam.db538.cryptosms.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

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
			File f = new File(mFilename);
			exists = f.exists();
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
	
	/**
	 * Close file.
	 *
	 * @throws StorageFileException the storage file exception
	 */
	public synchronized void closeFile() throws StorageFileException {
		try {
			smsFile.mFile.close();
		} catch (IOException ex) {
			throw new StorageFileException(ex);
		}
	}
	
	/**
	 * Delete file.
	 */
	public synchronized void deleteFile() {
		new File(mFilename).delete();
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
	 * Reads data from specified entry index the file.
	 *
	 * @param index the index
	 * @return the entry
	 * @throws StorageFileException the storage file exception
	 */
	synchronized byte[] getEntry(long index) throws StorageFileException {
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
	 * Saves data to specified entry index the file.
	 *
	 * @param index the index
	 * @param data the data
	 * @throws StorageFileException the storage file exception
	 */
	synchronized void setEntry(long index, byte[] data) throws StorageFileException {
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

	// LISTENERS
	
	private static ArrayList<StorageChangeListener> mGlobalListeners = new ArrayList<StorageChangeListener>();
	
	public static interface StorageChangeListener {
		
		/**
		 * On update.
		 */
		public void onUpdate();
	}
	
	/**
	 * Notify change.
	 */
	public static void notifyChange() {
		for (StorageChangeListener listener: mGlobalListeners) 
			listener.onUpdate();
	}
	
	/**
	 * Adds a listener.
	 *
	 * @param listener the listener
	 */
	public static void addListener(StorageChangeListener listener) {
		mGlobalListeners.add(listener);
	}
	
	/**
	 * Removes listener.
	 *
	 * @param listener the listener
	 */
	public static void removeListener(StorageChangeListener listener) {
		mGlobalListeners.remove(listener);
	}
	
	// FOR TESTING ONLY
	/**
	 * Deletes the singleton.
	 */
	static void freeSingleton() {
		if (mSingleton != null)
			try {
				mSingleton.smsFile.mFile.close();
			} catch (Exception e) {
			}
		mSingleton = null;
	}
}
