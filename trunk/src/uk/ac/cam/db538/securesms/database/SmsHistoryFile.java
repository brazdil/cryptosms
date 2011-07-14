package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class SmsHistoryFile {
	public RandomAccessFile mFile;
	public FileLock mLock;

	public SmsHistoryFile(String filename) throws IOException {
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