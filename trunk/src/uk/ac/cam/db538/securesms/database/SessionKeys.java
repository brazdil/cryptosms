package uk.ac.cam.db538.securesms.database;

import java.io.IOException;

/**
 * 
 * Class representing session keys for conversation with a specific person
 * 
 * @author David Brazdil
 *
 */
public class SessionKeys {
	private long mIndexEntry; // READ ONLY
	private boolean mKeysSent;
	private boolean mKeysConfirmed;
	private String mPhoneNumber;
	private byte[] mSessionKey_Out;
	private byte mLastID_Out;
	private byte[] mSessionKey_In;
	private byte mLastID_In;
	
	SessionKeys(long indexEntry) {
		if (indexEntry > 0xFFFFFFFFL || indexEntry <= 0L)
			throw new IndexOutOfBoundsException();
		
		mIndexEntry = indexEntry;
	}

	long getIndexEntry() {
		return mIndexEntry;
	}
	
	/**
	 * Loads data from the secure file (locks the file)
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void update() throws DatabaseFileException, IOException {
		update(true);
	}
	
	/**
	 * Loads data from the secure file
	 * @param lock		Specifies whether the file should be locked during the operation.
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void update(boolean lock) throws DatabaseFileException, IOException {
		if (mIndexEntry != 0)
			Database.getSingleton().updateSessionKeys(this, lock);
	}
	
	/**
	 * Saves data to the secure file (locks the file)
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void save() throws DatabaseFileException, IOException {
		save(true);
	}
	
	/**
	 * Saves data to the secure file
	 * @param lock		Specifies whether the file should be locked during the operation.
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void save(boolean lock) throws DatabaseFileException, IOException {
		if (mIndexEntry != 0)
			Database.getSingleton().saveSessionKeys(this, false);
	}

	public void setPhoneNumber(String phoneNumber) {
		this.mPhoneNumber = phoneNumber;
	}

	public String getPhoneNumber() {
		return mPhoneNumber;
	}

	void setSessionKey_Out(byte[] sessionKey_Out) {
		this.mSessionKey_Out = sessionKey_Out;
	}

	public byte[] getSessionKey_Out() {
		return mSessionKey_Out;
	}

	void setLastID_Out(byte lastID_Out) {
		this.mLastID_Out = lastID_Out;
	}

	public byte getLastID_Out() {
		return mLastID_Out;
	}

	void setSessionKey_In(byte[] sessionKey_In) {
		this.mSessionKey_In = sessionKey_In;
	}

	public byte[] getSessionKey_In() {
		return mSessionKey_In;
	}

	void setLastID_In(byte lastID_In) {
		this.mLastID_In = lastID_In;
	}

	public byte getLastID_In() {
		return mLastID_In;
	}

	public void setKeysSent(boolean keysSent) {
		this.mKeysSent = keysSent;
	}

	public boolean getKeysSent() {
		return mKeysSent;
	}

	public void setKeysConfirmed(boolean keysConfirmed) {
		this.mKeysConfirmed = keysConfirmed;
	}

	public boolean getKeysConfirmed() {
		return mKeysConfirmed;
	}

}
