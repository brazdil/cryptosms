package uk.ac.cam.db538.securesms.encryption;

import java.nio.ByteBuffer;
import java.security.*;
import java.util.Random;

public class Encryption {
	public static final int ENCRYPTION_OVERHEAD = 64;

	private static final String ENCRYPTION_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";
	private static final String HASHING_ALGORITHM = "SHA-256";
	
	private static Random mRandom = null;

	public static byte[] getHash(String password) {
		try {
			MessageDigest digester = MessageDigest.getInstance(HASHING_ALGORITHM);
			return digester.digest(password.getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] encodeWithPassphrase(byte[] data) {
		ByteBuffer result = ByteBuffer.allocate(data.length + ENCRYPTION_OVERHEAD);
		// IV
		for (int i = 0; i < ENCRYPTION_OVERHEAD / 4; i++)
			result.putShort((short) 0x4956);
		// DATA
		result.put(data);
		// MAC without 2 bytes
		for (int i = 0; i < ENCRYPTION_OVERHEAD / 4 - 1; i++)
			result.putShort((short) 0x7C5C);
		// carriage return
		result.putShort((short) 0x0D0A);
		
		return result.array();
	}
	
	public static byte[] decodeWithPassphrase(byte[] data) {
		ByteBuffer result = ByteBuffer.allocate(data.length - ENCRYPTION_OVERHEAD);
		result.put(data, ENCRYPTION_OVERHEAD / 2, data.length - ENCRYPTION_OVERHEAD);
		return result.array();
	}

	public static byte[] getRandomData(int length) {
		if (mRandom == null) mRandom = new Random();
		byte[] data = new byte[length];
		mRandom.nextBytes(data);
		return data;
	}
}