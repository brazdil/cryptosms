package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;

public class FileEntryHeader {
	static final int CURRENT_VERSION = 1;
	
	static final int LENGTH_PLAIN_HEADER = 4;
	static final int LENGTH_ENCRYPTED_HEADER = Database.ENCRYPTED_ENTRY_SIZE - LENGTH_PLAIN_HEADER;
	static final int LENGTH_ENCRYPTED_HEADER_WITH_OVERHEAD = LENGTH_ENCRYPTED_HEADER + Encryption.ENCRYPTION_OVERHEAD;

	private static final int OFFSET_CONVINDEX = LENGTH_ENCRYPTED_HEADER - 4;
	private static final int OFFSET_FREEINDEX = OFFSET_CONVINDEX - 4;

	private long mIndexFree;
	private long mIndexConversations;
	private int mVersion;
	
	FileEntryHeader(long indexFree, long indexConversations) {
		this(CURRENT_VERSION, indexFree, indexConversations);
	}
	
	FileEntryHeader(int version, long indexFree, long indexConversations) {
		if (indexFree > 0xFFFFFFFFL || 
			indexConversations > 0xFFFFFFFFL ||
			version > 0xFF)
			throw new IndexOutOfBoundsException();
		
		mIndexFree = indexFree;
		mIndexConversations = indexConversations;
		mVersion = version;
	}

	public long getIndexFree() {
		return mIndexFree;
	}

	public void setIndexFree(long indexFree) {
		mIndexFree = indexFree;
	}

	public long getIndexConversations() {
		return mIndexConversations;
	}

	public void setIndexConversations(long indexConversations) {
		mIndexConversations = indexConversations;
	}

	public int getVersion() {
		return mVersion;
	}

	public void setVersion(int version) {
		mVersion = version;
	}

	static byte[] createData(FileEntryHeader header) {
		ByteBuffer headerBuffer = ByteBuffer.allocate(LENGTH_ENCRYPTED_HEADER);
		headerBuffer.put(Encryption.generateRandomData(LENGTH_ENCRYPTED_HEADER - 8));
		headerBuffer.put(Database.getBytes(header.mIndexFree)); 
		headerBuffer.put(Database.getBytes(header.mIndexConversations));
		
		ByteBuffer headerBufferEncrypted = ByteBuffer.allocate(Database.CHUNK_SIZE);
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) 0x4D); // M
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) (header.mVersion & 0xFF)); // version
		headerBufferEncrypted.put(Encryption.encryptSymmetric(headerBuffer.array(), Encryption.retreiveEncryptionKey()));
		
		return headerBufferEncrypted.array();
	}
	
	static FileEntryHeader parseData(byte[] dataAll) throws DatabaseFileException {
		if (dataAll[0] != (byte) 0x53 ||
		    dataAll[1] != (byte) 0x4D ||
		    dataAll[2] != (byte) 0x53
		   )
			throw new DatabaseFileException("Not an SMS history file");
		
		int version = 0 | (dataAll[3] & 0xFF);

		byte[] dataEncrypted = new byte[LENGTH_ENCRYPTED_HEADER_WITH_OVERHEAD];
		System.arraycopy(dataAll, LENGTH_PLAIN_HEADER, dataEncrypted, 0, LENGTH_ENCRYPTED_HEADER_WITH_OVERHEAD);
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		return new FileEntryHeader(version,
		                            Database.getInt(dataPlain, OFFSET_FREEINDEX), 
		                            Database.getInt(dataPlain, OFFSET_CONVINDEX)
		                            );
	}
}
