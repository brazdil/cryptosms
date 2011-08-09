package uk.ac.cam.db538.securesms.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.crypto.Encryption;
import uk.ac.cam.db538.securesms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.securesms.data.LowLevel;

/**
 * 
 * Class representing a message part entry in the secure storage file.
 * This should not be accessible outside the package. Message has API for handling it seamlessly
 * 
 * @author David Brazdil
 *
 */
class MessageDataPart {
	// FILE FORMAT
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_MESSAGEBODYLEN = 2;
	private static final int LENGTH_MESSAGEBODY = 140;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_MESSAGEBODYLEN = OFFSET_FLAGS + LENGTH_FLAGS;
	private static final int OFFSET_MESSAGEBODY = OFFSET_MESSAGEBODYLEN + LENGTH_MESSAGEBODYLEN;

	private static final int OFFSET_RANDOMDATA = OFFSET_MESSAGEBODY + LENGTH_MESSAGEBODY;

	private static final int OFFSET_NEXTINDEX = Storage.ENCRYPTED_ENTRY_SIZE - 4;
	private static final int OFFSET_PREVINDEX = OFFSET_NEXTINDEX - 4;
	private static final int OFFSET_PARENTINDEX = OFFSET_PREVINDEX - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_PARENTINDEX - OFFSET_RANDOMDATA;	
	
	// STATIC
	
	private static ArrayList<MessageDataPart> cacheMessageDataPart = new ArrayList<MessageDataPart>();
	
	/**
	 * Removes all instances from the list of cached objects.
	 * Be sure you don't use the instances afterwards.
	 */
	public static void forceClearCache() {
		synchronized (cacheMessageDataPart) {
			cacheMessageDataPart = new ArrayList<MessageDataPart>();
		}
	}
	
	/**
	 * Replaces an empty entry with new MessagePart
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	static MessageDataPart createMessageDataPart() throws StorageFileException, IOException {
		return createMessageDataPart(true);
	}
	
	/**
	 * Replaces an empty entry with new MessagePart
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	static MessageDataPart createMessageDataPart(boolean lockAllow) throws StorageFileException, IOException {
		return new MessageDataPart(Empty.getEmptyIndex(lockAllow), false, lockAllow);
	}

	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 */
	static MessageDataPart getMessageDataPart(long index) throws StorageFileException, IOException {
		return getMessageDataPart(index, true);
	}
	
	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index			Index in file
	 * @param lockAllow		File lock allow
	 */
	static MessageDataPart getMessageDataPart(long index, boolean lockAllow) throws StorageFileException, IOException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheMessageDataPart) {
			for (MessageDataPart msgPart: cacheMessageDataPart)
				if (msgPart.getEntryIndex() == index)
					return msgPart; 
		}
		// create a new one
		return new MessageDataPart(index, true, lockAllow);
	}
	
	// INTERNAL FIELDS
	private long mEntryIndex; // READ ONLY
	private boolean mDeliveredPart;
	private byte[] mMessageBody;
	private long mIndexParent;
	private long mIndexPrev;
	private long mIndexNext;
	
	// CONSTRUCTORS
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @throws StorageFileException
	 * @throws IOException
	 */
	private MessageDataPart(long index, boolean readFromFile) throws StorageFileException, IOException {
		this(index, readFromFile, true);
	}
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @param lockAllow		Allow the file to be locked
	 * @throws StorageFileException
	 * @throws IOException
	 */
	private MessageDataPart(long index, boolean readFromFile, boolean lockAllow) throws StorageFileException, IOException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Storage.getDatabase().getEntry(index, lockAllow);
			byte[] dataPlain;
			try {
				dataPlain = Encryption.getSingleton().decryptSymmetricWithMasterKey(dataEncrypted);
			} catch (EncryptionException e) {
				throw new StorageFileException(e);
			}
			
			byte flags = dataPlain[OFFSET_FLAGS];
			boolean deliveredPart = ((flags & (1 << 7)) == 0) ? false : true;
			
			setDeliveredPart(deliveredPart);
			int messageBodyLength = Math.min(LENGTH_MESSAGEBODY, LowLevel.getUnsignedShort(dataPlain, OFFSET_MESSAGEBODYLEN));
			setMessageBody(LowLevel.cutData(dataPlain, OFFSET_MESSAGEBODY, messageBodyLength));
			setIndexParent(LowLevel.getUnsignedInt(dataPlain, OFFSET_PARENTINDEX));
			setIndexPrev(LowLevel.getUnsignedInt(dataPlain, OFFSET_PREVINDEX));
			setIndexNext(LowLevel.getUnsignedInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			setDeliveredPart(false);
			setMessageBody(new byte[0]);
			setIndexParent(0L);
			setIndexPrev(0L);
			setIndexNext(0L);
			
			saveToFile(lockAllow);
		}

		synchronized (cacheMessageDataPart) {
			cacheMessageDataPart.add(this);
		}
	}

	// FUNCTIONS
	
	/**
	 * Save contents of the class to the storage file
	 * @throws StorageFileException
	 * @throws IOException
	 */
	void saveToFile() throws StorageFileException, IOException {
		saveToFile(true);
	}
	
	/**
	 * Save contents of the class to the storage file
	 * @param lockAllow		Allow the file to be locked
	 * @throws StorageFileException
	 * @throws IOException
	 */
	void saveToFile(boolean lock) throws StorageFileException, IOException {
		ByteBuffer msgBuffer = ByteBuffer.allocate(Storage.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		if (this.mDeliveredPart)
			flags |= (byte) ((1 << 7) & 0xFF);
		msgBuffer.put(flags);
		
		// message body
		msgBuffer.put(LowLevel.getBytesUnsignedShort(this.mMessageBody.length));
		msgBuffer.put(LowLevel.wrapData(mMessageBody, LENGTH_MESSAGEBODY));

		// random data
		msgBuffer.put(Encryption.getSingleton().generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		msgBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexParent));
		msgBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexPrev));
		msgBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexNext));
		
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = Encryption.getSingleton().encryptSymmetricWithMasterKey(msgBuffer.array());
		} catch (EncryptionException e) {
			throw new StorageFileException(e);
		}
		Storage.getDatabase().setEntry(mEntryIndex, dataEncrypted, lock);
	}

	/**
	 * Returns Message that is a parent to this MessagePart in the data structure
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	MessageData getParent() throws StorageFileException, IOException {
		return getParent(true);
	}
	
	/**
	 * Returns Message that is a parent to this MessagePart in the data structure
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	MessageData getParent(boolean lockAllow) throws StorageFileException, IOException {
		return MessageData.getMessageData(mIndexParent, lockAllow);
	}

	/**
	 * Returns next MessagePart in the linked list, or null if there isn't any
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	MessageDataPart getPreviousMessageDataPart() throws StorageFileException, IOException {
		return getPreviousMessageDataPart(true);
	}
	
	/**
	 * Returns next MessagePart in the linked list, or null if there isn't any
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	MessageDataPart getPreviousMessageDataPart(boolean lockAllow) throws StorageFileException, IOException {
		return MessageDataPart.getMessageDataPart(mIndexPrev, lockAllow);
	}
	
	/**
	 * Returns next MessagePart in the linked list, or null if there isn't any
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	MessageDataPart getNextMessageDataPart() throws StorageFileException, IOException {
		return getNextMessageDataPart(true);
	}
	
	/**
	 * Returns next MessagePart in the linked list, or null if there isn't any
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	MessageDataPart getNextMessageDataPart(boolean lockAllow) throws StorageFileException, IOException {
		return getMessageDataPart(mIndexNext, lockAllow);
	}

	/**
	 * Replace the file space with Empty entry
	 * @throws IOException
	 * @throws StorageFileException
	 */
	void delete() throws IOException, StorageFileException {
		delete(true);
	}
	
	/**
	 * Replace the file space with Empty entry
	 * @param lockAllow		Allow the file to be locked
	 * @throws IOException
	 * @throws StorageFileException
	 */
	void delete(boolean lockAllow) throws IOException, StorageFileException {
		Storage db = Storage.getDatabase();
		
		db.lockFile(lockAllow);
		try {
			MessageDataPart prev = this.getPreviousMessageDataPart(false);
			MessageDataPart next = this.getNextMessageDataPart(false); 
	
			if (prev != null) {
				// this is not the first message part in the list
				// update the previous one
				prev.setIndexNext(this.getIndexNext());
				prev.saveToFile(false);
			} else {
				// this IS the first message part in the list
				// update parent
				MessageData parent = this.getParent(false);
				parent.setIndexMessageParts(this.getIndexNext());
				parent.saveToFile(false);
			}
			
			// update next one
			if (next != null) {
				next.setIndexPrev(this.getIndexPrev());
				next.saveToFile(false);
			}
			
			// delete this message
			Empty.replaceWithEmpty(mEntryIndex, false);
					
			// remove from cache
			synchronized (cacheMessageDataPart) {
				cacheMessageDataPart.remove(this);
			}
			
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

	void setMessageBody(byte[] messageBody) {
		this.mMessageBody = messageBody;
	}

	byte[] getMessageBody() {
		return mMessageBody;
	}

	long getIndexParent() {
		return mIndexParent;
	}

	void setIndexParent(long indexParent) {
	    if (indexParent > 0xFFFFFFFFL || indexParent < 0L)
	    	throw new IndexOutOfBoundsException();
		
		this.mIndexParent = indexParent;
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
