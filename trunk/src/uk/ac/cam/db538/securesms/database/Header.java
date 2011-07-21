package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;

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
	private static final int LENGTH_ENCRYPTED_HEADER = Database.ENCRYPTED_ENTRY_SIZE - LENGTH_PLAIN_HEADER;
	private static final int LENGTH_ENCRYPTED_HEADER_WITH_OVERHEAD = LENGTH_ENCRYPTED_HEADER + Encryption.ENCRYPTION_OVERHEAD;

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
	 * Returns an instance of Header class. Locks the file if necessary
	 * @return
	 * @throws IOException 
	 * @throws DatabaseFileException 
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public static Header getHeader() throws DatabaseFileException, IOException {
		return getHeader(true);
	}
	
	/**
	 * Returns an instance of Header class
	 * @param lockAllow 		Lock the file if necessary
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public static Header getHeader(boolean lockAllow) throws DatabaseFileException, IOException {
		if (cacheHeader == null) 
			cacheHeader = new Header(true, lockAllow);
		return cacheHeader;
	}
	
	/**
	 * Only to be called from within Database.createFile()
	 * Forces the header to be created with default values and written to the file.
	 * @throws IOException 
	 * @throws DatabaseFileException 
	 */
	static Header createHeader() throws DatabaseFileException, IOException {
		return createHeader(true);
	}

	/**
	 * Only to be called from within Database.createFile()
	 * Forces the header to be created with default values and written to the file.
	 * @param lockAllow 		Lock the file if necessary
	 * @throws IOException 
	 * @throws DatabaseFileException 
	 */
	static Header createHeader(boolean lockAllow) throws DatabaseFileException, IOException {
		cacheHeader = new Header(false, lockAllow);
		return cacheHeader;
	}
	
	// INTERNAL FIELDS
	private long mIndexEmpty;
	private long mIndexConversations;
	private int mVersion;
	
	/**
	 * Constructor
	 * @param readFromFile	Does this entry already exist in the file?
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	private Header(boolean readFromDisk) throws DatabaseFileException, IOException {
		this(readFromDisk, true);
	}
	
	/**
	 * Constructor
	 * @param readFromFile	Does this entry already exist in the file?
	 * @param lockAllow		Allow the file to be locked
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	private Header(boolean readFromDisk, boolean lockAllow) throws DatabaseFileException, IOException {
		if (readFromDisk) {
			// read bytes from file
			byte[] dataAll = Database.getDatabase().getEntry(INDEX_HEADER, lockAllow);
			
			// check the first three bytes, looking for SMS in ASCII
			if (dataAll[0] != (byte) 0x53 ||
			    dataAll[1] != (byte) 0x4D ||
			    dataAll[2] != (byte) 0x53
			   )
				throw new DatabaseFileException("Not an SMS history file");
			
			// get the version
			int version = 0 | (dataAll[3] & 0xFF);

			// decrypt rest of  data
			byte[] dataEncrypted = new byte[LENGTH_ENCRYPTED_HEADER_WITH_OVERHEAD];
			System.arraycopy(dataAll, LENGTH_PLAIN_HEADER, dataEncrypted, 0, LENGTH_ENCRYPTED_HEADER_WITH_OVERHEAD);
			byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
			
			// set fields
			setVersion(version);
			setIndexEmpty(Database.getInt(dataPlain, OFFSET_FREEINDEX));
			setIndexConversations(Database.getInt(dataPlain, OFFSET_CONVINDEX));
		}
		else {
			// default values			
			setVersion(CURRENT_VERSION);
			setIndexEmpty(0L);
			setIndexConversations(0L);
			
			saveToFile(lockAllow);
		}
	}

	/**
	 * Save data to the storage file
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void saveToFile() throws DatabaseFileException, IOException {
		saveToFile(true);
	}
	
	/**
	 * Save data to the storage file
	 * @param lock		Allow the file to be locked
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void saveToFile(boolean lock) throws DatabaseFileException, IOException {
		ByteBuffer headerBuffer = ByteBuffer.allocate(LENGTH_ENCRYPTED_HEADER);
		headerBuffer.put(Encryption.generateRandomData(LENGTH_ENCRYPTED_HEADER - 8));
		headerBuffer.put(Database.getBytes(this.getIndexEmpty())); 
		headerBuffer.put(Database.getBytes(this.getIndexConversations()));
		
		ByteBuffer headerBufferEncrypted = ByteBuffer.allocate(Database.CHUNK_SIZE);
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) 0x4D); // M
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) (this.getVersion() & 0xFF)); // version
		headerBufferEncrypted.put(Encryption.encryptSymmetric(headerBuffer.array(), Encryption.retreiveEncryptionKey()));
		
		Database.getDatabase().setEntry(INDEX_HEADER, headerBufferEncrypted.array(), lock);
	}
	
	/**
	 * Return instance of the first object in the empty-entry stack
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Empty getFirstEmpty() throws DatabaseFileException, IOException {
		return getFirstEmpty(true);
	}

	/**
	 * Return instance of the first object in the empty-entry stack
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws IOException 
	 * @throws DatabaseFileException 
	 */
	public Empty getFirstEmpty(boolean lockAllow) throws DatabaseFileException, IOException {
		if (this.mIndexEmpty == 0)
			return null;
		else
			return Empty.getEmpty(this.mIndexEmpty, lockAllow);
	}
	
	/**
	 * Return instance of the first object in the conversations linked list
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation getFirstConversation() throws DatabaseFileException, IOException {
		return getFirstConversation(true);
	}
	
	/**
	 * Return instance of the first object in the conversations linked list
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public Conversation getFirstConversation(boolean lockAllow) throws DatabaseFileException, IOException {
		if (this.mIndexConversations == 0) 
			return null;
		else
			return Conversation.getConversation(this.mIndexConversations, lockAllow);
	}

	/**
	 * Insert new element into the linked list of conversations
	 * @param conv
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	public void attachConversation(Conversation conv) throws IOException, DatabaseFileException {
		attachConversation(conv, true);
	}
	
	/**
	 * Insert new element into the linked list of conversations
	 * @param lockAllow		Allow the file to be locked
	 * @param conv
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	public void attachConversation(Conversation conv, boolean lockAllow) throws IOException, DatabaseFileException {
		Database.getDatabase().lockFile(lockAllow);
		try {
			long indexFirstInStack = getIndexConversations();
			if (indexFirstInStack != 0) {
				Conversation first = Conversation.getConversation(indexFirstInStack, false);
				first.setIndexPrev(conv.getEntryIndex());
				first.saveToFile(false);
			}

			conv.setIndexNext(indexFirstInStack);
			conv.setIndexPrev(0L);
			conv.saveToFile(false);
			
			this.setIndexConversations(conv.getEntryIndex());
			this.saveToFile(false);
		} catch (DatabaseFileException ex) {
			throw new DatabaseFileException(ex.getMessage());
		} catch (IOException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			Database.getDatabase().unlockFile(lockAllow);	
		}
	}

	/**
	 * Insert new element into the stack of empty entries
	 * @param conv
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	public void attachEmpty(Empty empty) throws IOException, DatabaseFileException {
		attachEmpty(empty, true);
	}
	
	/**
	 * Insert new element into the stack of empty entries
	 * @param lockAllow		Allow the file to be locked
	 * @param conv
	 * @throws IOException
	 * @throws DatabaseFileException
	 */
	public void attachEmpty(Empty empty, boolean lockAllow) throws IOException, DatabaseFileException {
		Database.getDatabase().lockFile(lockAllow);
		try {
			long indexFirstInStack = getIndexEmpty();
			empty.setIndexNext(indexFirstInStack);
			empty.saveToFile(false);
			
			this.setIndexEmpty(empty.getEntryIndex());
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
