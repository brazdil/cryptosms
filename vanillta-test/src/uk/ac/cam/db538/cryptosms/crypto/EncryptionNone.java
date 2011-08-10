package uk.ac.cam.db538.cryptosms.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public class EncryptionNone implements EncryptionInterface {
	private static final String HASHING_ALGORITHM = "SHA-256";

	public static void initEncryption() {
		Encryption.setEncryption(new EncryptionNone());
	}
	
	@Override
	public byte[] decryptSymmetric(byte[] data, byte[] key)
			throws EncryptionException {
		byte[] dataDecrypted = LowLevel.cutData(data, Encryption.ENCRYPTION_OVERHEAD, data.length - Encryption.ENCRYPTION_OVERHEAD);
		byte[] hashSaved = LowLevel.cutData(data, 0, Encryption.MAC_LENGTH);
		byte[] hashReal = getHash(dataDecrypted);
		for (int i = 0; i < Encryption.MAC_LENGTH; ++i)
			if (hashSaved[i] != hashReal[i])
				throw new EncryptionException();
		return dataDecrypted;
	}

	@Override
	public byte[] decryptSymmetricWithMasterKey(byte[] data)
			throws EncryptionException {
		return decryptSymmetric(data, getMasterKey());
	}

	@Override
	public byte[] encryptSymmetric(byte[] data, byte[] key)
			throws EncryptionException {
		byte[] buffer = new byte[data.length + Encryption.MAC_LENGTH + Encryption.IV_LENGTH];
		System.arraycopy(getHash(data), 0, buffer, 0, Encryption.MAC_LENGTH);
		for (int i = 0; i < Encryption.IV_LENGTH; ++i)
			buffer[Encryption.MAC_LENGTH + i] = (byte) 0x49;
		System.arraycopy(data, 0, buffer, Encryption.ENCRYPTION_OVERHEAD, data.length);
		return buffer;
	}

	@Override
	public byte[] encryptSymmetricWithMasterKey(byte[] data)
			throws EncryptionException {
		return encryptSymmetric(data, getMasterKey());
	}

	private static byte[] mMasterKey = null;
	
	@Override
	public void generateMasterKey() throws EncryptionException {
		if (mMasterKey == null)
			mMasterKey = generateRandomData(Encryption.KEY_LENGTH);
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
		return length + (Encryption.AES_BLOCK_LENGTH - (length % Encryption.AES_BLOCK_LENGTH)) % Encryption.AES_BLOCK_LENGTH;
	}

	@Override
	public int getEncryptedLength(int length) {
		return getAlignedLength(length) + Encryption.ENCRYPTION_OVERHEAD;
	}

	@Override
	public byte[] getHash(byte[] data) {
		try {
			MessageDigest digester = MessageDigest.getInstance(HASHING_ALGORITHM);
			return digester.digest(data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public byte[] getMasterKey() throws EncryptionException {
		if (mMasterKey == null)
			generateMasterKey();
		return mMasterKey;
	}

	@Override
	public boolean testEncryption() throws EncryptionException {
		return true;
	}

}
