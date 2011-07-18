package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import uk.ac.cam.db538.securesms.encryption.Encryption;
import android.text.format.Time;

public class Conversation {
	private long mIndexEntry; // READ ONLY
	private boolean mKeysExchanged;
	private String mPhoneNumber;
	private Time mTimeStamp;
	private byte[] mSessionKey_Out;
	private byte[] mSessionKey_In;
	
	Conversation() {
		this(0L);
	}

	Conversation(long indexEntry) {
		if (indexEntry > 0xFFFFFFFFL || indexEntry <= 0L)
			throw new IndexOutOfBoundsException();
		
		mIndexEntry = indexEntry;
	}

	public void nextSessionKey_Out() {
		setSessionKey_Out(Encryption.getHash(getSessionKey_Out()));
	}

	public void nextSessionKey_In() {
		setSessionKey_Out(Encryption.getHash(getSessionKey_In()));
	}

	long getIndexEntry() {
		return mIndexEntry;
	}
	
	public void update() throws DatabaseFileException, IOException {
		update(true);
	}
	
	/**
	 * 
	 * @param lock
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void update(boolean lock) throws DatabaseFileException, IOException {
		if (mIndexEntry != 0)
			Database.getSingleton().updateConversation(this, lock);
	}
	
	public void save() throws DatabaseFileException, IOException {
		save(true);
	}
	
	public void save(boolean lock) throws DatabaseFileException, IOException {
		if (mIndexEntry != 0)
			Database.getSingleton().saveConversation(this, false);
	}

	public void setKeysExchanged(boolean mKeysExchanged) {
		this.mKeysExchanged = mKeysExchanged;
	}

	public boolean getKeysExchanged() {
		return mKeysExchanged;
	}

	public void setPhoneNumber(String mPhoneNumber) {
		this.mPhoneNumber = mPhoneNumber;
	}

	public String getPhoneNumber() {
		return mPhoneNumber;
	}

	public void setTimeStamp(Time mTimeStamp) {
		this.mTimeStamp = mTimeStamp;
	}

	public Time getTimeStamp() {
		return mTimeStamp;
	}
	
	public void setSessionKey_Out(byte[] mSessionKey_Out) {
		this.mSessionKey_Out = mSessionKey_Out;
	}

	public byte[] getSessionKey_Out() {
		return mSessionKey_Out;
	}

	public void setSessionKey_In(byte[] mSessionKey_In) {
		this.mSessionKey_In = mSessionKey_In;
	}

	public byte[] getSessionKey_In() {
		return mSessionKey_In;
	}
	
	
}
