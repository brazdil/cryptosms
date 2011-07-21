package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.text.format.Time;

import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing a message entry in the secure storage file.
 * 
 * @author David Brazdil
 *
 */
public class Message {
	// FILE FORMAT
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_TIMESTAMP = 29;
	private static final int LENGTH_MESSAGEBODY = 140;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_TIMESTAMP = OFFSET_FLAGS + LENGTH_FLAGS;
	private static final int OFFSET_MESSAGEBODY = OFFSET_TIMESTAMP + LENGTH_TIMESTAMP;

	private static final int OFFSET_RANDOMDATA = OFFSET_MESSAGEBODY + LENGTH_MESSAGEBODY;

	private static final int OFFSET_NEXTINDEX = Database.ENCRYPTED_ENTRY_SIZE - 4;
	private static final int OFFSET_PREVINDEX = OFFSET_NEXTINDEX - 4;
	private static final int OFFSET_MSGSINDEX = OFFSET_PREVINDEX - 4;
	private static final int OFFSET_PARENTINDEX = OFFSET_MSGSINDEX - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_PARENTINDEX - OFFSET_RANDOMDATA;	
	
	public enum MessageType {
		INCOMING,
		OUTGOING
	}
	
	// STATIC
	
	private static ArrayList<Message> cacheMessage = new ArrayList<Message>();
	
	/**
	 * Removes all instances from the list of cached objects.
	 * Be sure you don't use the instances afterwards.
	 */
	public static void forceClearCache() {
		synchronized (cacheMessage) {
			cacheMessage = new ArrayList<Message>();
		}
	}

	/**
	 * Returns an instance of a new Message entry in the storage file.
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public static Message createMessage() throws DatabaseFileException, IOException {
		return createMessage(true);
	}
	
	/**
	 * Returns an instance of a new Message entry in the storage file.
	 * @param lockAllow		Allow file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public static Message createMessage(boolean lockAllow) throws DatabaseFileException, IOException {
		// create a new one
		return new Message(Empty.getEmptyIndex(lockAllow), false, lockAllow);
	}

	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 */
	static Message getMessage(long index) throws DatabaseFileException, IOException {
		return getMessage(index, true);
	}
	
	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 * @param lock		File lock allow
	 */
	static Message getMessage(long index, boolean lockAllow) throws DatabaseFileException, IOException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheMessage) {
			for (Message empty: cacheMessage)
				if (empty.getEntryIndex() == index)
					return empty; 
		}
		
		// create a new one
		return new Message(index, true, lockAllow);
	}
	
	// INTERNAL FIELDS
	private long mEntryIndex; // READ ONLY
	private boolean mDeliveredPart;
	private boolean mDeliveredAll;
	private MessageType mMessageType;
	private Time mTimeStamp;
	private String mMessageBody;
	private long mIndexParent;
	private long mIndexMessageParts;
	private long mIndexPrev ;
	private long mIndexNext;
	
	// CONSTRUCTORS
	
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	private Message(long index, boolean readFromFile) throws DatabaseFileException, IOException {
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
	private Message(long index, boolean readFromFile, boolean lockAllow) throws DatabaseFileException, IOException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Database.getDatabase().getEntry(index, lockAllow);
			byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
			
			byte flags = dataPlain[OFFSET_FLAGS];
			boolean deliveredPart = ((flags & (1 << 7)) == 0) ? false : true;
			boolean deliveredAll = ((flags & (1 << 6)) == 0) ? false : true;
			boolean messageOutgoing = ((flags & (1 << 5)) == 0) ? false : true;
			
			Time timeStamp = new Time();
			timeStamp.parse3339(Database.fromLatin(dataPlain, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP));

			setDeliveredPart(deliveredPart);
			setDeliveredAll(deliveredAll);
			setMessageType((messageOutgoing) ? MessageType.OUTGOING : MessageType.INCOMING);
			setTimeStamp(timeStamp);
			setMessageBody(Database.fromLatin(dataPlain, OFFSET_MESSAGEBODY, LENGTH_MESSAGEBODY));
			setIndexParent(Database.getInt(dataPlain, OFFSET_PARENTINDEX));
			setIndexMessageParts(Database.getInt(dataPlain, OFFSET_MSGSINDEX));
			setIndexPrev(Database.getInt(dataPlain, OFFSET_PREVINDEX));
			setIndexNext(Database.getInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			Time timeStamp = new Time(); 
			timeStamp.setToNow();
			
			setDeliveredPart(false);
			setDeliveredAll(false);
			setMessageType(MessageType.OUTGOING);
			setTimeStamp(timeStamp);
			setMessageBody("");
			setIndexParent(0L);
			setIndexMessageParts(0L);
			setIndexPrev(0L);
			setIndexNext(0L);
			
			saveToFile(lockAllow);
		}
		
		synchronized (cacheMessage) {
			cacheMessage.add(this);
		}
	}

	// FUNCTIONS

	/**
	 * Save the contents of this class to its place in the storage file.
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void saveToFile() throws DatabaseFileException, IOException {
		saveToFile(true);
	}

	/**
	 * Save the contents of this class to its place in the storage file.
	 * @param lock		Allow the file to be locked
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void saveToFile(boolean lock) throws DatabaseFileException, IOException {
		ByteBuffer msgBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		if (this.mDeliveredPart)
			flags |= (byte) ((1 << 7) & 0xFF);
		if (this.mDeliveredAll)
			flags |= (byte) ((1 << 6) & 0xFF);
		if (this.mMessageType == MessageType.OUTGOING)
			flags |= (byte) ((1 << 5) & 0xFF);
		msgBuffer.put(flags);
		
		// time stamp
		msgBuffer.put(Database.toLatin(this.mTimeStamp.format3339(false), LENGTH_TIMESTAMP));

		// message body
		msgBuffer.put(Database.toLatin(this.mMessageBody, LENGTH_MESSAGEBODY));

		// random data
		msgBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		msgBuffer.put(Database.getBytes(this.mIndexParent)); 
		msgBuffer.put(Database.getBytes(this.mIndexMessageParts)); 
		msgBuffer.put(Database.getBytes(this.mIndexPrev));
		msgBuffer.put(Database.getBytes(this.mIndexNext));
		
		byte[] dataEncrypted = Encryption.encryptSymmetric(msgBuffer.array(), Encryption.retreiveEncryptionKey());
		Database.getDatabase().setEntry(mEntryIndex, dataEncrypted, lock);
	}

	/**
	 * Returns an instance of the Conversation class that is the parent of this Message in the data structure
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation getParent() throws DatabaseFileException, IOException {
		return getParent(true);
	}
	
	/**
	 * Returns an instance of the Conversation class that is the parent of this Message in the data structure
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
	 * Returns an instance of the Message that's predecessor of this one in the linked list of Messages of this Conversation
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Message getPreviousMessage() throws DatabaseFileException, IOException {
		return getPreviousMessage(true);
	}
	
	/**
	 * Returns an instance of the Message that's predecessor of this one in the linked list of Messages of this Conversation
	 * @param lockAllow		Allow file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Message getPreviousMessage(boolean lockAllow) throws DatabaseFileException, IOException {
		if (mIndexPrev == 0)
			return null;
		return Message.getMessage(mIndexPrev, lockAllow);
	}

	/**
	 * Returns an instance of the Message that's successor of this one in the linked list of Messages of this Conversation
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Message getNextMessage() throws DatabaseFileException, IOException {
		return getNextMessage(true);
	}
	
	/**
	 * Returns an instance of the Message that's successor of this one in the linked list of Messages of this Conversation
	 * @param lockAllow		Allow file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Message getNextMessage(boolean lockAllow) throws DatabaseFileException, IOException {
		if (mIndexNext == 0)
			return null;
		return Message.getMessage(mIndexNext, lockAllow);
	}
	
	/**
	 * Returns first message part in the linked list of MessageParts.
	 * Should not be public - Message has API for making this seamlessly
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	MessagePart getFirstMessagePart() throws DatabaseFileException, IOException {
		return getFirstMessagePart(true);
	}
	
	/**
	 * Returns first message part in the linked list of MessageParts.
	 * Should not be public - Message has API for making this seamlessly
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	MessagePart getFirstMessagePart(boolean lockAllow) throws DatabaseFileException, IOException {
		if (mIndexMessageParts == 0)
			return null;
		return MessagePart.getMessagePart(mIndexMessageParts, lockAllow);
	}
	
	/**
	 * Replaces assigned message parts with given list
	 * @param list
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	void assignMessageParts(ArrayList<MessagePart> list) throws IOException, DatabaseFileException {
		assignMessageParts(list, true);
	}
	
	/**
	 * Replaces assigned message parts with given list
	 * @param list
	 * @param lockAllow		Allow the file to be locked
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	void assignMessageParts(ArrayList<MessagePart> list, boolean lockAllow) throws IOException, DatabaseFileException {
		Database.getDatabase().lockFile(lockAllow);
		try {
			long indexFirstInStack = getIndexMessageParts();
			while (indexFirstInStack != 0) {
				MessagePart msgPart = MessagePart.getMessagePart(indexFirstInStack, false);
				indexFirstInStack = msgPart.getIndexNext();
				msgPart.delete(false);
			}

			for (int i = list.size() - 1; i >= 0; --i) {
				MessagePart msgPart = list.get(i); 
				msgPart.setIndexNext(indexFirstInStack);
				indexFirstInStack = msgPart.getEntryIndex();
				msgPart.saveToFile(false);
			}
			this.setIndexMessageParts(indexFirstInStack);
			this.saveToFile(false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			Database.getDatabase().unlockFile(lockAllow);	
		}
	}

	// GETTERS / SETTERS
	
	long getEntryIndex() {
		return mEntryIndex;
	}
	
	public void setDeliveredPart(boolean deliveredPart) {
		this.mDeliveredPart = deliveredPart;
	}

	public boolean getDeliveredPart() {
		return mDeliveredPart;
	}

	public void setDeliveredAll(boolean deliveredAll) {
		this.mDeliveredAll = deliveredAll;
	}

	public boolean getDeliveredAll() {
		return mDeliveredAll;
	}

	public void setMessageType(MessageType messageType) {
		this.mMessageType = messageType;
	}

	public MessageType getMessageType() {
		return mMessageType;
	}

	public void setMessageBody(String messageBody) {
		this.mMessageBody = messageBody;
	}

	public String getMessageBody() {
		return mMessageBody;
	}

	public void setTimeStamp(Time timeStamp) {
		this.mTimeStamp = timeStamp;
	}

	public Time getTimeStamp() {
		return mTimeStamp;
	}

	long getIndexMessageParts() {
		return mIndexMessageParts;
	}

	void setIndexMessageParts(long indexMessageParts) {
		if (indexMessageParts > 0xFFFFFFFFL || indexMessageParts < 0L)
			throw new IndexOutOfBoundsException();
			
		this.mIndexMessageParts = indexMessageParts;
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

	public void setIndexParent(long indexParent) {
		this.mIndexParent = indexParent;
	}

	public long getIndexParent() {
		return mIndexParent;
	}
}
