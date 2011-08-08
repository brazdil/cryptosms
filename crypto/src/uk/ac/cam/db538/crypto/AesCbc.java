package uk.ac.cam.db538.crypto;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AesCbc {
	private static final int AES_BLOCKSIZE = 16;
	
	/**
	 * Inserts data into an array of specified length. Puts random data behind to fill the rest.
	 * @param data
	 * @param length
	 * @return
	 */
	private static byte[] wrapData(byte[] data, int length, boolean putRandom) {
		ByteBuffer buffer = ByteBuffer.allocate(length);
		if (data.length >= length)
			buffer.put(data, 0, length);
		else {
			buffer.put(data);
			byte[] rand = new byte[length - data.length];
			if (putRandom) { 
				try {
					SecureRandom.getInstance("SHA1PRNG").nextBytes(rand);
				} catch (NoSuchAlgorithmException e) {
				}
			} else {
				for (int i = 0; i < rand.length; ++i)
					rand[i] = 0;
			}
			buffer.put(rand);
		}
		return buffer.array();
	}
	
	private static byte[] xor(byte[] original, byte[] added) {
		// assumes parameters are arrays of AES_BLOCKSIZE length !
		byte[] result = new byte[AES_BLOCKSIZE];
		for (int i = 0; i < AES_BLOCKSIZE; ++i)
			result[i] = (byte) (original[i] ^ added[i]);
		return result;
	}
	
	/**
	 * Encrypts data with AES/CBC encryption.
	 * @param data			Data to encrypt
	 * @param iv			Initialisation vector
	 * @param key			Encryption key
	 * @return
	 */
	public static byte[] encrypt(byte[] data, byte[] iv, byte[] key, boolean alignWithRandom, boolean storeLength) {
		// set up AES
		AesAlgorithm aesAlgorithm = new AesAlgorithm();
		aesAlgorithm.setKey(key);
		return encrypt(data, iv, aesAlgorithm, alignWithRandom, storeLength);
	}

	/**
	 * Encrypts data with AES/CBC encryption.
	 * @param data			Data to encrypt
	 * @param iv			Initialisation vector
	 * @param aesAlgorithm			AES class instance
	 * @return
	 */
	static byte[] encrypt(byte[] data, byte[] iv, AesAlgorithm aesAlgorithm, boolean alignWithRandom, boolean storeLength) {
		// align with random data to AES_BLOCKSIZE bytes
		int lengthCrap = (AES_BLOCKSIZE - data.length % AES_BLOCKSIZE) % AES_BLOCKSIZE; 
		if (lengthCrap != 0)
			data = wrapData(data, data.length + lengthCrap, alignWithRandom);

		byte[] result = new byte[(storeLength) ? data.length + 1 : data.length];
		byte[] buffer, buffer2;
		for (int i = 0; i < data.length / AES_BLOCKSIZE; ++i) {
			// get this block of data
			buffer = new byte[AES_BLOCKSIZE];
			System.arraycopy(data, AES_BLOCKSIZE * i, buffer, 0, AES_BLOCKSIZE);
			// apply IV
			buffer = xor(buffer, iv);
			// encrypt
			buffer2 = aesAlgorithm.encrypt(buffer);
			// copy to result
			System.arraycopy(buffer2, 0, result, AES_BLOCKSIZE * i, AES_BLOCKSIZE);
			// IV is now the previous result
			iv = buffer2;
		}
		
		if (storeLength)
			result[data.length] = (byte)lengthCrap;
		
		return result;
	}
	
	/**
	 * Decrypts data with AES/CBC algorithm
	 * @param data
	 * @param iv
	 * @param key
	 * @return
	 */
	public static byte[] decrypt(byte[] data, byte[] iv, byte[] key, boolean lengthStored) {
		// set up AES
		AesAlgorithm aesAlgorithm = new AesAlgorithm();
		aesAlgorithm.setKey(key);
		return decrypt(data, iv, aesAlgorithm, lengthStored);
	}

	/**
	 * Decrypts data with AES/CBC algorithm
	 * @param data
	 * @param iv
	 * @param aesAlgorithm
	 * @return
	 */
	static byte[] decrypt(byte[] data, byte[] iv, AesAlgorithm aesAlgorithm, boolean lengthStored) {
		int lengthCrap = (lengthStored) ? data[data.length - 1] : 0;
		int length = (lengthStored) ? data.length - lengthCrap - 1 : data.length;
		byte[] result = new byte[length];
		byte[] buffer, decrypted, xored;
		
		// decrypt with AES
		int blockCount = data.length / AES_BLOCKSIZE;
		for (int i = 0; i < blockCount; ++i) {
			buffer = new byte[AES_BLOCKSIZE];
			// get this block of data
			System.arraycopy(data, AES_BLOCKSIZE * i, buffer, 0, AES_BLOCKSIZE);
			// decrypt
			decrypted = aesAlgorithm.decrypt(buffer);
			// apply iv
			xored = xor(decrypted, iv);
			// copy to result
			if (i == blockCount - 1)
				System.arraycopy(xored, 0, result, AES_BLOCKSIZE * i, AES_BLOCKSIZE - lengthCrap);
			else
				System.arraycopy(xored, 0, result, AES_BLOCKSIZE * i, AES_BLOCKSIZE);
			// IV is now the original block
			iv = buffer;
		}
		return result;
	}
}
