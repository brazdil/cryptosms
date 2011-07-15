package uk.ac.cam.db538.securesms.database;

import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;

public class SmsHistory_Free {
	private static final int OFFSET_NEXTINDEX = SmsHistory.ENCRYPTED_ENTRY_SIZE - 4;

	private long mIndexNext;
	
	SmsHistory_Free(long indexNext) {
		if (indexNext > 0xFFFFFFFFL) 
			throw new IndexOutOfBoundsException();

		mIndexNext = indexNext;
	}

	static byte[] createData(SmsHistory_Free free) {
		ByteBuffer entryBuffer = ByteBuffer.allocate(SmsHistory.ENCRYPTED_ENTRY_SIZE);
		entryBuffer.put(Encryption.getRandomData(OFFSET_NEXTINDEX));
		entryBuffer.put(SmsHistory.getBytes(free.mIndexNext));
		return Encryption.encryptSymmetric(entryBuffer.array());
	}

	static SmsHistory_Free parseData(byte[] dataEncrypted) {
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted);
		return new SmsHistory_Free(SmsHistory.getInt(dataPlain, OFFSET_NEXTINDEX));
	}

	public long getIndexNext() {
		return mIndexNext;
	}

	public void setIndexNext(long indexNext) {
		this.mIndexNext = indexNext;
	}
}
