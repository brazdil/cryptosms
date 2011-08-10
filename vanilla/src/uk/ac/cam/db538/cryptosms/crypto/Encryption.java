package uk.ac.cam.db538.cryptosms.crypto;

public class Encryption {
	public static final int IV_LENGTH = 16;
	public static final int AES_BLOCK_LENGTH = 16;
	public static final int MAC_LENGTH = 32;
	public static final int KEY_LENGTH = 32;
	public static final int ENCRYPTION_OVERHEAD = IV_LENGTH + MAC_LENGTH;

	private static EncryptionInterface mEncryption = null;
	
	public static EncryptionInterface getEncryption() {
		return mEncryption;
	}
	
	public static void setEncryption(EncryptionInterface crypto) {
		mEncryption = crypto;
	}
}
