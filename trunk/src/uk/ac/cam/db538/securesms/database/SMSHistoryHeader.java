package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;

public class SMSHistoryHeader {
	static final int LENGTH_PLAIN_HEADER = 4;
	static final int LENGTH_ENCRYPTED_HEADER = SMSHistory.ENCRYPTED_ENTRY_SIZE - LENGTH_PLAIN_HEADER;

	private static final int OFFSET_CONVINDEX = LENGTH_ENCRYPTED_HEADER - 4;
	private static final int OFFSET_FREEINDEX = OFFSET_CONVINDEX - 4;

	private long mIndexFree;
	private long mIndexConversations;
	private int mVersion;
	
	SMSHistoryHeader(long indexFree, long indexConversations) {
		this(indexFree, indexConversations, 1);
	}
	
	SMSHistoryHeader(long indexFree, long indexConversations, int version) {
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

	static byte[] createData(SMSHistoryHeader header) {
		ByteBuffer headerBuffer = ByteBuffer.allocate(LENGTH_ENCRYPTED_HEADER);
		headerBuffer.put(Encryption.getRandomData(LENGTH_ENCRYPTED_HEADER - 8));
		headerBuffer.put(SMSHistory.getBytes(header.mIndexFree)); 
		headerBuffer.put(SMSHistory.getBytes(header.mIndexConversations));
		
		ByteBuffer headerBufferEncrypted = ByteBuffer.allocate(SMSHistory.CHUNK_SIZE);
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) 0x4D); // M
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) (header.mVersion & 0xFF)); // version
		headerBufferEncrypted.put(Encryption.encodeWithPassphrase(headerBuffer.array()));
		
		return headerBufferEncrypted.array();
	}
	
	static SMSHistoryHeader parseData(byte[] dataAll) throws HistoryFileException {
		if (dataAll[0] != (byte) 0x53 ||
		    dataAll[1] != (byte) 0x4D ||
		    dataAll[2] != (byte) 0x53
		   )
			throw new HistoryFileException("Not an SMS history file");
		
		int version = 0 | (dataAll[3] & 0xFF);

		byte[] dataEncrypted = new byte[LENGTH_ENCRYPTED_HEADER];
		System.arraycopy(dataAll, LENGTH_PLAIN_HEADER, dataEncrypted, 0, LENGTH_ENCRYPTED_HEADER);
		byte[] dataPlain = Encryption.decodeWithPassphrase(dataEncrypted);
		return new SMSHistoryHeader(SMSHistory.getInt(dataPlain, OFFSET_FREEINDEX), 
		                            SMSHistory.getInt(dataPlain, OFFSET_CONVINDEX),
		                            version
		                            );
	}
}
