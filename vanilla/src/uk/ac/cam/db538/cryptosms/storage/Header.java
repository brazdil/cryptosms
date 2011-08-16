package uk.ac.cam.db538.cryptosms.storage;

import java.nio.ByteBuffer;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

/**
 * 
 * Class representing the header entry in the secure storage file.
 * 
 * @author David Brazdil
 *
 */
public class Header {
	static final int CURRENT_VERSION = 1;
	
	private static final int INDEX_HEADER = 0;
	
	// FILE FORMAT
	private static final int LENGTH_PLAIN_HEADER = 4;
	private static final int LENGTH_RANDOM_STUFF = Encryption.SYM_BLOCK_LENGTH - LENGTH_PLAIN_HEADER; 	// for alignment
	private static final int OFFSET_ENCRYPTED_HEADER = LENGTH_PLAIN_HEADER + LENGTH_RANDOM_STUFF;  
	private static final int LENGTH_ENCRYPTED_HEADER = Storage.ENCRYPTED_ENTRY_SIZE - OFFSET_ENCRYPTED_HEADER;
	private static final int LENGTH_ENCRYPTED_HEADER_WITH_OVERHEAD = LENGTH_ENCRYPTED_HEADER + Encryption.SYM_OVERHEAD;

	private static final int OFFSET_CONVINDEX = LENGTH_ENCRYPTED_HEADER - 4;
	private static final int OFFSET_FREEINDEX = OFFSET_CONVINDEX - 4;
	
	// CACHING
	private static Header cacheHeader = null;
	
	/**
	 * Removes all instances from the list of cached objects.
	 * Be sure you don't use the instances afterwards.
	 */
	public static void forceClearCache() {
		cacheHeader = null;
	}
	
	/**
	 * Returns an instance of Header class.
	 * @return
	 * @throws StorageFileException 
	 */
	public static Header getHeader() throws StorageFileException {
		if (cacheHeader == null) 
			cacheHeader = new Header(true);
		return cacheHeader;
	}
	
	/**
	 * Only to be called from within Database.createFile()
	 * Forces the header to be created with default values and written to the file.
	 * @throws StorageFileException 
	 */
	static Header createHeader() throws StorageFileException {
		cacheHeader = new Header(false);
		return cacheHeader;
	}
	
	// INTERNAL FIELDS
	private long mIndexEmpty;
	private long mIndexConversations;
	private int mVersion;
	
	/**
	 * Constructor
	 * @param readFromFile	Does this entry already exist in the file?
	 * @throws StorageFileException
	 */
	private Header(boolean readFromDisk) throws StorageFileException {
		if (readFromDisk) {
			// read bytes from file
			byte[] dataAll = Storage.getStorage().getEntry(INDEX_HEADER);
			
			// check the first three bytes, looking for SMS in ASCII
			if (dataAll[0] != (byte) 0x53 ||
			    dataAll[1] != (byte) 0x4D ||
			    dataAll[2] != (byte) 0x53
			   )
				throw new StorageFileException("Not an SMS history file");
			
			// get the version
			int version = 0 | (dataAll[3] & 0xFF);

			// decrypt rest of  data
			byte[] dataEncrypted = new byte[LENGTH_ENCRYPTED_HEADER_WITH_OVERHEAD];
			System.arraycopy(dataAll, OFFSET_ENCRYPTED_HEADER, dataEncrypted, 0, LENGTH_ENCRYPTED_HEADER_WITH_OVERHEAD);
			byte[] dataPlain;
			try {
				dataPlain = Encryption.getEncryption().decryptSymmetricWithMasterKey(dataEncrypted);
			} catch (EncryptionException e) {
				throw new StorageFileException(e);
			}
			
			// set fields
			setVersion(version);
			setIndexEmpty(LowLevel.getUnsignedInt(dataPlain, OFFSET_FREEINDEX));
			setIndexConversations(LowLevel.getUnsignedInt(dataPlain, OFFSET_CONVINDEX));
		}
		else {
			// default values			
			setVersion(CURRENT_VERSION);
			setIndexEmpty(0L);
			setIndexConversations(0L);
			
			saveToFile();
		}
	}

	/**
	 * Save data to the storage file
	 * @throws StorageFileException
	 */
	public void saveToFile() throws StorageFileException {
		ByteBuffer headerBuffer = ByteBuffer.allocate(LENGTH_ENCRYPTED_HEADER);
		headerBuffer.put(Encryption.getEncryption().generateRandomData(LENGTH_ENCRYPTED_HEADER - 8));
		headerBuffer.put(LowLevel.getBytesUnsignedInt(this.getIndexEmpty())); 
		headerBuffer.put(LowLevel.getBytesUnsignedInt(this.getIndexConversations()));
		
		ByteBuffer headerBufferEncrypted = ByteBuffer.allocate(Storage.CHUNK_SIZE);
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) 0x4D); // M
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) (this.getVersion() & 0xFF)); // version
		headerBufferEncrypted.put(Encryption.getEncryption().generateRandomData(LENGTH_RANDOM_STUFF)); // random stuff
		try {
			headerBufferEncrypted.put(Encryption.getEncryption().encryptSymmetricWithMasterKey(headerBuffer.array()));
		} catch (EncryptionException e) {
			throw new StorageFileException(e);
		}
		
		Storage.getStorage().setEntry(INDEX_HEADER, headerBufferEncrypted.array());
	}
	
	/**
	 * Return instance of the first object in the empty-entry stack
	 * @return
	 * @throws StorageFileException
	 */
	public Empty getFirstEmpty() throws StorageFileException {
		if (this.mIndexEmpty == 0)
			return null;
		else
			return Empty.getEmpty(this.mIndexEmpty);
	}
	
	/**
	 * Return instance of the first object in the conversations linked list
	 * @return
	 * @throws StorageFileException
	 */
	public Conversation getFirstConversation() throws StorageFileException {
		if (this.mIndexConversations == 0) 
			return null;
		else
			return Conversation.getConversation(this.mIndexConversations);
	}

	/**
	 * Insert new element into the linked list of conversations
	 * @param conv
	 * @throws StorageFileException
	 */
	void attachConversation(Conversation conv) throws StorageFileException {
		long indexFirstInStack = getIndexConversations();
		if (indexFirstInStack != 0) {
			Conversation first = Conversation.getConversation(indexFirstInStack);
			first.setIndexPrev(conv.getEntryIndex());
			first.saveToFile();
		}

		conv.setIndexNext(indexFirstInStack);
		conv.setIndexPrev(0L);
		conv.saveToFile();
		
		this.setIndexConversations(conv.getEntryIndex());
		this.saveToFile();
	}

	/**
	 * Insert new element into the stack of empty entries
	 * @param conv
	 * @throws StorageFileException
	 */
	void attachEmpty(Empty empty) throws StorageFileException {
		long indexFirstInStack = getIndexEmpty();
		empty.setIndexNext(indexFirstInStack);
		empty.saveToFile();
		
		this.setIndexEmpty(empty.getEntryIndex());
		this.saveToFile();
	}

	// GETTERS / SETTERS
	
	long getIndexEmpty() {
		return mIndexEmpty;
	}

	void setIndexEmpty(long indexEmpty) {
		if (indexEmpty > 0xFFFFFFFFL || indexEmpty < 0L)
			throw new IndexOutOfBoundsException();
		mIndexEmpty = indexEmpty;
	}

	long getIndexConversations() {
		return mIndexConversations;
	}

	void setIndexConversations(long indexConversations) {
		if (indexConversations > 0xFFFFFFFFL || indexConversations < 0L)
			throw new IndexOutOfBoundsException();
		
		mIndexConversations = indexConversations;
	}

	int getVersion() {
		return mVersion;
	}

	void setVersion(int version) {
		if (version > 0xFF)
			throw new IndexOutOfBoundsException();
		
		mVersion = version;
	}
}
