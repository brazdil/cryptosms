package uk.ac.cam.db538.securesms.database;

import uk.ac.cam.db538.securesms.encryption.Encryption;
import java.io.IOException;
import android.text.format.Time;

/**
 * 
 * Class representing a conversation with a specific person
 * 
 * @author David Brazdil
 *
 */
public class Conversation implements Comparable<Conversation> {
	private long mIndexEntry; // READ ONLY
	private boolean mKeysExchanged;
	private String mPhoneNumber;
	private Time mTimeStamp;
	private byte[] mSessionKey_Out;
	private byte mLastID_Out;
	private byte[] mSessionKey_In;
	private byte mLastID_In;
	
	Conversation(long indexEntry) {
		if (indexEntry > 0xFFFFFFFFL || indexEntry <= 0L)
			throw new IndexOutOfBoundsException();
		
		mIndexEntry = indexEntry;
	}

	/**
	 * Hashes the session key for sending
	 */
	public void nextSessionKey_Out() {
		setSessionKey_Out(Encryption.getHash(getSessionKey_Out()));
	}

	/**
	 * Hashes the session key for receiving
	 */
	public void nextSessionKey_In() {
		setSessionKey_Out(Encryption.getHash(getSessionKey_In()));
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
			Database.getSingleton().updateConversation(this, lock);
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
			Database.getSingleton().saveConversation(this, false);
	}

	public void setKeysExchanged(boolean keysExchanged) {
		this.mKeysExchanged = keysExchanged;
	}

	public boolean getKeysExchanged() {
		return mKeysExchanged;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.mPhoneNumber = phoneNumber;
	}

	public String getPhoneNumber() {
		return mPhoneNumber;
	}

	public void setTimeStamp(Time timeStamp) {
		this.mTimeStamp = timeStamp;
	}

	public Time getTimeStamp() {
		return mTimeStamp;
	}
	
	public void setSessionKey_Out(byte[] sessionKey_Out) {
		this.mSessionKey_Out = sessionKey_Out;
	}

	public byte[] getSessionKey_Out() {
		return mSessionKey_Out;
	}

	public void setSessionKey_In(byte[] sessionKey_In) {
		this.mSessionKey_In = sessionKey_In;
	}

	public byte[] getSessionKey_In() {
		return mSessionKey_In;
	}

	public void setLastID_In(byte lastID_In) {
		this.mLastID_In = lastID_In;
	}

	public byte getLastID_In() {
		return mLastID_In;
	}

	public void setLastID_Out(byte lastID_Out) {
		this.mLastID_Out = lastID_Out;
	}

	public byte getLastID_Out() {
		return mLastID_Out;
	}

	@Override
	public int compareTo(Conversation another) {
		return Time.compare(this.getTimeStamp(), another.getTimeStamp());
	}
	
	
}
