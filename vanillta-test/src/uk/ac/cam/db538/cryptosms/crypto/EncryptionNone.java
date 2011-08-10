package uk.ac.cam.db538.cryptosms.crypto;

import java.util.Random;

public class EncryptionNone implements EncryptionInterface {

	public static void initEncryption() {
		Encryption.setEncryption(new EncryptionNone());
	}
	
	@Override
	public byte[] decryptSymmetric(byte[] data, byte[] key)
			throws EncryptionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] decryptSymmetricWithMasterKey(byte[] data)
			throws EncryptionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] encryptSymmetric(byte[] data, byte[] key)
			throws EncryptionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] encryptSymmetricWithMasterKey(byte[] data)
			throws EncryptionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void generateMasterKey() throws EncryptionException {
		// TODO Auto-generated method stub

	}
	
	private static Random mRandom = null;

	@Override
	public byte[] generateRandomData(int length) {
		if (mRandom == null)
			mRandom = new Random();
		byte[] buffer = new byte[length];
		mRandom.nextBytes(buffer);
		return buffer;
	}

	@Override
	public int getAlignedLength(int length) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getEncryptedLength(int length) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte[] getHash(byte[] data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] getMasterKey() throws EncryptionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean testEncryption() throws EncryptionException {
		// TODO Auto-generated method stub
		return false;
	}

}
