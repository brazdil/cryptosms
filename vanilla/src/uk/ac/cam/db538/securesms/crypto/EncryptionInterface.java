package uk.ac.cam.db538.securesms.crypto;

public interface EncryptionInterface {
	public static class WrongKeyException extends Exception {
		private static final long serialVersionUID = 7462739153684558050L;
		
		public WrongKeyException() {
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
		}
	}

	public byte[] generateRandomData(int length);
	public byte[] getHash(byte[] data);
	public int getEncryptedLength(int length);
	public int getAlignedLength(int length);
	public byte[] encryptSymmetricWithMasterKey(byte[] data) throws EncryptionException;
	public byte[] encryptSymmetric(byte[] data, byte[] key) throws EncryptionException;
	public byte[] decryptSymmetricWithMasterKey(byte[] data) throws EncryptionException;
	public byte[] decryptSymmetric(byte[] data, byte[] key) throws EncryptionException;
	public void generateMasterKey() throws EncryptionException;
	public byte[] getMasterKey() throws EncryptionException;
	public boolean testEncryption() throws EncryptionException;
}
