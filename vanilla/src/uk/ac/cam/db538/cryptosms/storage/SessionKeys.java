package uk.ac.cam.db538.cryptosms.storage;

import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.crypto.EllipticCurveDeffieHellman;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.utils.Charset;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import uk.ac.cam.db538.cryptosms.utils.SimNumber;

/**
 * 
 * Class representing a session keys entry in the secure storage file.
 * 
 * @author David Brazdil
 *
 */
public class SessionKeys {
	// FILE FORMAT
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_SIMNUMBER = 32;
	private static final int LENGTH_SESSIONKEY = Encryption.SYM_KEY_LENGTH;
	private static final int LENGTH_PRIVATE_KEY = EllipticCurveDeffieHellman.LENGTH_PRIVATE_KEY; 
	private static final int LENGTH_ID = 1;
	private static final int LENGTH_TIMESTAMP = 8;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_SIMNUMBER = OFFSET_FLAGS + LENGTH_FLAGS;
	private static final int OFFSET_SESSIONKEY_OUTGOING = OFFSET_SIMNUMBER + LENGTH_SIMNUMBER;
	private static final int OFFSET_NEXTID_OUTGOING = OFFSET_SESSIONKEY_OUTGOING + LENGTH_SESSIONKEY;
	private static final int OFFSET_SESSIONKEY_INCOMING = OFFSET_NEXTID_OUTGOING + LENGTH_ID;
	private static final int OFFSET_LASTID_INCOMING = OFFSET_SESSIONKEY_INCOMING + LENGTH_SESSIONKEY;
	private static final int OFFSET_PRIVATE_KEY = OFFSET_LASTID_INCOMING + LENGTH_ID;
	private static final int OFFSET_TIMESTAMP = OFFSET_PRIVATE_KEY + LENGTH_PRIVATE_KEY;
	
	private static final int OFFSET_RANDOMDATA = OFFSET_TIMESTAMP + LENGTH_TIMESTAMP;

	private static final int OFFSET_NEXTINDEX = Storage.ENCRYPTED_ENTRY_SIZE - 4;
	private static final int OFFSET_PREVINDEX = OFFSET_NEXTINDEX - 4;
	private static final int OFFSET_PARENTINDEX = OFFSET_PREVINDEX - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_PARENTINDEX - OFFSET_RANDOMDATA;
	
	public static final byte DEFAULT_ID = 0x00;
		
	// STATIC
	
	private static ArrayList<SessionKeys> cacheSessionKeys = new ArrayList<SessionKeys>();
	
	/**
	 * Removes all instances from the list of cached objects.
	 * Be sure you don't use the instances afterwards.
	 */
	public static void forceClearCache() {
		synchronized (cacheSessionKeys) {
			cacheSessionKeys = new ArrayList<SessionKeys>();
		}
	}

	/**
	 * Returns a new instance of the SessionKeys class, which replaces an empty entry in the file  
	 */
	public static SessionKeys createSessionKeys(Conversation parent) throws StorageFileException {
		SessionKeys keys = new SessionKeys(Empty.getEmptyIndex(), false);
		parent.attachSessionKeys(keys);
		return keys;
	}

	/**
	 * Returns a new instance of the SessionKeys class, which represents a given entry in the file  
	 * @param index		Index in file
	 */
	static SessionKeys getSessionKeys(long index) throws StorageFileException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheSessionKeys) {
			for (SessionKeys keys: cacheSessionKeys)
				if (keys.getEntryIndex() == index)
					return keys; 
		}
		
		// create a new one
		return new SessionKeys(index, true);
	}

	// INTERNAL FIELDS
	private long mEntryIndex; // READ ONLY
	private boolean mKeysSent;
	private boolean mKeysConfirmed;
	private SimNumber mSimNumber;
	private byte[] mSessionKey_Out;
	private byte mNextID_Out;
	private byte[] mSessionKey_In;
	private byte mLastID_In;
	private byte[] mPrivateKey;
	private long mTimeStamp;
	private long mIndexParent;
	private long mIndexPrev;
	private long mIndexNext;
	
	// CONSTRUCTORS
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @throws StorageFileException
	 */
	private SessionKeys(long index, boolean readFromFile) throws StorageFileException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Storage.getStorage().getEntry(index);
			byte[] dataPlain;
			try {
				dataPlain = Encryption.getEncryption().decryptSymmetricWithMasterKey(dataEncrypted);
			} catch (EncryptionException e) {
				throw new StorageFileException(e);
			}

			byte flags = dataPlain[OFFSET_FLAGS];
			boolean keysSent = ((flags & (1 << 7)) == 0) ? false : true;
			boolean keysConfirmed = ((flags & (1 << 6)) == 0) ? false : true;
			boolean simSerial = ((flags & (1 << 5)) == 0) ? false : true;

			setKeysSent(keysSent);
			setKeysConfirmed(keysConfirmed);
			setSimNumber(new SimNumber(Charset.fromAscii8(dataPlain, OFFSET_SIMNUMBER, LENGTH_SIMNUMBER), simSerial));
			setSessionKey_Out(LowLevel.cutData(dataPlain, OFFSET_SESSIONKEY_OUTGOING, LENGTH_SESSIONKEY));
			setNextID_Out(dataPlain[OFFSET_NEXTID_OUTGOING]);
			setSessionKey_In(LowLevel.cutData(dataPlain, OFFSET_SESSIONKEY_INCOMING, LENGTH_SESSIONKEY));
			setLastID_In(dataPlain[OFFSET_LASTID_INCOMING]);
			setPrivateKey(LowLevel.cutData(dataPlain, OFFSET_PRIVATE_KEY, LENGTH_PRIVATE_KEY));
			setTimeStamp(LowLevel.getLong(LowLevel.cutData(dataPlain, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP)));
			setIndexParent(LowLevel.getUnsignedInt(dataPlain, OFFSET_PARENTINDEX));
			setIndexPrev(LowLevel.getUnsignedInt(dataPlain, OFFSET_PREVINDEX));
			setIndexNext(LowLevel.getUnsignedInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			setKeysSent(false);
			setKeysConfirmed(false);
			setSimNumber(new SimNumber());
			setSessionKey_Out(Encryption.getEncryption().generateRandomData(LENGTH_SESSIONKEY));
			setNextID_Out(DEFAULT_ID);
			setSessionKey_In(Encryption.getEncryption().generateRandomData(LENGTH_SESSIONKEY));
			setLastID_In(DEFAULT_ID);
			setPrivateKey(Encryption.getEncryption().generateRandomData(LENGTH_PRIVATE_KEY));
			setTimeStamp(0L);
			setIndexParent(0L);
			setIndexPrev(0L);
			setIndexNext(0L);
			
			saveToFile();
		}

		synchronized (cacheSessionKeys) {
			cacheSessionKeys.add(this);
		}
	}

	// FUNCTIONS

	/**
	 * Saves contents of the class to the storage file
	 * @throws StorageFileException
	 */
	public void saveToFile() throws StorageFileException {
		byte[] keysBuffer = new byte[Storage.ENCRYPTED_ENTRY_SIZE];
		
		// flags
		byte flags = 0;
		if (this.mKeysSent)
			flags |= (byte) ((1 << 7) & 0xFF);
		if (this.mKeysConfirmed)
			flags |= (byte) ((1 << 6) & 0xFF);
		if (this.mSimNumber.isSerial())
			flags |= (byte) ((1 << 5) & 0xFF);
		keysBuffer[OFFSET_FLAGS] = flags;
		
		// phone number
		System.arraycopy(Charset.toAscii8(this.mSimNumber.getNumber(), LENGTH_SIMNUMBER), 0, keysBuffer, OFFSET_SIMNUMBER, LENGTH_SIMNUMBER);
		
		// session keys and last IDs and confirmation nonce
		System.arraycopy(this.mSessionKey_Out, 0, keysBuffer, OFFSET_SESSIONKEY_OUTGOING, LENGTH_SESSIONKEY);
		keysBuffer[OFFSET_NEXTID_OUTGOING] = (byte) this.mNextID_Out; 
		System.arraycopy(this.mSessionKey_In, 0, keysBuffer, OFFSET_SESSIONKEY_INCOMING, LENGTH_SESSIONKEY);
		keysBuffer[OFFSET_LASTID_INCOMING] = (byte) this.mLastID_In; 
		System.arraycopy(this.mPrivateKey, 0, keysBuffer, OFFSET_PRIVATE_KEY, LENGTH_PRIVATE_KEY);
		System.arraycopy(LowLevel.getBytesLong(this.mTimeStamp), 0, keysBuffer, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP);
		
		// random data
		System.arraycopy(Encryption.getEncryption().generateRandomData(LENGTH_RANDOMDATA), 0, keysBuffer, OFFSET_RANDOMDATA, LENGTH_RANDOMDATA);
		
		// indices
		System.arraycopy(LowLevel.getBytesUnsignedInt(this.mIndexParent), 0, keysBuffer, OFFSET_PARENTINDEX, 4);
		System.arraycopy(LowLevel.getBytesUnsignedInt(this.mIndexPrev), 0, keysBuffer, OFFSET_PREVINDEX, 4);
		System.arraycopy(LowLevel.getBytesUnsignedInt(this.mIndexNext), 0, keysBuffer, OFFSET_NEXTINDEX, 4);
		
		// encrypt and save
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = Encryption.getEncryption().encryptSymmetricWithMasterKey(keysBuffer);
		} catch (EncryptionException e) {
			throw new StorageFileException(e);
		}
		Storage.getStorage().setEntry(mEntryIndex, dataEncrypted);
	}

	/**
	 * Returns an instance of the Conversation class that is the parent of this SessionKeys in the data structure
	 * @return
	 * @throws StorageFileException
	 */
	public Conversation getParent() throws StorageFileException {
		if (mIndexParent == 0)
			return null;
		return Conversation.getConversation(mIndexParent);
	}
	
	/**
	 * Returns an instance of the predecessor in the list of session keys for parent conversation
	 * @return
	 * @throws StorageFileException
	 */
	public SessionKeys getPreviousSessionKeys() throws StorageFileException {
		if (mIndexPrev == 0)
			return null;
		return getSessionKeys(mIndexPrev);
	}

	/**
	 * Returns an instance of the successor in the list of session keys for parent conversation
	 * @return
	 * @throws StorageFileException
	 */
	public SessionKeys getNextSessionKeys() throws StorageFileException {
		if (mIndexNext == 0)
			return null;
		return getSessionKeys(mIndexNext);
	}
	
	/**
	 * Delete Message and all the MessageParts it controls
	 * @throws StorageFileException
	 */
	public void delete() throws StorageFileException {
		SessionKeys prev = this.getPreviousSessionKeys();
		SessionKeys next = this.getNextSessionKeys(); 

		if (prev != null) {
			// this is not the first message in the list
			// update the previous one
			prev.setIndexNext(this.getIndexNext());
			prev.saveToFile();
		} else {
			// this IS the first message in the list
			// update parent
			Conversation parent = this.getParent();
			parent.setIndexSessionKeys(this.getIndexNext());
			parent.saveToFile();
		}
		
		// update next one
		if (next != null) {
			next.setIndexPrev(this.getIndexPrev());
			next.saveToFile();
		}
		
		// delete this message
		Empty.replaceWithEmpty(mEntryIndex);
				
		// remove from cache
		synchronized (cacheSessionKeys) {
			cacheSessionKeys.remove(this);
		}
		
		// make this instance invalid
		this.mEntryIndex = -1L;
	}

	public enum SessionKeysStatus {
		SENDING_KEYS,
		SENDING_CONFIRMATION,
		WAITING_FOR_REPLY,
		KEYS_EXCHANGED,
		KEYS_EXPIRED
	}
	
	/**
	 * Returns the status of session keys exchange 
	 * @param simNumber
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public SessionKeysStatus getStatus() {
		if (mKeysSent) {
			if (mKeysConfirmed) {
				if (LowLevel.getUnsignedByte(mNextID_Out) < 255)
					return SessionKeysStatus.KEYS_EXCHANGED;
				else
					return SessionKeysStatus.KEYS_EXPIRED;
			} else
				return SessionKeysStatus.WAITING_FOR_REPLY;
		} else {
			if (mKeysConfirmed)
				return SessionKeysStatus.SENDING_CONFIRMATION;
			else
				return SessionKeysStatus.SENDING_KEYS;
		}
	}
	
	public void incrementOut(int count) {
		for (int i = 0; i < count && LowLevel.getUnsignedByte(getNextID_Out()) < 255; ++i) {
			int id = LowLevel.getUnsignedByte(getNextID_Out()) + 1;
			setNextID_Out(LowLevel.getBytesUnsignedByte(id));
			setSessionKey_Out(Encryption.getEncryption().getHash(getSessionKey_Out()));
		}
	}
	
	public void incrementIn(int count) {
		for (int i = 0; i < count && LowLevel.getUnsignedByte(getLastID_In()) < 255; ++i) {
			int id = LowLevel.getUnsignedByte(getLastID_In()) + 1;
			setLastID_In(LowLevel.getBytesUnsignedByte(id));
			setSessionKey_In(Encryption.getEncryption().getHash(getSessionKey_In()));
		}
	}

	// GETTERS / SETTERS
	
	long getEntryIndex() {
		return mEntryIndex;
	}
	
	public boolean getKeysSent() {
		return mKeysSent;
	}

	public void setKeysSent(boolean keysSent) {
		mKeysSent = keysSent;
	}

	public boolean getKeysConfirmed() {
		return mKeysConfirmed;
	}

	public void setKeysConfirmed(boolean keysConfirmed) {
		mKeysConfirmed = keysConfirmed;
	}

	public SimNumber getSimNumber() {
		return mSimNumber;
	}

	public void setSimNumber(SimNumber simNumber) {
		this.mSimNumber = simNumber;
	}

	public byte[] getSessionKey_Out() {
		return mSessionKey_Out;
	}

	public void setSessionKey_Out(byte[] sessionKeyOut) {
		mSessionKey_Out = sessionKeyOut;
	}

	public byte[] getSessionKey_In() {
		return mSessionKey_In;
	}

	public void setSessionKey_In(byte[] sessionKeyIn) {
		mSessionKey_In = sessionKeyIn;
	}
	
	public byte getNextID_Out() {
		return mNextID_Out;
	}
	
	public void setNextID_Out(byte nextID_Out) {
		mNextID_Out = nextID_Out;
	}

	public byte getLastID_In() {
		return mLastID_In;
	}
	
	public void setLastID_In(byte lastID_In) {
		mLastID_In = lastID_In;
	}

	public byte[] getPrivateKey() {
		return mPrivateKey;
	}

	public void setPrivateKey(byte[] privateKey) {
		mPrivateKey = privateKey;
	}

	public long getTimeStamp() {
		return mTimeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		mTimeStamp = timeStamp;
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

	void setIndexParent(long indexParent) {
		this.mIndexParent = indexParent;
	}

	long getIndexParent() {
		return mIndexParent;
	}
}
