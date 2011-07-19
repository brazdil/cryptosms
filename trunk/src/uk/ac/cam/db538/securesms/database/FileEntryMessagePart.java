package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import android.text.format.Time;

import uk.ac.cam.db538.securesms.database.Message.MessageType;
import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing a message entry in the secure storage file.
 * Not to be used outside the package.
 * 
 * @author David Brazdil
 *
 */
class FileEntryMessagePart {
	
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_MESSAGEBODY = 140;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_MESSAGEBODY = OFFSET_FLAGS + LENGTH_FLAGS;

	private static final int OFFSET_RANDOMDATA = OFFSET_MESSAGEBODY + LENGTH_MESSAGEBODY;

	private static final int OFFSET_NEXTINDEX = Database.ENCRYPTED_ENTRY_SIZE - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_NEXTINDEX - OFFSET_RANDOMDATA;	

	private boolean mDeliveredPart;
	private String mMessageBody;
	private long mIndexNext;
	
	FileEntryMessagePart(boolean deliveredPart,
	                     String messageBody,
	                     long indexNext) {
		setDeliveredPart(deliveredPart);
		setMessageBody(messageBody);
		setIndexNext(indexNext);
	}
	
	long getIndexNext() {
		return mIndexNext;
	}

	void setIndexNext(long indexNext) {
	    if (indexNext > 0xFFFFFFFFL || indexNext < 0L)
	    	throw new IndexOutOfBoundsException();
		
		this.mIndexNext = indexNext;
	}
	
	void setDeliveredPart(boolean deliveredPart) {
		this.mDeliveredPart = deliveredPart;
	}

	boolean getDeliveredPart() {
		return mDeliveredPart;
	}

	void setMessageBody(String messageBody) {
		this.mMessageBody = messageBody;
	}

	String getMessageBody() {
		return mMessageBody;
	}

	static byte[] createData(FileEntryMessagePart msgPart) throws DatabaseFileException {
		ByteBuffer msgBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		if (msgPart.mDeliveredPart)
			flags |= (byte) ((1 << 7) & 0xFF);
		msgBuffer.put(flags);
		
		// message body
		msgBuffer.put(Database.toLatin(msgPart.mMessageBody, LENGTH_MESSAGEBODY));

		// random data
		msgBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		msgBuffer.put(Database.getBytes(msgPart.mIndexNext));
		
		return Encryption.encryptSymmetric(msgBuffer.array(), Encryption.retreiveEncryptionKey());
	}
	
	static FileEntryMessagePart parseData(byte[] dataEncrypted) throws DatabaseFileException {
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		byte flags = dataPlain[OFFSET_FLAGS];
		boolean deliveredPart = ((flags & (1 << 7)) == 0) ? false : true;
		
		return new FileEntryMessagePart(deliveredPart, 
		                            Database.fromLatin(dataPlain, OFFSET_MESSAGEBODY, LENGTH_MESSAGEBODY), 
		                            Database.getInt(dataPlain, OFFSET_NEXTINDEX)
		                           );
	}
}
