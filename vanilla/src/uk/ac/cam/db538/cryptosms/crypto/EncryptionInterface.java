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

import java.security.SecureRandom;

/*
 * Interface for classes providing encryption
 */
public interface EncryptionInterface {
	public static class WrongKeyDecryptionException extends RuntimeException {
		private static final long serialVersionUID = 7462739153684558050L;
		
		/**
		 * Instantiates a new wrong key decryption exception.
		 */
		public WrongKeyDecryptionException() {
			super("Wrong key exception");
		}
	}
	
	public static class EncryptionException extends Exception {
		private static final long serialVersionUID = 2761165138191855888L;

		/**
		 * Instantiates a new encryption exception.
		 */
		public EncryptionException() {
			super("Encryption exception");
		}
		
		/**
		 * Instantiates a new encryption exception.
		 *
		 * @param e the e
		 */
		public EncryptionException(Exception e) {
			super("Encryption exception: " + e.getClass().getName() + " (" + e.getMessage() + ")");
			initCause(e);
		}
	}

	public SecureRandom getRandom();
	
	/**
	 * Generate random data.
	 *
	 * @param length the length
	 * @return the byte[]
	 */
	public byte[] generateRandomData(int length);
	
	/**
	 * Gets the hash.
	 *
	 * @param data the data
	 * @return the hash
	 */
	public byte[] getHash(byte[] data);
	
	/**
	 * Gets the hMAC.
	 *
	 * @param data the data
	 * @param key the key
	 * @return the hMAC
	 */
	public byte[] getHMAC(byte[] data, byte[] key);
	
	/**
	 * Gets the symmetric encrypted length.
	 *
	 * @param length the length
	 * @return the symmetric encrypted length
	 */
	public int getSymmetricEncryptedLength(int length);
	
	/**
	 * Gets the symmetric aligned length.
	 *
	 * @param length the length
	 * @return the symmetric aligned length
	 */
	public int getSymmetricAlignedLength(int length);
	
	/**
	 * Encrypt symmetric with master key.
	 *
	 * @param data the data
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	public byte[] encryptSymmetricWithMasterKey(byte[] data) throws EncryptionException;
	
	/**
	 * Encrypt symmetric with master key.
	 *
	 * @param data the data
	 * @param forceLogIn the force log in
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	public byte[] encryptSymmetricWithMasterKey(byte[] data, boolean forceLogIn) throws EncryptionException;
	
	/**
	 * Encrypt symmetric.
	 *
	 * @param data the data
	 * @param key the key
	 * @return the byte[]
	 */
	public byte[] encryptSymmetric(byte[] data, byte[] key);
	
	/**
	 * Decrypt symmetric with master key.
	 *
	 * @param data the data
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	public byte[] decryptSymmetricWithMasterKey(byte[] data) throws EncryptionException;
	
	/**
	 * Decrypt symmetric with master key.
	 *
	 * @param data the data
	 * @param forceLogIn the force log in
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	public byte[] decryptSymmetricWithMasterKey(byte[] data, boolean forceLogIn) throws EncryptionException;
	
	/**
	 * Decrypt symmetric.
	 *
	 * @param data the data
	 * @param key the key
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	public byte[] decryptSymmetric(byte[] data, byte[] key) throws EncryptionException;
	
	/**
	 * Decrypt symmetric.
	 *
	 * @param data the data
	 * @param key the key
	 * @param blocks the blocks
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	public byte[] decryptSymmetric(byte[] data, byte[] key, int blocks) throws EncryptionException;
	
	/**
	 * Sign.
	 *
	 * @param data the data
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	public byte[] sign(byte[] data) throws EncryptionException;
	
	/**
	 * Verify.
	 *
	 * @param data the data
	 * @param signature the signature
	 * @param contactId the contact id
	 * @return true, if successful
	 * @throws EncryptionException the encryption exception
	 */
	public boolean verify(byte[] data, byte[] signature, long contactId) throws EncryptionException;
}
