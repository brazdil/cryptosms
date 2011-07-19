package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import android.text.format.Time;

import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing a conversation entry in the secure storage file.
 * Not to be used outside the package.
 * 
 * @author David Brazdil
 *
 */
class FileEntryConversation {
	
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

	private String mPhoneNumber;
	private Time mTimeStamp;
	private long mIndexSessionKeys;
	private long mIndexMessages;
	private long mIndexPrev;
	private long mIndexNext;
	
	FileEntryConversation(String phoneNumber, Time timeStamp, long indexSessionKeys, long indexMessages, long indexPrev, long indexNext) {
		setPhoneNumber(phoneNumber);
		setTimeStamp(timeStamp);
		setIndexSessionKeys(indexSessionKeys);
		setIndexMessages(indexMessages);
		setIndexPrev(indexPrev);
		setIndexNext(indexNext);
	}
	
	String getPhoneNumber() {
		return mPhoneNumber;
	}

	void setPhoneNumber(String phoneNumber) {
		this.mPhoneNumber = phoneNumber;
	}

	Time getTimeStamp() {
		return mTimeStamp;
	}

	void setTimeStamp(Time timeStamp) {
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

	static byte[] createData(FileEntryConversation conversation) throws DatabaseFileException {
		ByteBuffer convBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		convBuffer.put(flags);
		
		// phone number
		convBuffer.put(Database.toLatin(conversation.mPhoneNumber, LENGTH_PHONENUMBER));
		
		// time stamp
		convBuffer.put(Database.toLatin(conversation.mTimeStamp.format3339(false), LENGTH_TIMESTAMP));

		// random data
		convBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		convBuffer.put(Database.getBytes(conversation.mIndexSessionKeys)); 
		convBuffer.put(Database.getBytes(conversation.mIndexMessages)); 
		convBuffer.put(Database.getBytes(conversation.mIndexPrev));
		convBuffer.put(Database.getBytes(conversation.mIndexNext));
		
		return Encryption.encryptSymmetric(convBuffer.array(), Encryption.retreiveEncryptionKey());
	}
	
	static FileEntryConversation parseData(byte[] dataEncrypted) throws DatabaseFileException {
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		Time timeStamp = new Time();
		timeStamp.parse3339(Database.fromLatin(dataPlain, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP));

		return new FileEntryConversation(Database.fromLatin(dataPlain, OFFSET_PHONENUMBER, LENGTH_PHONENUMBER),
		                                 timeStamp, 
		                                 Database.getInt(dataPlain, OFFSET_KEYSINDEX), 
		                                 Database.getInt(dataPlain, OFFSET_MSGSINDEX), 
		                                 Database.getInt(dataPlain, OFFSET_PREVINDEX),
		                                 Database.getInt(dataPlain, OFFSET_NEXTINDEX)
		                                 );
	}
}
