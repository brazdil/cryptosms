package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;

public class SmsHistoryFree {
	private static final int OFFSET_NEXTINDEX = SmsHistory.ENCRYPTED_ENTRY_SIZE - 4;

	private long mIndexNext;
	
	SmsHistoryFree(long indexNext) {
		mIndexNext = indexNext;
	}

	static byte[] createData(SmsHistoryFree free) {
		ByteBuffer entryBuffer = ByteBuffer.allocate(SmsHistory.ENCRYPTED_ENTRY_SIZE);
		entryBuffer.put(Encryption.getRandomData(OFFSET_NEXTINDEX));
		entryBuffer.put(SmsHistory.getBytes(free.mIndexNext));
		return Encryption.encodeWithPassphrase(entryBuffer.array());
	}

	static SmsHistoryFree parseData(byte[] dataEncrypted) {
		byte[] dataPlain = Encryption.decodeWithPassphrase(dataEncrypted);
		return new SmsHistoryFree(SmsHistory.getInt(dataPlain, OFFSET_NEXTINDEX));
	}

	public long getIndexNext() {
		return mIndexNext;
	}

	public void setIndexNext(long indexNext) {
		this.mIndexNext = indexNext;
	}
}
