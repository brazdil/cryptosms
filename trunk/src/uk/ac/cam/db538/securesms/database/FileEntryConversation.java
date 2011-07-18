package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import android.text.format.Time;

import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing a conversation entry in the secure storage file.
 * Not to be used outside the package.
 * 
 * @author David Brazdil
 *
 */
class FileEntryConversation {
	
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_PHONENUMBER = 32;
	private static final int LENGTH_TIMESTAMP = 29;
	private static final int LENGTH_LASTID = 1;
	private static final int LENGTH_SESSIONKEY = Encryption.KEY_LENGTH;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_PHONENUMBER = OFFSET_FLAGS + LENGTH_FLAGS;
	private static final int OFFSET_TIMESTAMP = OFFSET_PHONENUMBER + LENGTH_PHONENUMBER;
	private static final int OFFSET_SESSIONKEY_OUTGOING = OFFSET_TIMESTAMP + LENGTH_TIMESTAMP;
	private static final int OFFSET_LASTID_OUTGOING = OFFSET_SESSIONKEY_OUTGOING + LENGTH_SESSIONKEY;
	private static final int OFFSET_SESSIONKEY_INCOMING = OFFSET_LASTID_OUTGOING + LENGTH_LASTID;
	private static final int OFFSET_LASTID_INCOMING = OFFSET_SESSIONKEY_INCOMING + LENGTH_SESSIONKEY;
	
	private static final int OFFSET_RANDOMDATA = OFFSET_LASTID_INCOMING + LENGTH_LASTID;

	private static final int OFFSET_NEXTINDEX = Database.ENCRYPTED_ENTRY_SIZE - 4;
	private static final int OFFSET_PREVINDEX = OFFSET_NEXTINDEX - 4;
	private static final int OFFSET_MSGSINDEX = OFFSET_PREVINDEX - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_MSGSINDEX - OFFSET_RANDOMDATA;	

	private boolean mKeysExchanged;
	private String mPhoneNumber;
	private Time mTimeStamp;
	private byte[] mSessionKey_Out;
	private byte mLastID_Out;
	private byte[] mSessionKey_In;
	private byte mLastID_In;
	private long mIndexMessages;
	private long mIndexPrev;
	private long mIndexNext;
	
	FileEntryConversation(boolean keysExchanged, String phoneNumber, Time timeStamp, byte[] sessionKey_Out, byte lastID_Out, byte[] sessionKey_In, byte lastID_In, long indexMessages, long indexPrev, long indexNext) {
		setKeysExchanged(keysExchanged);
		setPhoneNumber(phoneNumber);
		setTimeStamp(timeStamp);
		setSessionKey_Out(sessionKey_Out);
		setLastID_Out(lastID_Out);
		setSessionKey_In(sessionKey_In);
		setLastID_In(lastID_In);
		setIndexMessages(indexMessages);
		setIndexPrev(indexPrev);
		setIndexNext(indexNext);
	}
	
	boolean getKeysExchanged() {
		return mKeysExchanged;
	}

	void setKeysExchanged(boolean keysExchanged) {
		mKeysExchanged = keysExchanged;
	}

	String getPhoneNumber() {
		return mPhoneNumber;
	}

	void setPhoneNumber(String phoneNumber) {
		this.mPhoneNumber = phoneNumber;
	}

	Time getTimeStamp() {
		return mTimeStamp;
	}

	void setTimeStamp(Time timeStamp) {
		this.mTimeStamp = timeStamp;
	}

	byte[] getSessionKey_Out() {
		return mSessionKey_Out;
	}

	void setSessionKey_Out(byte[] sessionKeyOut) {
		mSessionKey_Out = sessionKeyOut;
	}

	byte[] getSessionKey_In() {
		return mSessionKey_In;
	}

	void setSessionKey_In(byte[] sessionKeyIn) {
		mSessionKey_In = sessionKeyIn;
	}
	
	byte getLastID_Out() {
		return mLastID_Out;
	}
	
	void setLastID_Out(byte lastID_Out) {
		mLastID_Out = lastID_Out;
	}

	byte getLastID_In() {
		return mLastID_In;
	}
	
	void setLastID_In(byte lastID_In) {
		mLastID_In = lastID_In;
	}

	long getIndexMessages() {
		return mIndexMessages;
	}

	void setIndexMessages(long indexMessages) {
		if (indexMessages > 0xFFFFFFFFL || indexMessages < 0L)
			throw new IndexOutOfBoundsException();
			
		this.mIndexMessages = indexMessages;
	}

	long getIndexPrev() {
		return mIndexPrev;
	}

	void setIndexPrev(long indexPrev) {
		if (indexPrev > 0xFFFFFFFFL || indexPrev < 0L)
			throw new IndexOutOfBoundsException();
		
		this.mIndexPrev = indexPrev;
	}

	long getIndexNext() {
		return mIndexNext;
	}

	void setIndexNext(long indexNext) {
	    if (indexNext > 0xFFFFFFFFL || indexNext < 0L)
	    	throw new IndexOutOfBoundsException();
		
		this.mIndexNext = indexNext;
	}

	static byte[] createData(FileEntryConversation conversation) throws DatabaseFileException {
		ByteBuffer convBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		if (conversation.mKeysExchanged)
			flags |= (byte) ((1 << 7) & 0xFF);
		convBuffer.put(flags);
		
		// phone number
		convBuffer.put(Database.toLatin(conversation.mPhoneNumber, LENGTH_PHONENUMBER));
		
		// time stamp
		convBuffer.put(Database.toLatin(conversation.mTimeStamp.format3339(false), LENGTH_TIMESTAMP));

		// session keys and last IDs
		convBuffer.put(conversation.mSessionKey_Out);
		convBuffer.put((byte) conversation.mLastID_Out);
		convBuffer.put(conversation.mSessionKey_In);
		convBuffer.put((byte) conversation.mLastID_In);
		
		// random data
		convBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		convBuffer.put(Database.getBytes(conversation.mIndexMessages)); 
		convBuffer.put(Database.getBytes(conversation.mIndexPrev));
		convBuffer.put(Database.getBytes(conversation.mIndexNext));
		
		return Encryption.encryptSymmetric(convBuffer.array(), Encryption.retreiveEncryptionKey());
	}
	
	static FileEntryConversation parseData(byte[] dataEncrypted) throws DatabaseFileException {
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		byte flags = dataPlain[OFFSET_FLAGS];
		boolean keysExchanged = ((flags & (1 << 7)) == 0) ? false : true;

		byte[] dataSessionKey_Out = new byte[LENGTH_SESSIONKEY];
		System.arraycopy(dataPlain, OFFSET_SESSIONKEY_OUTGOING, dataSessionKey_Out, 0, LENGTH_SESSIONKEY);
		byte[] dataSessionKey_In = new byte[LENGTH_SESSIONKEY];
		System.arraycopy(dataPlain, OFFSET_SESSIONKEY_INCOMING, dataSessionKey_In, 0, LENGTH_SESSIONKEY);

		Time timeStamp = new Time();
		timeStamp.parse3339(Database.fromLatin(dataPlain, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP));

		return new FileEntryConversation(keysExchanged,
		                                 Database.fromLatin(dataPlain, OFFSET_PHONENUMBER, LENGTH_PHONENUMBER),
		                                 timeStamp, 
		                                 dataSessionKey_Out, 
		                                 dataPlain[OFFSET_LASTID_OUTGOING],
		                                 dataSessionKey_In, 
		                                 dataPlain[OFFSET_LASTID_INCOMING],
		                                 Database.getInt(dataPlain, OFFSET_MSGSINDEX), 
		                                 Database.getInt(dataPlain, OFFSET_PREVINDEX),
		                                 Database.getInt(dataPlain, OFFSET_NEXTINDEX)
		                                 );
	}
}
