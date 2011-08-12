package uk.ac.cam.db538.cryptosms.storage;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

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
	 */
	static MessageDataPart createMessageDataPart() throws StorageFileException {
		return new MessageDataPart(Empty.getEmptyIndex(), false);
	}

	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 */
	static MessageDataPart getMessageDataPart(long index) throws StorageFileException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheMessageDataPart) {
			for (MessageDataPart msgPart: cacheMessageDataPart)
				if (msgPart.getEntryIndex() == index)
					return msgPart; 
		}
		// create a new one
		return new MessageDataPart(index, true);
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
	 */
	private MessageDataPart(long index, boolean readFromFile) throws StorageFileException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Storage.getStorage().getEntry(index);
			byte[] dataPlain;
			try {
				dataPlain = Encryption.getEncryption().decryptSymmetricWithMasterKey(dataEncrypted);
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
			
			saveToFile();
		}

		synchronized (cacheMessageDataPart) {
			cacheMessageDataPart.add(this);
		}
	}

	// FUNCTIONS
	
	/**
	 * Save contents of the class to the storage file
	 * @throws StorageFileException
	 */
	void saveToFile() throws StorageFileException {
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
		msgBuffer.put(Encryption.getEncryption().generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		msgBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexParent));
		msgBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexPrev));
		msgBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexNext));
		
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = Encryption.getEncryption().encryptSymmetricWithMasterKey(msgBuffer.array());
		} catch (EncryptionException e) {
			throw new StorageFileException(e);
		}
		Storage.getStorage().setEntry(mEntryIndex, dataEncrypted);
	}

	/**
	 * Returns Message that is a parent to this MessagePart in the data structure
	 * @return
	 * @throws StorageFileException
	 */
	MessageData getParent() throws StorageFileException {
		return MessageData.getMessageData(mIndexParent);
	}

	/**
	 * Returns next MessagePart in the linked list, or null if there isn't any
	 * @return
	 * @throws StorageFileException
	 */
	MessageDataPart getPreviousMessageDataPart() throws StorageFileException {
		return MessageDataPart.getMessageDataPart(mIndexPrev);
	}
	
	/**
	 * Returns next MessagePart in the linked list, or null if there isn't any
	 * @return
	 * @throws StorageFileException
	 */
	MessageDataPart getNextMessageDataPart() throws StorageFileException {
		return getMessageDataPart(mIndexNext);
	}

	/**
	 * Replace the file space with Empty entry
	 * @throws StorageFileException
	 */
	void delete() throws StorageFileException {
		MessageDataPart prev = this.getPreviousMessageDataPart();
		MessageDataPart next = this.getNextMessageDataPart(); 

		if (prev != null) {
			// this is not the first message part in the list
			// update the previous one
			prev.setIndexNext(this.getIndexNext());
			prev.saveToFile();
		} else {
			// this IS the first message part in the list
			// update parent
			MessageData parent = this.getParent();
			parent.setIndexMessageParts(this.getIndexNext());
			parent.saveToFile();
		}
		
		// update next one
		if (next != null) {
			next.setIndexPrev(this.getIndexPrev());
			next.saveToFile();
		}
		
		// delete this message
		Empty.replaceWithEmpty(mEntryIndex);
				
		// remove from cache
		synchronized (cacheMessageDataPart) {
			cacheMessageDataPart.remove(this);
		}
		
		// make this instance invalid
		this.mEntryIndex = -1L;
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
