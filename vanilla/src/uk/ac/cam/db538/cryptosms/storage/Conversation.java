package uk.ac.cam.db538.cryptosms.storage;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.DateTimeFormat;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.utils.Charset;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import uk.ac.cam.db538.cryptosms.utils.PhoneNumber;
import uk.ac.cam.db538.cryptosms.utils.SimNumber;

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
	 * @throws StorageFileException 
	 */
	public static Conversation createConversation() throws StorageFileException {
		// create a new one
		Conversation conv = new Conversation(Empty.getEmptyIndex(), false);
		Header.getHeader().attachConversation(conv);
		Storage.notifyChange();
		return conv;
	}	
	
	/**
	 * Returns an instance of Conversation class with given index in file.
	 * @param phoneNumber		Contacts phone number
	 */
	public static Conversation getConversation(String phoneNumber) throws StorageFileException {
		Conversation conv = Header.getHeader().getFirstConversation();
		while (conv != null) {
			if (PhoneNumber.compare(conv.getPhoneNumber(), phoneNumber))
				return conv;
			conv = conv.getNextConversation();
		}
		return null;
	}

	/**
	 * Returns an instance of Conversation class with given index in file.
	 * @param index		Index in file
	 */
	static Conversation getConversation(long index) throws StorageFileException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheConversation) {
			for (Conversation conv: cacheConversation)
				if (conv.getEntryIndex() == index)
					return conv; 
		}
		
		// create a new one
		return new Conversation(index, true);
	}
	
	/** 
	 * Explicitly requests each conversation in the file to be loaded to memory
	 * @throws StorageFileException
	 * @throws WrongKeyException 
	 */
	public static void cacheAllConversations() throws StorageFileException {
		Conversation convCurrent = Header.getHeader().getFirstConversation();
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
	
	private Conversation(long index, boolean readFromFile) throws StorageFileException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Storage.getStorage().getEntry(index);
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
			
			saveToFile();
		}

		synchronized (cacheConversation) {
			cacheConversation.add(this);
		}
	}

	// FUNCTIONS
	
	/**
	 * Saves data to the storage file.
	 */
	public void saveToFile() throws StorageFileException {
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
		Storage.getStorage().setEntry(this.mEntryIndex, dataEncrypted);
	}

	/**
	 * Returns previous instance of Conversation in the double-linked list or null if this is the first.
	 * @return
	 * @throws StorageFileException
	 */
	public Conversation getPreviousConversation() throws StorageFileException {
		return Conversation.getConversation(mIndexPrev);
	}

	/**
	 * Returns next instance of Conversation in the double-linked list or null if this is the last.
	 * @return
	 * @throws StorageFileException
	 */
	public Conversation getNextConversation() throws StorageFileException {
		return Conversation.getConversation(mIndexNext);
	}

	/**
	 * Returns first SessionKeys object in the stored linked list, or null if there isn't any.
	 * @return
	 * @throws StorageFileException
	 */
	public SessionKeys getFirstSessionKeys() throws StorageFileException {
		if (mIndexSessionKeys == 0)
			return null;
		return SessionKeys.getSessionKeys(mIndexSessionKeys);
	}

	/**
	 * Attaches new SessionKeys object to the conversation.
	 * Deletes other SessionKeys already attached with the same simNumber. 
	 * @param keys
	 * @throws StorageFileException
	 */
	void attachSessionKeys(SessionKeys keys) throws StorageFileException {
		long indexFirstInStack = getIndexSessionKeys();
		if (indexFirstInStack != 0) {
			SessionKeys firstInStack = SessionKeys.getSessionKeys(indexFirstInStack);
			firstInStack.setIndexPrev(keys.getEntryIndex());
			firstInStack.saveToFile();
		}
		keys.setIndexNext(indexFirstInStack);
		keys.setIndexPrev(0L);
		keys.setIndexParent(this.mEntryIndex);
		keys.saveToFile();
		this.setIndexSessionKeys(keys.getEntryIndex());
		this.saveToFile();
	}
	
	/**
	 * Attach new MessageData object to the conversation 
	 * @param msg
	 * @throws StorageFileException
	 */
	void attachMessageData(MessageData msg) throws StorageFileException {
		long indexFirstInStack = getIndexMessages();
		if (indexFirstInStack != 0) {
			MessageData firstInStack = MessageData.getMessageData(indexFirstInStack);
			firstInStack.setIndexPrev(msg.getEntryIndex());
			firstInStack.saveToFile();
		}
		msg.setIndexNext(indexFirstInStack);
		msg.setIndexPrev(0L);
		msg.setIndexParent(this.mEntryIndex);
		msg.saveToFile();
		this.setIndexMessages(msg.getEntryIndex());
		this.saveToFile();
	}
	
	/**
	 * Get the first MessageData object in the linked listed attached to this conversation, or null if there isn't any
	 * @return
	 * @throws StorageFileException
	 */
	public MessageData getFirstMessageData() throws StorageFileException {
		return MessageData.getMessageData(mIndexMessages);
	}
	
	public boolean hasMessageData() {
		return mIndexMessages != 0;
	}
	
	/**
	 * Get the first MessageData object in the linked listed attached to this conversation, or null if there isn't any
	 * @return
	 * @throws StorageFileException
	 */
	public ArrayList<MessageData> getMessages() throws StorageFileException {
		ArrayList<MessageData> list = new ArrayList<MessageData>();
		MessageData msg = getFirstMessageData();
		while (msg != null) {
			list.add(msg);
			msg = msg.getNextMessageData();
		}
		return list;
	}

	/**
	 * Delete MessageData and all the MessageDataParts it controls
	 * @throws StorageFileException
	 */
	public void delete() throws StorageFileException {
		Conversation prev = this.getPreviousConversation();
		Conversation next = this.getNextConversation(); 

		if (prev != null) {
			// this is not the first Conversation in the list
			// update the previous one
			prev.setIndexNext(this.getIndexNext());
			prev.saveToFile();
		} else {
			// this IS the first Conversation in the list
			// update parent
			Header header = Header.getHeader();
			header.setIndexConversations(this.getIndexNext());
			header.saveToFile();
		}
		
		// update next one
		if (next != null) {
			next.setIndexPrev(this.getIndexPrev());
			next.saveToFile();
		}
		
		// delete all of the SessionKeys
		SessionKeys keys = getFirstSessionKeys();
		while (keys != null) {
			keys.delete();
			keys = getFirstSessionKeys();
		}
		
		// delete all of the MessageDatas
		MessageData msg = getFirstMessageData();
		while (msg != null) {
			msg.delete();
			msg = getFirstMessageData();
		}

		// delete this conversation
		Empty.replaceWithEmpty(mEntryIndex);
		
		// remove from cache
		synchronized (cacheConversation) {
			cacheConversation.remove(this);
		}
		Storage.notifyChange();
		
		// make this instance invalid
		this.mEntryIndex = -1L;
	}

	/**
	 * Returns session keys assigned to this conversation for specified SIM number, or null if there aren't any
	 * @param simNumber
	 * @return
	 * @throws StorageFileException
	 */
	public SessionKeys getSessionKeys(SimNumber simNumber) throws StorageFileException {
		SessionKeys keys = getFirstSessionKeys();
		while (keys != null) {
			if (simNumber.equals(keys.getSimNumber()))
				return keys;
			keys = keys.getNextSessionKeys();
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
	 */
	public void replaceSessionKeys(SimNumber original, SimNumber replacement) throws StorageFileException {
		if (original.equals(replacement))
			// no point in continuing
			return;
		
		boolean canBeReplaced = false;
		boolean sthToDelete = false;
		SessionKeys keys = this.getFirstSessionKeys();
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
			keys = keys.getNextSessionKeys();
		}

		if (canBeReplaced) {
			if (sthToDelete) {
				keys = this.getFirstSessionKeys();
				while (keys != null) {
					if (keys.getSimNumber().equals(replacement))
						keys.delete();
					keys = keys.getNextSessionKeys();
				}
			}
			
			boolean found = false;
			keys = this.getFirstSessionKeys();
			while (keys != null) {
				if (keys.getSimNumber().equals(original)) {
					// if this is the first key matching original,
					// its SIM number will be replaced
					// otherwise deleted because it's redundant
					if (found)
						keys.delete();
					else {
						keys.setSimNumber(replacement);
						keys.saveToFile();
						found = true;
					}
				}
				keys = keys.getNextSessionKeys();
			}
		}
	}
	
	/**
	 * Goes through all the SessionKeys assigned with the Conversation
	 * and deletes those that match the simNumber in parameter,
	 * Nothing happens if none are found.
	 * @param simNumber
	 * @throws StorageFileException
	 */
	public void deleteSessionKeys(SimNumber simNumber) throws StorageFileException {
		SessionKeys temp, keys = getFirstSessionKeys();
		while (keys != null) {
			temp = keys.getNextSessionKeys();
			if (keys.getSimNumber().equals(simNumber))
				keys.delete();
			keys = temp;
		}
		Storage.notifyChange();
	}


	public DateTime getTimeStamp() {
		MessageData firstMessage = null;
		try {
			firstMessage = getFirstMessageData();
		} catch (StorageFileException e) {
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
	 */
	public static void changeAllSessionKeys(SimNumber original, SimNumber replacement) throws StorageFileException {
		Conversation conv = Header.getHeader().getFirstConversation();
		while (conv != null) {
			conv.replaceSessionKeys(original, replacement);
			conv = conv.getNextConversation();
		}
		Storage.notifyChange();
	}
	
	/**
	 * Returns all SIM numbers stored with session keys of all conversations
	 * @return
	 * @throws StorageFileException
	 */
	public static ArrayList<SimNumber> getAllSimNumbersStored() throws StorageFileException {
		ArrayList<SimNumber> simNumbers = new ArrayList<SimNumber>();
		
		Conversation conv = Header.getHeader().getFirstConversation();
		while (conv != null) {
			SessionKeys keys = conv.getFirstSessionKeys();
			while (keys != null) {
				boolean found = false;
				for (SimNumber n : simNumbers)
					if (keys.getSimNumber().equals(n))
						found = true;
				if (!found)
					simNumbers.add(keys.getSimNumber());
				keys = keys.getNextSessionKeys();
			}
			conv = conv.getNextConversation();			
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
	
	@Override
	public int compareTo(Conversation another) {
		try {
			return DateTimeComparator.getInstance().compare(this.getTimeStamp(), another.getTimeStamp());
		} catch (Exception e) {
			return 0;
		}
	}
}
