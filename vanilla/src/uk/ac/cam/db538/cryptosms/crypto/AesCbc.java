/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package uk.ac.cam.db538.cryptosms.crypto;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

/*
 * Class with static methods for AES/CBC encryption/decryption
 */
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
	
	private static AesAlgorithm mAes = null;
	
	/**
	 * Encrypts data with AES/CBC encryption.
	 *
	 * @param data 			Data to encrypt
	 * @param iv 			Initialization vector
	 * @param key 			Encryption key
	 * @param alignWithRandom If true, puts random data at the end to align to AES block size. Otherwise puts zeros.
	 * @param storeLength 	If true, stores the length of data used to align to AES block size as one extra byte at the end.
	 * @return the byte[]
	 */
	public static byte[] encrypt(byte[] data, byte[] iv, byte[] key, boolean alignWithRandom, boolean storeLength) {
		// PARAMETERS!!!
		ParametersWithIV paramKeyAndIv = new ParametersWithIV(new KeyParameter(key), iv);
		
		// set up AES
		if (mAes == null)
			mAes = new AesAlgorithm();
		mAes.setKey(key);

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
			buffer2 = mAes.encrypt(buffer);
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
	 * Decrypts data with AES/CBC algorithm.
	 *
	 * @param data 		Data to encrypt
	 * @param iv 		Initialization vector
	 * @param key 		Encryption key
	 * @param lengthStored Indicates whether the last byte holds the length of random data used to align to AES block size.
	 * @return the byte[]
	 */
	public static byte[] decrypt(byte[] data, byte[] iv, byte[] key, boolean lengthStored) {
		// set up AES
		if (mAes == null)
			mAes = new AesAlgorithm();
		mAes.setKey(key);

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
			decrypted = mAes.decrypt(buffer);
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
