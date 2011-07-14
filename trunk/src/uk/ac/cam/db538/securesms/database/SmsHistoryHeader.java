package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;

public class SmsHistoryHeader {
	static final int LENGTH_PLAIN_HEADER = 4;
	static final int LENGTH_ENCRYPTED_HEADER = SmsHistory.ENCRYPTED_ENTRY_SIZE - LENGTH_PLAIN_HEADER;

	private static final int OFFSET_CONVINDEX = LENGTH_ENCRYPTED_HEADER - 4;
	private static final int OFFSET_FREEINDEX = OFFSET_CONVINDEX - 4;

	private long mIndexFree;
	private long mIndexConversations;
	private int mVersion;
	
	SmsHistoryHeader(long indexFree, long indexConversations) {
		this(indexFree, indexConversations, 1);
	}
	
	SmsHistoryHeader(long indexFree, long indexConversations, int version) {
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

	static byte[] createData(SmsHistoryHeader header) {
		ByteBuffer headerBuffer = ByteBuffer.allocate(LENGTH_ENCRYPTED_HEADER);
		headerBuffer.put(Encryption.getRandomData(LENGTH_ENCRYPTED_HEADER - 8));
		headerBuffer.put(SmsHistory.getBytes(header.mIndexFree)); 
		headerBuffer.put(SmsHistory.getBytes(header.mIndexConversations));
		
		ByteBuffer headerBufferEncrypted = ByteBuffer.allocate(SmsHistory.CHUNK_SIZE);
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) 0x4D); // M
		headerBufferEncrypted.put((byte) 0x53); // S
		headerBufferEncrypted.put((byte) (header.mVersion & 0xFF)); // version
		headerBufferEncrypted.put(Encryption.encodeWithPassphrase(headerBuffer.array()));
		
		return headerBufferEncrypted.array();
	}
	
	static SmsHistoryHeader parseData(byte[] dataAll) throws HistoryFileException {
		if (dataAll[0] != (byte) 0x53 ||
		    dataAll[1] != (byte) 0x4D ||
		    dataAll[2] != (byte) 0x53
		   )
			throw new HistoryFileException("Not an SMS history file");
		
		int version = 0 | (dataAll[3] & 0xFF);

		byte[] dataEncrypted = new byte[LENGTH_ENCRYPTED_HEADER];
		System.arraycopy(dataAll, LENGTH_PLAIN_HEADER, dataEncrypted, 0, LENGTH_ENCRYPTED_HEADER);
		byte[] dataPlain = Encryption.decodeWithPassphrase(dataEncrypted);
		return new SmsHistoryHeader(SmsHistory.getInt(dataPlain, OFFSET_FREEINDEX), 
		                            SmsHistory.getInt(dataPlain, OFFSET_CONVINDEX),
		                            version
		                            );
	}
}
