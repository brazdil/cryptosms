package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.text.format.Time;

import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing a conversation entry in the secure storage file.
 * 
 * @author David Brazdil
 *
 */
public class Conversation {
	// FILE FORMAT
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_PHONENUMBER = 32;
	private static final int LENGTH_TIMESTAMP = 29;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_PHONENUMBER = OFFSET_FLAGS + LENGTH_FLAGS;
	private static final int OFFSET_TIMESTAMP = OFFSET_PHONENUMBER + LENGTH_PHONENUMBER;
	
	private static final int OFFSET_RANDOMDATA = OFFSET_TIMESTAMP + LENGTH_TIMESTAMP;

	private static final int OFFSET_NEXTINDEX = Database.ENCRYPTED_ENTRY_SIZE - 4;
	private static final int OFFSET_PREVINDEX = OFFSET_NEXTINDEX - 4;
	private static final int OFFSET_MSGSINDEX = OFFSET_PREVINDEX - 4;
	private static final int OFFSET_KEYSINDEX = OFFSET_MSGSINDEX - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_KEYSINDEX - OFFSET_RANDOMDATA;	
	
	// STATIC
	
	private static ArrayList<Conversation> cacheConversation = new ArrayList<Conversation>();
	
	/**
	 * Removes all instances from the list of cached objects.
	 * Be sure you don't use the instances afterwards.
	 */
	public static void forceClearCache() {
		synchronized (cacheConversation) {
			cacheConversation = new ArrayList<Conversation>();
		}
	}
	
	/**
	 * Returns instance of a new Conversation 
	 * @return
	 * @throws IOException 
	 * @throws DatabaseFileException 
	 */
	public static Conversation createConversation() throws DatabaseFileException, IOException {
		return createConversation(true);
	}

	/**
	 * Returns instance of a new Conversation
	 * @return
	 * @throws IOException 
	 * @throws DatabaseFileException 
	 */
	public static Conversation createConversation(boolean lockAllow) throws DatabaseFileException, IOException {
		// create a new one
		return new Conversation(Empty.getEmptyIndex(lockAllow), false, lockAllow);
	}	
	/**
	 * Returns an instance of Conversation class with given index in file.
	 * @param index		Index in file
	 */
	static Conversation getConversation(long index) throws DatabaseFileException, IOException {
		return getConversation(index, true);
	}
	
	/**
	 * Returns an instance of Conversation class with given index in file.
	 * @param index		Index in file
	 * @param lock		File lock allow
	 */
	static Conversation getConversation(long index, boolean lockAllow) throws DatabaseFileException, IOException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheConversation) {
			for (Conversation conv: cacheConversation)
				if (conv.getEntryIndex() == index)
					return conv; 
		}
		
		// create a new one
		return new Conversation(index, true, lockAllow);
	}
	
	/** 
	 * Explicitly requests each conversation in the file to be loaded to memory
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	public static void cacheAllConversations() throws IOException, DatabaseFileException {
		Conversation convCurrent = Header.getHeader(false).getFirstConversation();
		while (convCurrent != null) 
			convCurrent = convCurrent.getNextConversation();
	}
	
	// INTERNAL FIELDS
	private long mEntryIndex; // READ ONLY
	private String mPhoneNumber;
	private Time mTimeStamp;
	private long mIndexSessionKeys;
	private long mIndexMessages;
	private long mIndexPrev;
	private long mIndexNext;
	
	// CONSTRUCTORS
	
	private Conversation(long index, boolean readFromFile) throws DatabaseFileException, IOException {
		this(index, readFromFile, true);
	}
	
	private Conversation(long index, boolean readFromFile, boolean lockAllow) throws DatabaseFileException, IOException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Database.getDatabase().getEntry(index, lockAllow);
			byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
			
			Time timeStamp = new Time();
			timeStamp.parse3339(Database.fromLatin(dataPlain, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP));

			setPhoneNumber(Database.fromLatin(dataPlain, OFFSET_PHONENUMBER, LENGTH_PHONENUMBER));
			setTimeStamp(timeStamp);
			setIndexSessionKeys(Database.getInt(dataPlain, OFFSET_KEYSINDEX));
			setIndexMessages(Database.getInt(dataPlain, OFFSET_MSGSINDEX));
			setIndexPrev(Database.getInt(dataPlain, OFFSET_PREVINDEX));
			setIndexNext(Database.getInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			Time timeStamp = new Time();
			timeStamp.setToNow();
			
			setPhoneNumber("");
			setTimeStamp(timeStamp);
			setIndexSessionKeys(0L);
			setIndexMessages(0L);
			setIndexPrev(0L);
			setIndexNext(0L);
			
			saveToFile(lockAllow);
		}

		synchronized (cacheConversation) {
			cacheConversation.add(this);
		}
	}

	// FUNCTIONS
	
	public void saveToFile() throws DatabaseFileException, IOException {
		saveToFile(true);
	}
	
	public void saveToFile(boolean lock) throws DatabaseFileException, IOException {
		ByteBuffer convBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		convBuffer.put(flags);
		
		// phone number
		convBuffer.put(Database.toLatin(this.mPhoneNumber, LENGTH_PHONENUMBER));
		
		// time stamp
		convBuffer.put(Database.toLatin(this.mTimeStamp.format3339(false), LENGTH_TIMESTAMP));

		// random data
		convBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		convBuffer.put(Database.getBytes(this.mIndexSessionKeys)); 
		convBuffer.put(Database.getBytes(this.mIndexMessages)); 
		convBuffer.put(Database.getBytes(this.mIndexPrev));
		convBuffer.put(Database.getBytes(this.mIndexNext));
		
		byte[] dataEncrypted = Encryption.encryptSymmetric(convBuffer.array(), Encryption.retreiveEncryptionKey());
		Database.getDatabase().setEntry(this.mEntryIndex, dataEncrypted, lock);
	}

	public Conversation getPreviousConversation() throws DatabaseFileException, IOException {
		return getPreviousConversation(true);
	}
	
	public Conversation getPreviousConversation(boolean lockAllow) throws DatabaseFileException, IOException {
		return Conversation.getConversation(mIndexPrev, lockAllow);
	}

	public Conversation getNextConversation() throws DatabaseFileException, IOException {
		return getNextConversation(true);
	}
	
	public Conversation getNextConversation(boolean lockAllow) throws DatabaseFileException, IOException {
		return Conversation.getConversation(mIndexNext, lockAllow);
	}
	
	public void delete() {
		delete(true);
	}
	
	public void delete(boolean lock) {
		//TODO: To be implemented
	}
	
	public SessionKeys getFirstSessionKeys() throws DatabaseFileException, IOException {
		return getFirstSessionKeys(true);
	}
	
	public SessionKeys getFirstSessionKeys(boolean lockAllow) throws DatabaseFileException, IOException {
		return SessionKeys.getSessionKeys(mIndexSessionKeys, lockAllow);
	}

	public Message getFirstMessage() throws DatabaseFileException, IOException {
		return getFirstMessage(true);
	}
	
	public Message getFirstMessage(boolean lockAllow) throws DatabaseFileException, IOException {
		return Message.getMessage(mIndexMessages, lockAllow);
	}
	
	public ArrayList<Message> getMessages(boolean lockAllow) throws DatabaseFileException, IOException {
		ArrayList<Message> list = new ArrayList<Message>();
		Message msg = getFirstMessage(lockAllow);
		while (msg != null) {
			list.add(msg);
			msg = msg.getNextMessage(lockAllow);
		}
		return list;
	}
	
	// GETTERS / SETTERS
	long getEntryIndex() {
		return mEntryIndex;
	}
	
	public String getPhoneNumber() {
		return mPhoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.mPhoneNumber = phoneNumber;
	}

	public Time getTimeStamp() {
		return mTimeStamp;
	}

	public void setTimeStamp(Time timeStamp) {
		this.mTimeStamp = timeStamp;
	}

	long getIndexSessionKeys() {
		return mIndexSessionKeys;
	}

	void setIndexSessionKeys(long indexSessionKyes) {
		if (indexSessionKyes > 0xFFFFFFFFL || indexSessionKyes < 0L)
			throw new IndexOutOfBoundsException();
			
		this.mIndexSessionKeys = indexSessionKyes;
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
	
	Conversation getNextEmpty() throws DatabaseFileException, IOException {
		return Conversation.getConversation(mIndexNext);
	}
}
