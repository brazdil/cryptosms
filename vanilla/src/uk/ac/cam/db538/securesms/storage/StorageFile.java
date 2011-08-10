package uk.ac.cam.db538.securesms.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

/**
 * Represents the opened secure storage file 
 * 
 * @author David Brazdil
 *
 */
class StorageFile {
	RandomAccessFile mFile;
	FileLock mLock;

	
	StorageFile(String filename) throws IOException {
		String directory = new File(filename).getParent();
		new File(directory).mkdirs();
		mFile = new RandomAccessFile(filename, "rw");
		mLock = null;
	}

	void lock() throws IOException {
		mLock = mFile.getChannel().lock();
	}
	
	void unlock() {
		try {
			if (mLock.isValid())
				mLock.release();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}