package uk.ac.cam.db538.securesms;

import java.nio.ByteBuffer;
import java.security.*;

import uk.ac.cam.db538.securesms.data.LowLevel;

public class Encryption {
	public static final int IV_LENGTH = 16;
	public static final int MAC_LENGTH = 32;
	public static final int KEY_LENGTH = 32;
	public static final int ENCRYPTION_OVERHEAD = IV_LENGTH + MAC_LENGTH;
	
	public static class WrongKeyException extends Exception {
		private static final long serialVersionUID = 7462739153684558050L;
		
		public WrongKeyException() {
			super("Wrong key exception");
		}
	}

	private static final String HASHING_ALGORITHM = "SHA-256";
	
	private static SecureRandom mRandom = null;
	private static byte[] mEncryptionKey = null;

	public static byte[] generateRandomData(int length) {
		if (mRandom == null)
			try {
				mRandom = SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		byte[] data = new byte[length];
		mRandom.nextBytes(data);
		return data;
	}

	public static byte[] getHash(String text) {
		return getHash(text.getBytes());
	}
	
	public static byte[] getHash(byte[] data) {
		try {
			MessageDigest digester = MessageDigest.getInstance(HASHING_ALGORITHM);
			return digester.digest(data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] encryptSymmetric(byte[] data, byte[] key) {
		ByteBuffer result = ByteBuffer.allocate(data.length + ENCRYPTION_OVERHEAD);
		// MAC
		result.put(getHash(data));
		// IV
		for (int i = 0; i < IV_LENGTH; i++)
			result.put((byte) 0x49);
		// DATA
		result.put(data);
		
		return result.array();
	}
	
	public static byte[] decryptSymmetric(byte[] data, byte[] key) throws WrongKeyException {
		byte[] macSaved = LowLevel.cutData(data, 0, MAC_LENGTH);
		byte[] dataEncrypted = LowLevel.cutData(data, MAC_LENGTH, data.length - MAC_LENGTH);
		byte[] dataDecrypted = LowLevel.cutData(dataEncrypted, IV_LENGTH, dataEncrypted.length - IV_LENGTH);
		byte[] macReal = getHash(dataDecrypted);
		
		boolean isCorrect = true;
		for (int i = 0; i < MAC_LENGTH; ++i)
			isCorrect = isCorrect && macSaved[i] == macReal[i];
		if (isCorrect)
			return dataDecrypted;
		else
			throw new WrongKeyException();
	}
	
	public static void storeEncryptionKey(byte[] key) {
		mEncryptionKey = key;
	}

	public static byte[] retreiveEncryptionKey() {
		if (mEncryptionKey == null)
			mEncryptionKey = generateRandomData(KEY_LENGTH);
		return mEncryptionKey;
	}
}
