package uk.ac.cam.db538.securesms.database;

import java.io.IOException;

import uk.ac.cam.db538.securesms.encryption.Encryption;

import android.text.format.Time;

public class Conversation {
	private long mIndexEntry;
	private boolean mKeysExchanged;
	private String mPhoneNumber;
	private Time mTimeStamp;
	private byte[] mSessionKey_Out;
	private byte[] mSessionKey_In;
	
	Conversation() throws DatabaseFileException, IOException {
		this(0L);
	}

	Conversation(long indexEntry) throws DatabaseFileException, IOException {
		setIndexEntry(indexEntry);
	}

	public void nextSessionKey_Out() {
		setSessionKey_Out(Encryption.getHash(getSessionKey_Out()));
	}

	public void nextSessionKey_In() {
		setSessionKey_Out(Encryption.getHash(getSessionKey_In()));
	}

	void setIndexEntry(long indexEntry) throws DatabaseFileException, IOException {
		this.mIndexEntry = indexEntry;
		if (mIndexEntry != 0)
			update();
	}

	long getIndexEntry() {
		return mIndexEntry;
	}
	
	public void update() throws DatabaseFileException, IOException {
		Database.getSingleton().updateConversation(this);
	}
	
	public void save() throws DatabaseFileException, IOException {
		Database.getSingleton().saveConversation(this);
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
