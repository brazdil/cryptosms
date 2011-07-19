package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;
import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing a session keys entry in the secure storage file.
 * Not to be used outside the package.
 * 
 * @author David Brazdil
 *
 */
class FileEntrySessionKeys {
	
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_PHONENUMBER = 32;
	private static final int LENGTH_SESSIONKEY = Encryption.KEY_LENGTH;
	private static final int LENGTH_LASTID = 1;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_PHONENUMBER = OFFSET_FLAGS + LENGTH_FLAGS;
	private static final int OFFSET_SESSIONKEY_OUTGOING = OFFSET_PHONENUMBER + LENGTH_PHONENUMBER;
	private static final int OFFSET_LASTID_OUTGOING = OFFSET_SESSIONKEY_OUTGOING + LENGTH_SESSIONKEY;
	private static final int OFFSET_SESSIONKEY_INCOMING = OFFSET_LASTID_OUTGOING + LENGTH_LASTID;
	private static final int OFFSET_LASTID_INCOMING = OFFSET_SESSIONKEY_INCOMING + LENGTH_SESSIONKEY;
	
	private static final int OFFSET_RANDOMDATA = OFFSET_LASTID_INCOMING + LENGTH_LASTID;

	private static final int OFFSET_NEXTINDEX = Database.ENCRYPTED_ENTRY_SIZE - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_NEXTINDEX - OFFSET_RANDOMDATA;	

	private boolean mKeysSent;
	private boolean mKeysConfirmed;
	private String mPhoneNumber;
	private byte[] mSessionKey_Out;
	private byte mLastID_Out;
	private byte[] mSessionKey_In;
	private byte mLastID_In;
	private long mIndexNext;
	
	FileEntrySessionKeys(boolean keysSent, boolean keysConfirmed, String phoneNumber, byte[] sessionKey_Out, byte lastID_Out, byte[] sessionKey_In, byte lastID_In, long indexNext) {
		setKeysSent(keysSent);
		setKeysConfirmed(keysConfirmed);
		setPhoneNumber(phoneNumber);
		setSessionKey_Out(sessionKey_Out);
		setLastID_Out(lastID_Out);
		setSessionKey_In(sessionKey_In);
		setLastID_In(lastID_In);
		setIndexNext(indexNext);
	}

	boolean getKeysSent() {
		return mKeysSent;
	}

	void setKeysSent(boolean keysSent) {
		mKeysSent = keysSent;
	}

	boolean getKeysConfirmed() {
		return mKeysConfirmed;
	}

	void setKeysConfirmed(boolean keysConfirmed) {
		mKeysConfirmed = keysConfirmed;
	}

	String getPhoneNumber() {
		return mPhoneNumber;
	}

	void setPhoneNumber(String phoneNumber) {
		this.mPhoneNumber = phoneNumber;
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

	long getIndexNext() {
		return mIndexNext;
	}

	void setIndexNext(long indexNext) {
	    if (indexNext > 0xFFFFFFFFL || indexNext < 0L)
	    	throw new IndexOutOfBoundsException();
		
		this.mIndexNext = indexNext;
	}

	static byte[] createData(FileEntrySessionKeys keys) throws DatabaseFileException {
		ByteBuffer keysBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		if (keys.mKeysSent)
			flags |= (byte) ((1 << 7) & 0xFF);
		if (keys.mKeysConfirmed)
			flags |= (byte) ((1 << 6) & 0xFF);
		keysBuffer.put(flags);
		
		// phone number
		keysBuffer.put(Database.toLatin(keys.mPhoneNumber, LENGTH_PHONENUMBER));
		
		// session keys and last IDs
		keysBuffer.put(keys.mSessionKey_Out);
		keysBuffer.put((byte) keys.mLastID_Out);
		keysBuffer.put(keys.mSessionKey_In);
		keysBuffer.put((byte) keys.mLastID_In);
		
		// random data
		keysBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		keysBuffer.put(Database.getBytes(keys.mIndexNext));
		
		return Encryption.encryptSymmetric(keysBuffer.array(), Encryption.retreiveEncryptionKey());
	}
	
	static FileEntrySessionKeys parseData(byte[] dataEncrypted) throws DatabaseFileException {
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());

		byte flags = dataPlain[OFFSET_FLAGS];
		boolean keysSent = ((flags & (1 << 7)) == 0) ? false : true;
		boolean keysConfirmed = ((flags & (1 << 6)) == 0) ? false : true;

		byte[] dataSessionKey_Out = new byte[LENGTH_SESSIONKEY];
		System.arraycopy(dataPlain, OFFSET_SESSIONKEY_OUTGOING, dataSessionKey_Out, 0, LENGTH_SESSIONKEY);
		byte[] dataSessionKey_In = new byte[LENGTH_SESSIONKEY];
		System.arraycopy(dataPlain, OFFSET_SESSIONKEY_INCOMING, dataSessionKey_In, 0, LENGTH_SESSIONKEY);

		return new FileEntrySessionKeys(keysSent,
		                                keysConfirmed,
		                                Database.fromLatin(dataPlain, OFFSET_PHONENUMBER, LENGTH_PHONENUMBER),
		                                dataSessionKey_Out, 
		                                dataPlain[OFFSET_LASTID_OUTGOING],
		                                dataSessionKey_In, 
		                                dataPlain[OFFSET_LASTID_INCOMING],
		                                Database.getInt(dataPlain, OFFSET_NEXTINDEX)
		                                );
	}
}
