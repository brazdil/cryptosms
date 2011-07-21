package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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
	 * Returns an instance of Header class. Locks the file
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
	 * @param lockAllow 		Lock the file if data not cached
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
	 * @throws IOException 
	 * @throws DatabaseFileException 
	 */
	static Header createHeader() throws DatabaseFileException, IOException {
		return createHeader(true);
	}

	static Header createHeader(boolean lockAllow) throws DatabaseFileException, IOException {
		cacheHeader = new Header(false, lockAllow);
		return cacheHeader;
	}
	
	// INTERNAL FIELDS
	private long mIndexFree;
	private long mIndexConversations;
	private int mVersion;
	
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
			setIndexFree(Database.getInt(dataPlain, OFFSET_FREEINDEX));
			setIndexConversations(Database.getInt(dataPlain, OFFSET_CONVINDEX));
		}
		else {
			// default values			
			setVersion(CURRENT_VERSION);
			setIndexFree(0L);
			setIndexConversations(0L);
			
			saveToFile(lockAllow);
		}
	}

	public void saveToFile() throws DatabaseFileException, IOException {
		saveToFile(true);
	}
	
	public void saveToFile(boolean lock) throws DatabaseFileException, IOException {
		ByteBuffer headerBuffer = ByteBuffer.allocate(LENGTH_ENCRYPTED_HEADER);
		headerBuffer.put(Encryption.generateRandomData(LENGTH_ENCRYPTED_HEADER - 8));
		headerBuffer.put(Database.getBytes(this.getIndexFree())); 
		headerBuffer.put(Database.getBytes(this.getIndexConversations()));
		
		ByteBuffer headerBufferEncrypted = ByteBuffer.allocate(Database.CHUNK_SIZE);
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) 0x4D); // M
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) (this.getVersion() & 0xFF)); // version
		headerBufferEncrypted.put(Encryption.encryptSymmetric(headerBuffer.array(), Encryption.retreiveEncryptionKey()));
		
		Database.getDatabase().setEntry(INDEX_HEADER, headerBufferEncrypted.array(), lock);
	}
	
	public Empty getFirstEmpty() throws DatabaseFileException, IOException {
		if (this.mIndexFree == 0)
			return null;
		else
			return Empty.getEmpty(this.mIndexFree);
	}
	
	public Conversation getFirstConversation() throws DatabaseFileException, IOException {
		if (this.mIndexConversations == 0) 
			return null;
		else
			return Conversation.getConversation(this.mIndexConversations);
	}
	
	// GETTERS / SETTERS
	
	long getIndexFree() {
		return mIndexFree;
	}

	void setIndexFree(long indexFree) {
		if (indexFree > 0xFFFFFFFFL || indexFree < 0L)
			throw new IndexOutOfBoundsException();
		mIndexFree = indexFree;
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
