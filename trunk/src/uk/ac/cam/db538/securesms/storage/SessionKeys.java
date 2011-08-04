package uk.ac.cam.db538.securesms.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.telephony.PhoneNumberUtils;

import uk.ac.cam.db538.securesms.Charset;
import uk.ac.cam.db538.securesms.Encryption;
import uk.ac.cam.db538.securesms.data.LowLevel;

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
	private static final int LENGTH_SESSIONKEY = Encryption.KEY_LENGTH;
	private static final int LENGTH_LASTID = 1;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_SIMNUMBER = OFFSET_FLAGS + LENGTH_FLAGS;
	private static final int OFFSET_SESSIONKEY_OUTGOING = OFFSET_SIMNUMBER + LENGTH_SIMNUMBER;
	private static final int OFFSET_NEXTID_OUTGOING = OFFSET_SESSIONKEY_OUTGOING + LENGTH_SESSIONKEY;
	private static final int OFFSET_SESSIONKEY_INCOMING = OFFSET_NEXTID_OUTGOING + LENGTH_LASTID;
	private static final int OFFSET_LASTID_INCOMING = OFFSET_SESSIONKEY_INCOMING + LENGTH_SESSIONKEY;
	
	private static final int OFFSET_RANDOMDATA = OFFSET_LASTID_INCOMING + LENGTH_LASTID;

	private static final int OFFSET_NEXTINDEX = Storage.ENCRYPTED_ENTRY_SIZE - 4;
	private static final int OFFSET_PREVINDEX = OFFSET_NEXTINDEX - 4;
	private static final int OFFSET_PARENTINDEX = OFFSET_PREVINDEX - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_PARENTINDEX - OFFSET_RANDOMDATA;	
	
	public static class SimNumber {
		private boolean mSerial;
		private String mNumber;
		
		public SimNumber() {
			setNumber("");
			setSerial(false);
		}
		
		public SimNumber(String number, boolean serial) {
			setNumber(number);
			setSerial(serial);
		}
		
		public void setSerial(boolean serial) {
			this.mSerial = serial;
		}
		
		public boolean isSerial() {
			return mSerial;
		}
		
		public void setNumber(String number) {
			this.mNumber = number;
		}
		
		public String getNumber() {
			return mNumber;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null)
				return false;
			
			try {
				SimNumber another = (SimNumber) o;
				if (this.mSerial == another.mSerial) {
					if (this.mSerial)
						return (this.mNumber.compareTo(another.mNumber) == 0);
					else
						return PhoneNumberUtils.compare(this.mNumber, another.mNumber);
				}
			} catch (Exception e) {
			}
			return false;
		}
		
		@Override
		public String toString() {
			return mNumber;
		}
	}
	
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
	public static SessionKeys createSessionKeys(Conversation parent) throws StorageFileException, IOException {
		return createSessionKeys(parent, true);
	}
	
	/**
	 * Returns a new instance of the SessionKeys class, which replaces an empty entry in the file  
	 * @param lock		File lock allow
	 */
	public static SessionKeys createSessionKeys(Conversation parent, boolean lockAllow) throws StorageFileException, IOException {
		SessionKeys keys = new SessionKeys(Empty.getEmptyIndex(lockAllow), false, lockAllow);
		parent.attachSessionKeys(keys, lockAllow);
		return keys;
	}

	/**
	 * Returns a new instance of the SessionKeys class, which represents a given entry in the file  
	 * @param index		Index in file
	 */
	static SessionKeys getSessionKeys(long index) throws StorageFileException, IOException {
		return getSessionKeys(index, true);
	}
	
	/**
	 * Returns a new instance of the SessionKeys class, which represents a given entry in the file  
	 * @param index		Index in file
	 * @param lock		File lock allow
	 */
	static SessionKeys getSessionKeys(long index, boolean lockAllow) throws StorageFileException, IOException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheSessionKeys) {
			for (SessionKeys keys: cacheSessionKeys)
				if (keys.getEntryIndex() == index)
					return keys; 
		}
		
		// create a new one
		return new SessionKeys(index, true, lockAllow);
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
	private long mIndexParent;
	private long mIndexPrev;
	private long mIndexNext;
	
	// CONSTRUCTORS
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @throws StorageFileException
	 * @throws IOException
	 */
	private SessionKeys(long index, boolean readFromFile) throws StorageFileException, IOException {
		this(index, readFromFile, true);
	}
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @param lockAllow		Allow the file to be locked
	 * @throws StorageFileException
	 * @throws IOException
	 */
	private SessionKeys(long index, boolean readFromFile, boolean lockAllow) throws StorageFileException, IOException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Storage.getDatabase().getEntry(index, lockAllow);
			byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());

			byte flags = dataPlain[OFFSET_FLAGS];
			boolean keysSent = ((flags & (1 << 7)) == 0) ? false : true;
			boolean keysConfirmed = ((flags & (1 << 6)) == 0) ? false : true;
			boolean simSerial = ((flags & (1 << 5)) == 0) ? false : true;

			byte[] dataSessionKey_Out = new byte[LENGTH_SESSIONKEY];
			System.arraycopy(dataPlain, OFFSET_SESSIONKEY_OUTGOING, dataSessionKey_Out, 0, LENGTH_SESSIONKEY);
			byte[] dataSessionKey_In = new byte[LENGTH_SESSIONKEY];
			System.arraycopy(dataPlain, OFFSET_SESSIONKEY_INCOMING, dataSessionKey_In, 0, LENGTH_SESSIONKEY);
			
			setKeysSent(keysSent);
			setKeysConfirmed(keysConfirmed);
			setSimNumber(new SimNumber(Charset.fromAscii8(dataPlain, OFFSET_SIMNUMBER, LENGTH_SIMNUMBER), simSerial));
			setSessionKey_Out(dataSessionKey_Out);
			setNextID_Out(dataPlain[OFFSET_NEXTID_OUTGOING]);
			setSessionKey_In(dataSessionKey_In);
			setLastID_In(dataPlain[OFFSET_LASTID_INCOMING]);
			setIndexParent(LowLevel.getUnsignedInt(dataPlain, OFFSET_PARENTINDEX));
			setIndexPrev(LowLevel.getUnsignedInt(dataPlain, OFFSET_PREVINDEX));
			setIndexNext(LowLevel.getUnsignedInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			setKeysSent(false);
			setKeysConfirmed(false);
			setSimNumber(new SimNumber());
			setSessionKey_Out(Encryption.generateRandomData(Encryption.KEY_LENGTH));
			setNextID_Out((byte) 0x00);
			setSessionKey_In(Encryption.generateRandomData(Encryption.KEY_LENGTH));
			setLastID_In((byte) 0x00);
			setIndexParent(0L);
			setIndexPrev(0L);
			setIndexNext(0L);
			
			saveToFile(lockAllow);
		}

		synchronized (cacheSessionKeys) {
			cacheSessionKeys.add(this);
		}
	}

	// FUNCTIONS

	/**
	 * Saves contents of the class to the storage file
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public void saveToFile() throws StorageFileException, IOException {
		saveToFile(true);
	}
	
	/**
	 * Saves contents of the class to the storage file
	 * @param lockAllow		Allow the file to be locked
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public void saveToFile(boolean lock) throws StorageFileException, IOException {
		ByteBuffer keysBuffer = ByteBuffer.allocate(Storage.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		if (this.mKeysSent)
			flags |= (byte) ((1 << 7) & 0xFF);
		if (this.mKeysConfirmed)
			flags |= (byte) ((1 << 6) & 0xFF);
		if (this.mSimNumber.isSerial())
			flags |= (byte) ((1 << 5) & 0xFF);
		keysBuffer.put(flags);
		
		// phone number
		keysBuffer.put(Charset.toAscii8(this.mSimNumber.getNumber(), LENGTH_SIMNUMBER));
		
		// session keys and last IDs
		keysBuffer.put(this.mSessionKey_Out);
		keysBuffer.put((byte) this.mNextID_Out);
		keysBuffer.put(this.mSessionKey_In);
		keysBuffer.put((byte) this.mLastID_In);
		
		// random data
		keysBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		keysBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexParent));
		keysBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexPrev));
		keysBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexNext));
		
		byte[] dataEncrypted = Encryption.encryptSymmetric(keysBuffer.array(), Encryption.retreiveEncryptionKey());
		Storage.getDatabase().setEntry(mEntryIndex, dataEncrypted, lock);
	}

	/**
	 * Returns an instance of the Conversation class that is the parent of this SessionKeys in the data structure
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public Conversation getParent() throws StorageFileException, IOException {
		return getParent(true);
	}
	
	/**
	 * Returns an instance of the Conversation class that is the parent of this SessionKeys in the data structure
	 * @param lockAllow		Allow file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public Conversation getParent(boolean lockAllow) throws StorageFileException, IOException {
		if (mIndexParent == 0)
			return null;
		return Conversation.getConversation(mIndexParent, lockAllow);
	}
	
	/**
	 * Returns an instance of the predecessor in the list of session keys for parent conversation
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public SessionKeys getPreviousSessionKeys() throws StorageFileException, IOException {
		return getPreviousSessionKeys(true);
	}
	
	/**
	 * Returns an instance of the predecessor in the list of session keys for parent conversation
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public SessionKeys getPreviousSessionKeys(boolean lockAllow) throws StorageFileException, IOException {
		if (mIndexPrev == 0)
			return null;
		return getSessionKeys(mIndexPrev, lockAllow);
	}

	/**
	 * Returns an instance of the successor in the list of session keys for parent conversation
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public SessionKeys getNextSessionKeys() throws StorageFileException, IOException {
		return getNextSessionKeys(true);
	}
	
	/**
	 * Returns an instance of the predecessor in the list of session keys for parent conversation
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public SessionKeys getNextSessionKeys(boolean lockAllow) throws StorageFileException, IOException {
		if (mIndexNext == 0)
			return null;
		return getSessionKeys(mIndexNext, lockAllow);
	}
	
	/**
	 * Delete Message and all the MessageParts it controls
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public void delete() throws StorageFileException, IOException {
		delete(true);
	}
	
	/**
	 * Delete Message and all the MessageParts it controls
	 * @param lockAllow 	Allow the file to be locked
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public void delete(boolean lockAllow) throws StorageFileException, IOException {
		Storage db = Storage.getDatabase();
		
		db.lockFile(lockAllow);
		try {
			SessionKeys prev = this.getPreviousSessionKeys(false);
			SessionKeys next = this.getNextSessionKeys(false); 
	
			if (prev != null) {
				// this is not the first message in the list
				// update the previous one
				prev.setIndexNext(this.getIndexNext());
				prev.saveToFile(false);
			} else {
				// this IS the first message in the list
				// update parent
				Conversation parent = this.getParent(false);
				parent.setIndexSessionKeys(this.getIndexNext());
				parent.saveToFile(false);
			}
			
			// update next one
			if (next != null) {
				next.setIndexPrev(this.getIndexPrev());
				next.saveToFile(false);
			}
			
			// delete this message
			Empty.replaceWithEmpty(mEntryIndex, false);
					
			// remove from cache
			synchronized (cacheSessionKeys) {
				cacheSessionKeys.remove(this);
			}
			
			// make this instance invalid
			this.mEntryIndex = -1L;
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			db.unlockFile(lockAllow);
		}
	}

	public enum SessionKeysStatus {
		SENDING_KEYS,
		SENDING_CONFIRMATION,
		WAITING_FOR_REPLY,
		KEYS_EXCHANGED
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
			if (mKeysConfirmed)
				return SessionKeysStatus.KEYS_EXCHANGED;
			else
				return SessionKeysStatus.WAITING_FOR_REPLY;
		}
		else {
			if (mKeysConfirmed)
				return SessionKeysStatus.SENDING_CONFIRMATION;
			else
				return SessionKeysStatus.SENDING_KEYS;
		}
	}
	
	public void incrementID_Out() {
		int id = LowLevel.getUnsignedByte(getNextID_Out());
		setNextID_Out(LowLevel.getBytesUnsignedByte(id++));
	}
	
	public void incrementID_In() {
		int id = LowLevel.getUnsignedByte(getLastID_In());
		setLastID_In(LowLevel.getBytesUnsignedByte(id++));
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
