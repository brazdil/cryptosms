package uk.ac.cam.db538.cryptosms.crypto;

import java.security.MessageDigest;
import java.security.SecureRandom;

public interface EncryptionInterface {
	public static class WrongKeyDecryptionException extends RuntimeException {
		private static final long serialVersionUID = 7462739153684558050L;
		
		public WrongKeyDecryptionException() {
			super("Wrong key exception");
		}
	}
	
	public static class EncryptionException extends Exception {
		private static final long serialVersionUID = 2761165138191855888L;

		public EncryptionException() {
			super("Encryption exception");
		}
		
		public EncryptionException(Exception e) {
			super("Encryption exception: " + e.getClass().getName() + " (" + e.getMessage() + ")");
			initCause(e);
		}
	}

	public SecureRandom getRandom();
	public byte[] generateRandomData(int length);
	
	public byte[] getHash(byte[] data);
	public MessageDigest getHashingFunction();
	
	public int getSymmetricEncryptedLength(int length);
	public int getSymmetricAlignedLength(int length);
	public byte[] encryptSymmetricWithMasterKey(byte[] data) throws EncryptionException;
	public byte[] encryptSymmetricWithMasterKey(byte[] data, boolean forceLogIn) throws EncryptionException;
	public byte[] encryptSymmetric(byte[] data, byte[] key);
	public byte[] decryptSymmetricWithMasterKey(byte[] data) throws EncryptionException;
	public byte[] decryptSymmetricWithMasterKey(byte[] data, boolean forceLogIn) throws EncryptionException;
	public byte[] decryptSymmetric(byte[] data, byte[] key) throws EncryptionException;
	public byte[] decryptSymmetric(byte[] data, byte[] key, int blocks) throws EncryptionException;
	
	public byte[] sign(byte[] data) throws EncryptionException;
	public boolean verify(byte[] data, byte[] signature, long contactId) throws EncryptionException;
}
