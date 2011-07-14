package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;

public class SMSHistoryFree {
	private static final int OFFSET_NEXTINDEX = SMSHistory.ENCRYPTED_ENTRY_SIZE - 4;

	private long mIndexNext;
	
	SMSHistoryFree(long indexNext) {
		mIndexNext = indexNext;
	}

	static byte[] createData(SMSHistoryFree free) {
		ByteBuffer entryBuffer = ByteBuffer.allocate(SMSHistory.ENCRYPTED_ENTRY_SIZE);
		entryBuffer.put(Encryption.getRandomData(OFFSET_NEXTINDEX));
		entryBuffer.put(SMSHistory.getBytes(free.mIndexNext));
		return Encryption.encodeWithPassphrase(entryBuffer.array());
	}

	static SMSHistoryFree parseData(byte[] dataEncrypted) {
		byte[] dataPlain = Encryption.decodeWithPassphrase(dataEncrypted);
		return new SMSHistoryFree(SMSHistory.getInt(dataPlain, OFFSET_NEXTINDEX));
	}

	public long getIndexNext() {
		return mIndexNext;
	}

	public void setIndexNext(long indexNext) {
		this.mIndexNext = indexNext;
	}
}
