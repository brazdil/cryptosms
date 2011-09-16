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
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Represents the opened secure storage file 
 * 
 * @author David Brazdil
 *
 */
class StorageFile {
	RandomAccessFile mFile;
//	FileLock mLock;

	/**
 * Instantiates a new storage file.
 *
 * @param filename the filename
 * @throws IOException Signals that an I/O exception has occurred.
 */
StorageFile(String filename) throws IOException {
		String directory = new File(filename).getParent();
		new File(directory).mkdirs();
		mFile = new RandomAccessFile(filename, "rw");
//		mLock = null;
	}

//	void lock() throws IOException {
//		mLock = mFile.getChannel().lock();
//	}
//	
//	void unlock() {
//		try {
//			if (mLock.isValid())
//				mLock.release();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
}