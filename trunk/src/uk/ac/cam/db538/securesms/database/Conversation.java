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
	 * Returns instance of a new Conversation created in one of the empty spaces in file.
	 * @return
	 * @throws IOException 
	 * @throws DatabaseFileException 
	 */
	public static Conversation createConversation() throws DatabaseFileException, IOException {
		return createConversation(true);
	}

	/**
	 * Returns instance of a new Conversation created in one of the empty spaces in file.
	 * @lockAllow		Allow to lock the file
	 * @return
	 * @throws IOException 
	 * @throws DatabaseFileException 
	 */
	public static Conversation createConversation(boolean lockAllow) throws DatabaseFileException, IOException {
		// create a new one
		Conversation conv = new Conversation(Empty.getEmptyIndex(lockAllow), false, lockAllow);
		Header.getHeader(lockAllow).attachConversation(conv, lockAllow);
		return conv;
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
	
	/**
	 * Saves data to the storage file.
	 */
	public void saveToFile() throws DatabaseFileException, IOException {
		saveToFile(true);
	}
	
	/**
	 * Saves data to the storage file.
	 * @param lock			Allow the file to be locked.
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
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

	/**
	 * Returns previous instance of Conversation in the double-linked list or null if this is the first.
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation getPreviousConversation() throws DatabaseFileException, IOException {
		return getPreviousConversation(true);
	}
	
	/**
	 * Returns previous instance of Conversation in the double-linked list or null if this is the first.
	 * @param lockAllow		Allow storage file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation getPreviousConversation(boolean lockAllow) throws DatabaseFileException, IOException {
		return Conversation.getConversation(mIndexPrev, lockAllow);
	}

	/**
	 * Returns next instance of Conversation in the double-linked list or null if this is the last.
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation getNextConversation() throws DatabaseFileException, IOException {
		return getNextConversation(true);
	}
	
	/**
	 * Returns next instance of Conversation in the double-linked list or null if this is the last.
	 * @param lockAllow		Allow storage file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation getNextConversation(boolean lockAllow) throws DatabaseFileException, IOException {
		return Conversation.getConversation(mIndexNext, lockAllow);
	}

	/**
	 * Returns first SessionKeys object in the stored linked list, or null if there isn't any.
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public SessionKeys getFirstSessionKeys() throws DatabaseFileException, IOException {
		return getFirstSessionKeys(true);
	}
	
	/**
	 * Returns first SessionKeys object in the stored linked list, or null if there isn't any.
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public SessionKeys getFirstSessionKeys(boolean lockAllow) throws DatabaseFileException, IOException {
		if (mIndexSessionKeys == 0)
			return null;
		return SessionKeys.getSessionKeys(mIndexSessionKeys, lockAllow);
	}

	/**
	 * Attach new SessionKeys object to the conversation.
	 * @param keys
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void attachSessionKeys(SessionKeys keys) throws DatabaseFileException, IOException {
		attachSessionKeys(keys, true);
	}

	/**
	 * Attach new SessionKeys object to the conversation.
	 * @param lockAllow		Allow the file to be locked
	 * @param keys
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void attachSessionKeys(SessionKeys keys, boolean lockAllow) throws DatabaseFileException, IOException {
		Database.getDatabase().lockFile(lockAllow);
		try {
			long indexFirstInStack = getIndexSessionKeys();
			if (indexFirstInStack != 0) {
				SessionKeys firstInStack = SessionKeys.getSessionKeys(indexFirstInStack);
				firstInStack.setIndexPrev(keys.getEntryIndex());
				firstInStack.saveToFile(false);
			}
			keys.setIndexNext(indexFirstInStack);
			keys.setIndexPrev(0L);
			keys.setIndexParent(this.mEntryIndex);
			keys.saveToFile(false);
			this.setIndexSessionKeys(keys.getEntryIndex());
			this.saveToFile(false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			Database.getDatabase().unlockFile(lockAllow);	
		}
	}

	/**
	 * Attach new Message object to the conversation 
	 * @param msg
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void attachMessage(Message msg) throws DatabaseFileException, IOException {
		attachMessage(msg, true);
	}

	/**
	 * Attach new Message object to the conversation
	 * @param lockAllow		Allow the file to be locked 
	 * @param msg
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void attachMessage(Message msg, boolean lockAllow) throws DatabaseFileException, IOException {
		Database.getDatabase().lockFile(lockAllow);
		try {
			long indexFirstInStack = getIndexMessages();
			if (indexFirstInStack != 0) {
				Message firstInStack = Message.getMessage(indexFirstInStack);
				firstInStack.setIndexPrev(msg.getEntryIndex());
				firstInStack.saveToFile(false);
			}
			msg.setIndexNext(indexFirstInStack);
			msg.setIndexPrev(0L);
			msg.setIndexParent(this.mEntryIndex);
			msg.saveToFile(false);
			this.setIndexMessages(msg.getEntryIndex());
			this.saveToFile(false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			Database.getDatabase().unlockFile(lockAllow);	
		}
	}
	
	/**
	 * Get the first Message object in the linked listed attached to this conversation, or null if there isn't any
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Message getFirstMessage() throws DatabaseFileException, IOException {
		return getFirstMessage(true);
	}
	
	/**
	 * Get the first Message object in the linked listed attached to this conversation, or null if there isn't any
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Message getFirstMessage(boolean lockAllow) throws DatabaseFileException, IOException {
		return Message.getMessage(mIndexMessages, lockAllow);
	}
	
	/**
	 * Get the first Message object in the linked listed attached to this conversation, or null if there isn't any
	 * @param lockAllow		Allow the file to be locked 
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public ArrayList<Message> getMessages(boolean lockAllow) throws DatabaseFileException, IOException {
		ArrayList<Message> list = new ArrayList<Message>();
		Message msg = getFirstMessage(lockAllow);
		while (msg != null) {
			list.add(msg);
			msg = msg.getNextMessage(lockAllow);
		}
		return list;
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
		Conversation prev = this.getPreviousConversation(lockAllow);
		Conversation next = this.getNextConversation(lockAllow); 

		if (prev != null) {
			// this is not the first Conversation in the list
			// update the previous one
			prev.setIndexNext(this.getIndexNext());
			prev.saveToFile(lockAllow);
		} else {
			// this IS the first Conversation in the list
			// update parent
			Header header = Header.getHeader(lockAllow);
			header.setIndexConversations(this.getIndexNext());
			header.saveToFile(lockAllow);
		}
		
		// update next one
		if (next != null) {
			next.setIndexPrev(this.getIndexPrev());
			next.saveToFile(lockAllow);
		}
		
		// delete all of the SessionKeys
		SessionKeys keys = getFirstSessionKeys(lockAllow);
		while (keys != null) {
			keys.delete(lockAllow);
			keys = getFirstSessionKeys(lockAllow);
		}
		
		// delete all of the Messages
		Message msg = getFirstMessage(lockAllow);
		while (msg != null) {
			msg.delete(lockAllow);
			msg = getFirstMessage(lockAllow);
		}

		// delete this message
		Empty.replaceWithEmpty(mEntryIndex, lockAllow);
		
		// remove from cache
		synchronized (cacheConversation) {
			cacheConversation.remove(this);
		}
		
		// make this instance invalid
		this.mEntryIndex = -1L;
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
}
