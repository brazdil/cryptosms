package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing a message part entry in the secure storage file.
 * This should not be accessible outside the package. Message has API for handling it seamlessly
 * 
 * @author David Brazdil
 *
 */
class MessagePart {
	// FILE FORMAT
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_MESSAGEBODY = 140;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_MESSAGEBODY = OFFSET_FLAGS + LENGTH_FLAGS;

	private static final int OFFSET_RANDOMDATA = OFFSET_MESSAGEBODY + LENGTH_MESSAGEBODY;

	private static final int OFFSET_NEXTINDEX = Database.ENCRYPTED_ENTRY_SIZE - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_NEXTINDEX - OFFSET_RANDOMDATA;	
	
	// STATIC
	
	private static ArrayList<MessagePart> cacheMessagePart = new ArrayList<MessagePart>();
	
	/**
	 * Removes all instances from the list of cached objects.
	 * Be sure you don't use the instances afterwards.
	 */
	public static void forceClearCache() {
		synchronized (cacheMessagePart) {
			cacheMessagePart = new ArrayList<MessagePart>();
		}
	}
	
	/**
	 * Replaces an empty entry with new MessagePart
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	static MessagePart createMessagePart() throws DatabaseFileException, IOException {
		return createMessagePart(true);
	}
	
	/**
	 * Replaces an empty entry with new MessagePart
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	static MessagePart createMessagePart(boolean lockAllow) throws DatabaseFileException, IOException {
		return new MessagePart(Empty.getEmptyIndex(lockAllow), false, lockAllow);
	}

	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 */
	static MessagePart getMessagePart(long index) throws DatabaseFileException, IOException {
		return getMessagePart(index, true);
	}
	
	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index			Index in file
	 * @param lockAllow		File lock allow
	 */
	static MessagePart getMessagePart(long index, boolean lockAllow) throws DatabaseFileException, IOException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheMessagePart) {
			for (MessagePart msgPart: cacheMessagePart)
				if (msgPart.getEntryIndex() == index)
					return msgPart; 
		}
		// create a new one
		return new MessagePart(index, true, lockAllow);
	}
	
	// INTERNAL FIELDS
	private long mEntryIndex; // READ ONLY
	private boolean mDeliveredPart;
	private String mMessageBody;
	private long mIndexNext;
	
	// CONSTRUCTORS
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	private MessagePart(long index, boolean readFromFile) throws DatabaseFileException, IOException {
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
	private MessagePart(long index, boolean readFromFile, boolean lockAllow) throws DatabaseFileException, IOException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Database.getDatabase().getEntry(index, lockAllow);
			byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
			
			byte flags = dataPlain[OFFSET_FLAGS];
			boolean deliveredPart = ((flags & (1 << 7)) == 0) ? false : true;
			
			setDeliveredPart(deliveredPart);
			setMessageBody(Database.fromLatin(dataPlain, OFFSET_MESSAGEBODY, LENGTH_MESSAGEBODY));
			setIndexNext(Database.getInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			setDeliveredPart(false);
			setMessageBody("");
			setIndexNext(0L);
			
			saveToFile(lockAllow);
		}

		synchronized (cacheMessagePart) {
			cacheMessagePart.add(this);
		}
	}

	// FUNCTIONS
	
	/**
	 * Save contents of the class to the storage file
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	void saveToFile() throws DatabaseFileException, IOException {
		saveToFile(true);
	}
	
	/**
	 * Save contents of the class to the storage file
	 * @param lockAllow		Allow the file to be locked
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	void saveToFile(boolean lock) throws DatabaseFileException, IOException {
		ByteBuffer msgBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		if (this.mDeliveredPart)
			flags |= (byte) ((1 << 7) & 0xFF);
		msgBuffer.put(flags);
		
		// message body
		msgBuffer.put(Database.toLatin(this.mMessageBody, LENGTH_MESSAGEBODY));

		// random data
		msgBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		msgBuffer.put(Database.getBytes(this.mIndexNext));
		
		byte[] dataEncrypted = Encryption.encryptSymmetric(msgBuffer.array(), Encryption.retreiveEncryptionKey());
		Database.getDatabase().setEntry(mEntryIndex, dataEncrypted, lock);
	}

	/**
	 * Returns next MessagePart in the linked list, or null if there isn't any
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	MessagePart getNextMessagePart() throws DatabaseFileException, IOException {
		return getNextMessagePart(true);
	}
	
	/**
	 * Returns next MessagePart in the linked list, or null if there isn't any
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	MessagePart getNextMessagePart(boolean lockAllow) throws DatabaseFileException, IOException {
		return getMessagePart(mIndexNext, lockAllow);
	}
	
	/**
	 * Replace the file space with Empty entry
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	void delete() throws IOException, DatabaseFileException {
		delete(true);
	}
	
	/**
	 * Replace the file space with Empty entry
	 * @param lockAllow		Allow the file to be locked
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	void delete(boolean lockAllow) throws IOException, DatabaseFileException {
		// remove yourself from the cache
		synchronized (cacheMessagePart) {
			cacheMessagePart.remove(this);			
		}
		// replace your file chunk by empty entry and attach yourself to the header
		Header.getHeader().attachEmpty(Empty.replaceWithEmpty(this.mEntryIndex));
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

	void setMessageBody(String messageBody) {
		this.mMessageBody = messageBody;
	}

	String getMessageBody() {
		return mMessageBody;
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
