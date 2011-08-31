package uk.ac.cam.db538.cryptosms.crypto;

public class Encryption {
	public static final int MAC_LENGTH = 32;

	public static final int SYM_IV_LENGTH = 16;
	public static final int SYM_BLOCK_LENGTH = 16;
	public static final int SYM_KEY_LENGTH = 32;
	public static final int SYM_OVERHEAD = SYM_IV_LENGTH + MAC_LENGTH;
	public static final int SYM_CONFIRM_NONCE_LENGTH = Encryption.SYM_BLOCK_LENGTH;
	
	public static final int ASYM_KEY_LENGTH = 384;
	public static final int ASYM_BLOCK_LENGTH = ASYM_KEY_LENGTH;
	public static final int ASYM_SIGNATURE_LENGTH = ASYM_KEY_LENGTH;
	public static final int ASYM_OVERHEAD = MAC_LENGTH;
	
	public static final int DH_MIN_MODULUS_BITLENGTH = 3076;
	public static final int DH_MIN_KEYMAX_BITLENGTH = 256;

	private static EncryptionInterface mEncryption = null;
	
	public static EncryptionInterface getEncryption() {
		return mEncryption;
	}
	
	public static void setEncryption(EncryptionInterface crypto) {
		mEncryption = crypto;
	}
}
