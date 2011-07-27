package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.encryption.Encryption;

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
	private static final int OFFSET_LASTID_OUTGOING = OFFSET_SESSIONKEY_OUTGOING + LENGTH_SESSIONKEY;
	private static final int OFFSET_SESSIONKEY_INCOMING = OFFSET_LASTID_OUTGOING + LENGTH_LASTID;
	private static final int OFFSET_LASTID_INCOMING = OFFSET_SESSIONKEY_INCOMING + LENGTH_SESSIONKEY;
	
	private static final int OFFSET_RANDOMDATA = OFFSET_LASTID_INCOMING + LENGTH_LASTID;

	private static final int OFFSET_NEXTINDEX = Database.ENCRYPTED_ENTRY_SIZE - 4;
	private static final int OFFSET_PREVINDEX = OFFSET_NEXTINDEX - 4;
	private static final int OFFSET_PARENTINDEX = OFFSET_PREVINDEX - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_PARENTINDEX - OFFSET_RANDOMDATA;	
	
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
	static SessionKeys createSessionKeys(Conversation parent) throws DatabaseFileException, IOException {
		return createSessionKeys(parent, true);
	}
	
	/**
	 * Returns a new instance of the SessionKeys class, which replaces an empty entry in the file  
	 * @param lock		File lock allow
	 */
	static SessionKeys createSessionKeys(Conversation parent, boolean lockAllow) throws DatabaseFileException, IOException {
		SessionKeys keys = new SessionKeys(Empty.getEmptyIndex(lockAllow), false, lockAllow);
		parent.attachSessionKeys(keys, lockAllow);
		return keys;
	}

	/**
	 * Returns a new instance of the SessionKeys class, which represents a given entry in the file  
	 * @param index		Index in file
	 */
	static SessionKeys getSessionKeys(long index) throws DatabaseFileException, IOException {
		return getSessionKeys(index, true);
	}
	
	/**
	 * Returns a new instance of the SessionKeys class, which represents a given entry in the file  
	 * @param index		Index in file
	 * @param lock		File lock allow
	 */
	static SessionKeys getSessionKeys(long index, boolean lockAllow) throws DatabaseFileException, IOException {
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
	private boolean mSimSerial;
	private String mSimNumber; // or serial (based on the flag)
	private byte[] mSessionKey_Out;
	private byte mLastID_Out;
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
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	private SessionKeys(long index, boolean readFromFile) throws DatabaseFileException, IOException {
		this(index, readFromFile, true);
	}
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @param lockAllow		Allow the file to be locked
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	private SessionKeys(long index, boolean readFromFile, boolean lockAllow) throws DatabaseFileException, IOException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Database.getDatabase().getEntry(index, lockAllow);
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
			setSimSerial(simSerial);
			setSimNumber(Database.fromLatin(dataPlain, OFFSET_SIMNUMBER, LENGTH_SIMNUMBER));
			setSessionKey_Out(dataSessionKey_Out);
			setLastID_Out(dataPlain[OFFSET_LASTID_OUTGOING]);
			setSessionKey_In(dataSessionKey_In);
			setLastID_In(dataPlain[OFFSET_LASTID_INCOMING]);
			setIndexParent(Database.getInt(dataPlain, OFFSET_PARENTINDEX));
			setIndexPrev(Database.getInt(dataPlain, OFFSET_PREVINDEX));
			setIndexNext(Database.getInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			setKeysSent(false);
			setKeysConfirmed(false);
			setSimSerial(false);
			setSimNumber("");
			setSessionKey_Out(Encryption.generateRandomData(Encryption.KEY_LENGTH));
			setLastID_Out((byte) 0x00);
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
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void saveToFile() throws DatabaseFileException, IOException {
		saveToFile(true);
	}
	
	/**
	 * Saves contents of the class to the storage file
	 * @param lockAllow		Allow the file to be locked
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void saveToFile(boolean lock) throws DatabaseFileException, IOException {
		ByteBuffer keysBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		if (this.mKeysSent)
			flags |= (byte) ((1 << 7) & 0xFF);
		if (this.mKeysConfirmed)
			flags |= (byte) ((1 << 6) & 0xFF);
		if (this.mSimSerial)
			flags |= (byte) ((1 << 5) & 0xFF);
		keysBuffer.put(flags);
		
		// phone number
		keysBuffer.put(Database.toLatin(this.mSimNumber, LENGTH_SIMNUMBER));
		
		// session keys and last IDs
		keysBuffer.put(this.mSessionKey_Out);
		keysBuffer.put((byte) this.mLastID_Out);
		keysBuffer.put(this.mSessionKey_In);
		keysBuffer.put((byte) this.mLastID_In);
		
		// random data
		keysBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		keysBuffer.put(Database.getBytes(this.mIndexParent));
		keysBuffer.put(Database.getBytes(this.mIndexPrev));
		keysBuffer.put(Database.getBytes(this.mIndexNext));
		
		byte[] dataEncrypted = Encryption.encryptSymmetric(keysBuffer.array(), Encryption.retreiveEncryptionKey());
		Database.getDatabase().setEntry(mEntryIndex, dataEncrypted, lock);
	}

	/**
	 * Returns an instance of the Conversation class that is the parent of this SessionKeys in the data structure
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation getParent() throws DatabaseFileException, IOException {
		return getParent(true);
	}
	
	/**
	 * Returns an instance of the Conversation class that is the parent of this SessionKeys in the data structure
	 * @param lockAllow		Allow file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation getParent(boolean lockAllow) throws DatabaseFileException, IOException {
		if (mIndexParent == 0)
			return null;
		return Conversation.getConversation(mIndexParent, lockAllow);
	}
	
	/**
	 * Returns an instance of the predecessor in the list of session keys for parent conversation
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public SessionKeys getPreviousSessionKeys() throws DatabaseFileException, IOException {
		return getPreviousSessionKeys(true);
	}
	
	/**
	 * Returns an instance of the predecessor in the list of session keys for parent conversation
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public SessionKeys getPreviousSessionKeys(boolean lockAllow) throws DatabaseFileException, IOException {
		if (mIndexPrev == 0)
			return null;
		return getSessionKeys(mIndexPrev, lockAllow);
	}

	/**
	 * Returns an instance of the successor in the list of session keys for parent conversation
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public SessionKeys getNextSessionKeys() throws DatabaseFileException, IOException {
		return getNextSessionKeys(true);
	}
	
	/**
	 * Returns an instance of the predecessor in the list of session keys for parent conversation
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public SessionKeys getNextSessionKeys(boolean lockAllow) throws DatabaseFileException, IOException {
		if (mIndexNext == 0)
			return null;
		return getSessionKeys(mIndexNext, lockAllow);
	}
	
	/**
	 * Delete Message and all the MessageParts it controls
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void delete() throws DatabaseFileException, IOException {
		delete(true);
	}
	
	/**
	 * Delete Message and all the MessageParts it controls
	 * @param lockAllow 	Allow the file to be locked
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void delete(boolean lockAllow) throws DatabaseFileException, IOException {
		Database db = Database.getDatabase();
		
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
		} catch (DatabaseFileException ex) {
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
	 * @throws DatabaseFileException
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

	public boolean usesSimSerial() {
		return mSimSerial;
	}

	public void setSimSerial(boolean simSerial) {
		mSimSerial = simSerial;
	}

	public String getSimNumber() {
		return mSimNumber;
	}

	public void setSimNumber(String simNumber) {
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
	
	public byte getLastID_Out() {
		return mLastID_Out;
	}
	
	public void setLastID_Out(byte lastID_Out) {
		mLastID_Out = lastID_Out;
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
