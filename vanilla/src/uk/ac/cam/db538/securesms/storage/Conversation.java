package uk.ac.cam.db538.securesms.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.DateTimeFormat;

import uk.ac.cam.db538.securesms.crypto.Encryption;
import uk.ac.cam.db538.securesms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.securesms.storage.SessionKeys.SimNumber;
import uk.ac.cam.db538.securesms.utils.Charset;
import uk.ac.cam.db538.securesms.utils.LowLevel;
import uk.ac.cam.db538.securesms.utils.PhoneNumber;

/**
 * 
 * Class representing a conversation entry in the secure storage file.
 * 
 * @author David Brazdil
 *
 */
public class Conversation implements Comparable<Conversation> {
	// FILE FORMAT
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_PHONENUMBER = 32;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_PHONENUMBER = OFFSET_FLAGS + LENGTH_FLAGS;
	
	private static final int OFFSET_RANDOMDATA = OFFSET_PHONENUMBER + LENGTH_PHONENUMBER;

	private static final int OFFSET_NEXTINDEX = Storage.ENCRYPTED_ENTRY_SIZE - 4;
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
	 * @throws StorageFileException 
	 * @throws WrongKeyException 
	 */
	public static Conversation createConversation() throws StorageFileException, IOException {
		return createConversation(true);
	}

	/**
	 * Returns instance of a new Conversation created in one of the empty spaces in file.
	 * @lockAllow		Allow to lock the file
	 * @return
	 * @throws IOException 
	 * @throws StorageFileException 
	 * @throws WrongKeyException 
	 */
	public static Conversation createConversation(boolean lockAllow) throws StorageFileException, IOException {
		// create a new one
		Conversation conv = new Conversation(Empty.getEmptyIndex(lockAllow), false, lockAllow);
		Header.getHeader(lockAllow).attachConversation(conv, lockAllow);
		return conv;
	}	
	
	/**
	 * Returns an instance of Conversation class with given index in file.
	 * @param phoneNumber		Contacts phone number
	 * @throws WrongKeyException 
	 */
	public static Conversation getConversation(String phoneNumber) throws StorageFileException, IOException {
		return getConversation(phoneNumber, true);
	}

	/**
	 * Returns an instance of Conversation class with given index in file.
	 * @param phoneNumber		Contacts phone number
	 * @param lock		File lock allow
	 * @throws WrongKeyException 
	 */
	public static Conversation getConversation(String phoneNumber, boolean lockAllow) throws StorageFileException, IOException {
		Conversation conv = Header.getHeader(lockAllow).getFirstConversation(lockAllow);
		while (conv != null) {
			if (PhoneNumber.compare(conv.getPhoneNumber(), phoneNumber))
				return conv;
			conv = conv.getNextConversation(lockAllow);
		}
		return null;
	}

	/**
	 * Returns an instance of Conversation class with given index in file.
	 * @param index		Index in file
	 * @throws WrongKeyException 
	 */
	static Conversation getConversation(long index) throws StorageFileException, IOException {
		return getConversation(index, true);
	}
	
	/**
	 * Returns an instance of Conversation class with given index in file.
	 * @param index		Index in file
	 * @param lock		File lock allow
	 * @throws WrongKeyException 
	 */
	static Conversation getConversation(long index, boolean lockAllow) throws StorageFileException, IOException {
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
	 * @throws StorageFileException
	 * @throws WrongKeyException 
	 */
	public static void cacheAllConversations() throws IOException, StorageFileException {
		Conversation convCurrent = Header.getHeader(false).getFirstConversation();
		while (convCurrent != null) 
			convCurrent = convCurrent.getNextConversation();
	}
	
	// INTERNAL FIELDS
	private long mEntryIndex; // READ ONLY
	private String mPhoneNumber;
	private long mIndexSessionKeys;
	private long mIndexMessages;
	private long mIndexPrev;
	private long mIndexNext;
	
	// CONSTRUCTORS
	
	private Conversation(long index, boolean readFromFile) throws StorageFileException, IOException {
		this(index, readFromFile, true);
	}
	
	private Conversation(long index, boolean readFromFile, boolean lockAllow) throws StorageFileException, IOException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Storage.getDatabase().getEntry(index, lockAllow);
			byte[] dataPlain;
			try {
				dataPlain = Encryption.getEncryption().decryptSymmetricWithMasterKey(dataEncrypted);
			} catch (EncryptionException e) {
				throw new StorageFileException(e);
			}
			
			setPhoneNumber(Charset.fromAscii8(dataPlain, OFFSET_PHONENUMBER, LENGTH_PHONENUMBER));
			setIndexSessionKeys(LowLevel.getUnsignedInt(dataPlain, OFFSET_KEYSINDEX));
			setIndexMessages(LowLevel.getUnsignedInt(dataPlain, OFFSET_MSGSINDEX));
			setIndexPrev(LowLevel.getUnsignedInt(dataPlain, OFFSET_PREVINDEX));
			setIndexNext(LowLevel.getUnsignedInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			setPhoneNumber("");
			setIndexSessionKeys(0L);
			setIndexMessages(0L);
			setIndexPrev(0L);
			setIndexNext(0L);
			
			saveToFile(lockAllow);
		}

		synchronized (cacheConversation) {
			cacheConversation.add(this);
		}
		notifyUpdate();
	}

	// FUNCTIONS
	
	/**
	 * Saves data to the storage file.
	 */
	public void saveToFile() throws StorageFileException, IOException {
		saveToFile(true);
	}
	
	/**
	 * Saves data to the storage file.
	 * @param lock			Allow the file to be locked.
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public void saveToFile(boolean lock) throws StorageFileException, IOException {
		ByteBuffer convBuffer = ByteBuffer.allocate(Storage.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		convBuffer.put(flags);
		
		// phone number
		convBuffer.put(Charset.toAscii8(this.mPhoneNumber, LENGTH_PHONENUMBER));
		
		// random data
		convBuffer.put(Encryption.getEncryption().generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		convBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexSessionKeys)); 
		convBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexMessages)); 
		convBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexPrev));
		convBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexNext));
		
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = Encryption.getEncryption().encryptSymmetricWithMasterKey(convBuffer.array());
		} catch (EncryptionException e) {
			throw new StorageFileException(e);
		}
		Storage.getDatabase().setEntry(this.mEntryIndex, dataEncrypted, lock);
	}

	/**
	 * Returns previous instance of Conversation in the double-linked list or null if this is the first.
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 * @throws WrongKeyException 
	 */
	public Conversation getPreviousConversation() throws StorageFileException, IOException {
		return getPreviousConversation(true);
	}
	
	/**
	 * Returns previous instance of Conversation in the double-linked list or null if this is the first.
	 * @param lockAllow		Allow storage file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 * @throws WrongKeyException 
	 */
	public Conversation getPreviousConversation(boolean lockAllow) throws StorageFileException, IOException {
		return Conversation.getConversation(mIndexPrev, lockAllow);
	}

	/**
	 * Returns next instance of Conversation in the double-linked list or null if this is the last.
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 * @throws WrongKeyException 
	 */
	public Conversation getNextConversation() throws StorageFileException, IOException {
		return getNextConversation(true);
	}
	
	/**
	 * Returns next instance of Conversation in the double-linked list or null if this is the last.
	 * @param lockAllow		Allow storage file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 * @throws WrongKeyException 
	 */
	public Conversation getNextConversation(boolean lockAllow) throws StorageFileException, IOException {
		return Conversation.getConversation(mIndexNext, lockAllow);
	}

	/**
	 * Returns first SessionKeys object in the stored linked list, or null if there isn't any.
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public SessionKeys getFirstSessionKeys() throws StorageFileException, IOException {
		return getFirstSessionKeys(true);
	}
	
	/**
	 * Returns first SessionKeys object in the stored linked list, or null if there isn't any.
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public SessionKeys getFirstSessionKeys(boolean lockAllow) throws StorageFileException, IOException {
		if (mIndexSessionKeys == 0)
			return null;
		return SessionKeys.getSessionKeys(mIndexSessionKeys, lockAllow);
	}

	/**
	 * Attach new SessionKeys object to the conversation.
	 * @param keys
	 * @throws StorageFileException
	 * @throws IOException
	 */
	void attachSessionKeys(SessionKeys keys) throws StorageFileException, IOException {
		attachSessionKeys(keys, true);
	}

	/**
	 * Attach new SessionKeys object to the conversation.
	 * @param lockAllow		Allow the file to be locked
	 * @param keys
	 * @throws StorageFileException
	 * @throws IOException
	 */
	void attachSessionKeys(SessionKeys keys, boolean lockAllow) throws StorageFileException, IOException {
		Storage.getDatabase().lockFile(lockAllow);
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
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			Storage.getDatabase().unlockFile(lockAllow);	
		}
	}

	/**
	 * Attach new MessageData object to the conversation 
	 * @param msg
	 * @throws StorageFileException
	 * @throws IOException
	 */
	void attachMessageData(MessageData msg) throws StorageFileException, IOException {
		attachMessageData(msg, true);
	}

	/**
	 * Attach new MessageData object to the conversation
	 * @param lockAllow		Allow the file to be locked 
	 * @param msg
	 * @throws StorageFileException
	 * @throws IOException
	 */
	void attachMessageData(MessageData msg, boolean lockAllow) throws StorageFileException, IOException {
		Storage.getDatabase().lockFile(lockAllow);
		try {
			long indexFirstInStack = getIndexMessages();
			if (indexFirstInStack != 0) {
				MessageData firstInStack = MessageData.getMessageData(indexFirstInStack);
				firstInStack.setIndexPrev(msg.getEntryIndex());
				firstInStack.saveToFile(false);
			}
			msg.setIndexNext(indexFirstInStack);
			msg.setIndexPrev(0L);
			msg.setIndexParent(this.mEntryIndex);
			msg.saveToFile(false);
			this.setIndexMessages(msg.getEntryIndex());
			this.saveToFile(false);
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			Storage.getDatabase().unlockFile(lockAllow);	
		}
	}
	
	/**
	 * Get the first MessageData object in the linked listed attached to this conversation, or null if there isn't any
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public MessageData getFirstMessageData() throws StorageFileException, IOException {
		return getFirstMessageData(true);
	}
	
	/**
	 * Get the first MessageData object in the linked listed attached to this conversation, or null if there isn't any
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public MessageData getFirstMessageData(boolean lockAllow) throws StorageFileException, IOException {
		return MessageData.getMessageData(mIndexMessages, lockAllow);
	}
	
	/**
	 * Get the first MessageData object in the linked listed attached to this conversation, or null if there isn't any
	 * @param lockAllow		Allow the file to be locked 
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public ArrayList<MessageData> getMessages(boolean lockAllow) throws StorageFileException, IOException {
		ArrayList<MessageData> list = new ArrayList<MessageData>();
		MessageData msg = getFirstMessageData(lockAllow);
		while (msg != null) {
			list.add(msg);
			msg = msg.getNextMessageData(lockAllow);
		}
		return list;
	}

	/**
	 * Delete MessageData and all the MessageDataParts it controls
	 * @throws StorageFileException
	 * @throws IOException
	 * @throws WrongKeyException 
	 */
	public void delete() throws StorageFileException, IOException {
		delete(true);
	}
	
	/**
	 * Delete MessageData and all the MessageDataParts it controls
	 * @param lockAllow 	Allow the file to be locked
	 * @throws StorageFileException
	 * @throws IOException
	 * @throws WrongKeyException 
	 */
	public void delete(boolean lockAllow) throws StorageFileException, IOException {
		Storage db = Storage.getDatabase();
		
		db.lockFile(lockAllow);
		try {
			Conversation prev = this.getPreviousConversation(false);
			Conversation next = this.getNextConversation(false); 
	
			if (prev != null) {
				// this is not the first Conversation in the list
				// update the previous one
				prev.setIndexNext(this.getIndexNext());
				prev.saveToFile(false);
			} else {
				// this IS the first Conversation in the list
				// update parent
				Header header = Header.getHeader(false);
				header.setIndexConversations(this.getIndexNext());
				header.saveToFile(false);
			}
			
			// update next one
			if (next != null) {
				next.setIndexPrev(this.getIndexPrev());
				next.saveToFile(false);
			}
			
			// delete all of the SessionKeys
			SessionKeys keys = getFirstSessionKeys(false);
			while (keys != null) {
				keys.delete(false);
				keys = getFirstSessionKeys(false);
			}
			
			// delete all of the MessageDatas
			MessageData msg = getFirstMessageData(false);
			while (msg != null) {
				msg.delete(false);
				msg = getFirstMessageData(false);
			}
	
			// delete this conversation
			Empty.replaceWithEmpty(mEntryIndex, false);
			
			// remove from cache
			synchronized (cacheConversation) {
				cacheConversation.remove(this);
			}
			notifyUpdate();
			
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

	/**
	 * Returns session keys assigned to this conversation for specified SIM number, or null if there aren't any
	 * @param simNumber
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public SessionKeys getSessionKeys(SimNumber simNumber) throws StorageFileException, IOException {
		return getSessionKeys(simNumber, true);
	}

	/**
	 * Returns session keys assigned to this conversation for specified SIM number, or null if there aren't any
	 * @param simNumber
	 * @param lockAllow		Allow file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public SessionKeys getSessionKeys(SimNumber simNumber, boolean lockAllow) throws StorageFileException, IOException {
		SessionKeys keys = getFirstSessionKeys(lockAllow);
		while (keys != null) {
			if (simNumber.equals(keys.getSimNumber()))
				return keys;
			keys = keys.getNextSessionKeys(lockAllow);
		}
		
		return null;
	}
	
	/**
	 * Goes through all the assigned session keys.
	 * If there is a session key with SIM number of the param original,
	 * its SIM number is replaced for the one in param replacement and
	 * all the other keys matching the param replacement are deleted.
	 * If there isn't one matching param original, nothing happens.
	 * @param original
	 * @param replacement
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public void replaceSessionKeys(SimNumber original, SimNumber replacement) throws StorageFileException, IOException {
		replaceSessionKeys(original, replacement, true);
	}
		
	/**
	 * Goes through all the assigned session keys.
	 * If there is a session key with SIM number of the param original,
	 * its SIM number is replaced for the one in param replacement and
	 * all the other keys matching the param replacement are deleted.
	 * If there isn't one matching param original, nothing happens.
	 * @param original
	 * @param replacement
	 * @param lockAllow
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public void replaceSessionKeys(SimNumber original, SimNumber replacement, boolean lockAllow) throws StorageFileException, IOException {
		if (original.equals(replacement))
			// no point in continuing
			return;
		
		Storage db = Storage.getDatabase();
		
		db.lockFile(lockAllow);
		try {
			boolean canBeReplaced = false;
			boolean sthToDelete = false;
			SessionKeys keys = this.getFirstSessionKeys(false);
			while (keys != null) {
				// go through all the assigned keys
				// look for ones matching the param original
				if (keys.getSimNumber().equals(original))
					// so SIM number of this key should be replaced
					canBeReplaced = true;
				if (keys.getSimNumber().equals(replacement))
					// this matches the new SIM number
					// will be deleted if sth is replaced
					sthToDelete = true;
				keys = keys.getNextSessionKeys(false);
			}

			if (canBeReplaced) {
				if (sthToDelete) {
					keys = this.getFirstSessionKeys(false);
					while (keys != null) {
						if (keys.getSimNumber().equals(replacement))
							keys.delete(false);
						keys = keys.getNextSessionKeys(false);
					}
				}
				
				boolean found = false;
				keys = this.getFirstSessionKeys(false);
				while (keys != null) {
					if (keys.getSimNumber().equals(original)) {
						// if this is the first key matching original,
						// its SIM number will be replaced
						// otherwise deleted because it's redundant
						if (found)
							keys.delete(false);
						else {
							keys.setSimNumber(replacement);
							keys.saveToFile(false);
							found = true;
						}
					}
					keys = keys.getNextSessionKeys(false);
				}
			}
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			db.unlockFile(lockAllow);
		}
	}
	
	public DateTime getTimeStamp() {
		MessageData firstMessage = null;
		try {
			firstMessage = getFirstMessageData();
		} catch (StorageFileException e) {
		} catch (IOException e) {
		}
		
		if (firstMessage != null)
			return firstMessage.getTimeStamp();
		else
			return new DateTime();
	}

	/**
	 * Returns time in a nice way
	 * @return
	 */
	public String getFormattedTime() {
		return DateTimeFormat.forPattern("HH:mm").print(getTimeStamp());
	}
	
	/**
	 * Returns whether this conversation should be marked unread
	 * @return
	 */
	public boolean getMarkedUnread() {
		MessageData firstMessage = null;
		try {
			firstMessage = getFirstMessageData();
		} catch (StorageFileException e) {
		} catch (IOException e) {
		}
		
		if (firstMessage != null)
			return firstMessage.getUnread();
		else
			return false;
	}

	// STATIC FUNCTIONS

	/**
	 * Calls replaceSessionKeys on all the conversations.
	 * Calls an update of listeners afterwards.
	 * @throws WrongKeyException 
	 */
	public static void changeAllSessionKeys(SimNumber original, SimNumber replacement, boolean lockAllow) throws StorageFileException, IOException {
		Storage.getDatabase().lockFile(lockAllow);
		try {
			Conversation conv = Header.getHeader(false).getFirstConversation(false);
			while (conv != null) {
				conv.replaceSessionKeys(original, replacement, false);
				conv = conv.getNextConversation(false);
			}
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			Storage.getDatabase().unlockFile(lockAllow);
		}
		notifyUpdate();
	}
	
	/**
	 * Returns all SIM numbers stored with session keys of all conversations
	 * @param lockAllow
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 * @throws WrongKeyException 
	 */
	public static ArrayList<SimNumber> getAllSimNumbersStored(boolean lockAllow) throws StorageFileException, IOException {
		ArrayList<SimNumber> simNumbers = new ArrayList<SimNumber>();
		
		Conversation conv = Header.getHeader(lockAllow).getFirstConversation(lockAllow);
		while (conv != null) {
			SessionKeys keys = conv.getFirstSessionKeys(lockAllow);
			while (keys != null) {
				boolean found = false;
				for (SimNumber n : simNumbers)
					if (keys.getSimNumber().equals(n))
						found = true;
				if (!found)
					simNumbers.add(keys.getSimNumber());
				keys = keys.getNextSessionKeys(lockAllow);
			}
			conv = conv.getNextConversation(lockAllow);			
		}
		
		return simNumbers;
	}

	/**
	 * Filters list of SIM numbers, looking only for phone numbers
	 * @param simNumbers
	 * @return
	 */
	public static ArrayList<SimNumber> filterOnlyPhoneNumbers(ArrayList<SimNumber> simNumbers) {
		ArrayList<SimNumber> phoneNumbers = new ArrayList<SimNumber>();
		for (SimNumber n : simNumbers)
			if (n.isSerial() == false)
				phoneNumbers.add(n);
		return phoneNumbers;
	}

	/**
	 * Filters list of SIM numbers, removing specified one
	 * @param simNumbers
	 * @param filter
	 * @return
	 */
	public static ArrayList<SimNumber> filterOutNumber(ArrayList<SimNumber> simNumbers, SimNumber filter) {
		ArrayList<SimNumber> numbers = new ArrayList<SimNumber>();
		for (SimNumber n : simNumbers)
			if (!n.equals(filter))
				numbers.add(n);
		return numbers;
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
	
	// LISTENERS
	
	private static ArrayList<ConversationUpdateListener> mGlobalListeners = new ArrayList<ConversationUpdateListener>();
	
	public static interface ConversationUpdateListener {
		public void onUpdate();
	}
	
	private static void notifyUpdate() {
		for (ConversationUpdateListener listener: mGlobalListeners) 
			listener.onUpdate();
	}
	
	public static void addUpdateListener(ConversationUpdateListener listener) {
		mGlobalListeners.add(listener);
	}

	@Override
	public int compareTo(Conversation another) {
		try {
			return DateTimeComparator.getInstance().compare(this.getTimeStamp(), another.getTimeStamp());
		} catch (Exception e) {
			return 0;
		}
	}
}
