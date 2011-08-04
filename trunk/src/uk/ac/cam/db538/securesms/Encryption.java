package uk.ac.cam.db538.securesms;

import java.nio.ByteBuffer;
import java.security.*;

public class Encryption {
	public static final int KEY_LENGTH = 32;
	public static final int ENCRYPTION_OVERHEAD = 2 * KEY_LENGTH;

	//private static final String ENCRYPTION_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";
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
		// IV
		for (int i = 0; i < ENCRYPTION_OVERHEAD / 4; i++)
			result.putShort((short) 0x4956);
		// MAC
		result.put(getHash(data));
		// DATA
		result.put(data);
		
		return result.array();
	}
	
	public static byte[] decryptSymmetric(byte[] data, byte[] key) {
		ByteBuffer result = ByteBuffer.allocate(data.length - ENCRYPTION_OVERHEAD);
		result.put(data, ENCRYPTION_OVERHEAD, data.length - ENCRYPTION_OVERHEAD);
		return result.array();
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
