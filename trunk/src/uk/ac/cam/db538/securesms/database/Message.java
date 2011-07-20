package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.text.format.Time;

import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing an message entry in the secure storage file.
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
	
	private static final int LENGTH_RANDOMDATA = OFFSET_MSGSINDEX - OFFSET_RANDOMDATA;	
	
	public enum MessageType {
		INCOMING,
		OUTGOING
	}
	
	// STATIC
	
	private static ArrayList<Message> cacheMessage = new ArrayList<Message>();
	
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
		for (Message empty: cacheMessage)
			if (empty.getEntryIndex() == index)
				return empty; 
		
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
	private long mIndexMessageParts;
	private long mIndexPrev ;
	private long mIndexNext;
	
	// CONSTRUCTORS
	
	private Message(long index, boolean readFromFile) throws DatabaseFileException, IOException {
		this(index, readFromFile, true);
	}
	
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
			setIndexMessageParts(0L);
			setIndexPrev(0L);
			setIndexNext(0L);
			
			saveToFile(lockAllow);
		}

		cacheMessage.add(this);
	}

	// FUNCTIONS
	
	public void delete() {
		delete(true);
	}
	
	public void delete(boolean lock) {
		//TODO: To be implemented
	}
	
	public void saveToFile() throws DatabaseFileException, IOException {
		saveToFile(true);
	}
	
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
		msgBuffer.put(Database.getBytes(this.mIndexMessageParts)); 
		msgBuffer.put(Database.getBytes(this.mIndexPrev));
		msgBuffer.put(Database.getBytes(this.mIndexNext));
		
		byte[] dataEncrypted = Encryption.encryptSymmetric(msgBuffer.array(), Encryption.retreiveEncryptionKey());
		Database.getDatabase().setEntry(mEntryIndex, dataEncrypted, lock);
	}
	
	public Message getPreviousMessage() throws DatabaseFileException, IOException {
		return getPreviousMessage(true);
	}
	
	public Message getPreviousMessage(boolean lockAllow) throws DatabaseFileException, IOException {
		return Message.getMessage(mIndexPrev, lockAllow);
	}

	public Message getNextMessage() throws DatabaseFileException, IOException {
		return getNextMessage(true);
	}
	
	public Message getNextMessage(boolean lockAllow) throws DatabaseFileException, IOException {
		return Message.getMessage(mIndexNext, lockAllow);
	}

	// GETTERS / SETTERS
	
	long getEntryIndex() {
		return mEntryIndex;
	}
	
	void setDeliveredPart(boolean deliveredPart) {
		this.mDeliveredPart = deliveredPart;
	}

	boolean getDeliveredPart() {
		return mDeliveredPart;
	}

	void setDeliveredAll(boolean deliveredAll) {
		this.mDeliveredAll = deliveredAll;
	}

	boolean getDeliveredAll() {
		return mDeliveredAll;
	}

	void setMessageType(MessageType messageType) {
		this.mMessageType = messageType;
	}

	MessageType getMessageType() {
		return mMessageType;
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

	void setMessageBody(String messageBody) {
		this.mMessageBody = messageBody;
	}

	String getMessageBody() {
		return mMessageBody;
	}

	void setTimeStamp(Time timeStamp) {
		this.mTimeStamp = timeStamp;
	}

	Time getTimeStamp() {
		return mTimeStamp;
	}
}
