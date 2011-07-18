package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import android.text.format.Time;

import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing a message entry in the secure storage file.
 * Not to be used outside the package.
 * 
 * @author David Brazdil
 *
 */
class FileEntryMessage {
	
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

	enum MessageType {
		INCOMING,
		OUTGOING
	}

	private boolean mDeliveredPart;
	private boolean mDeliveredAll;
	private MessageType mMessageType;
	private Time mTimeStamp;
	private String mMessageBody;
	private long mIndexMessageParts;
	private long mIndexPrev ;
	private long mIndexNext;
	
	FileEntryMessage(boolean deliveredPart,
	                 boolean deliveredAll,
	                 MessageType messageType,
	                 Time timeStamp,
	                 String messageBody,
	                 long indexMessageParts,
	                 long indexPrev,
	                 long indexNext) {
		setDeliveredPart(deliveredPart);
		setDeliveredAll(deliveredAll);
		setMessageType(messageType);
		setTimeStamp(timeStamp);
		setMessageBody(messageBody);
		setIndexMessageParts(indexMessageParts);
		setIndexPrev(indexPrev);
		setIndexNext(indexNext);
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
	
	static byte[] createData(FileEntryMessage message) throws DatabaseFileException {
		ByteBuffer msgBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		if (message.mDeliveredPart)
			flags |= (byte) ((1 << 7) & 0xFF);
		if (message.mDeliveredAll)
			flags |= (byte) ((1 << 6) & 0xFF);
		if (message.mMessageType == MessageType.OUTGOING)
			flags |= (byte) ((1 << 5) & 0xFF);
		msgBuffer.put(flags);
		
		// time stamp
		msgBuffer.put(Database.toLatin(message.mTimeStamp.format3339(false), LENGTH_TIMESTAMP));

		// message body
		msgBuffer.put(Database.toLatin(message.mMessageBody, LENGTH_MESSAGEBODY));

		// random data
		msgBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		msgBuffer.put(Database.getBytes(message.mIndexMessageParts)); 
		msgBuffer.put(Database.getBytes(message.mIndexPrev));
		msgBuffer.put(Database.getBytes(message.mIndexNext));
		
		return Encryption.encryptSymmetric(msgBuffer.array(), Encryption.retreiveEncryptionKey());
	}
	
	static FileEntryMessage parseData(byte[] dataEncrypted) throws DatabaseFileException {
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		byte flags = dataPlain[OFFSET_FLAGS];
		boolean deliveredPart = ((flags & (1 << 7)) == 0) ? false : true;
		boolean deliveredAll = ((flags & (1 << 6)) == 0) ? false : true;
		boolean messageOutgoing = ((flags & (1 << 5)) == 0) ? false : true;
		
		Time timeStamp = new Time();
		timeStamp.parse3339(Database.fromLatin(dataPlain, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP));

		return new FileEntryMessage(deliveredPart, 
		                            deliveredAll, 
		                            (messageOutgoing) ? MessageType.OUTGOING : MessageType.INCOMING, 
		                            timeStamp, 
		                            Database.fromLatin(dataPlain, OFFSET_MESSAGEBODY, LENGTH_MESSAGEBODY), 
		                            Database.getInt(dataPlain, OFFSET_MSGSINDEX), 
		                            Database.getInt(dataPlain, OFFSET_PREVINDEX),
		                            Database.getInt(dataPlain, OFFSET_NEXTINDEX)
		                           );
	}
}
