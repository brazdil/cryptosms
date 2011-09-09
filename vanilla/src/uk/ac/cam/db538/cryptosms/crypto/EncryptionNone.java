package uk.ac.cam.db538.cryptosms.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public class EncryptionNone implements EncryptionInterface {
	private static final String HASHING_ALGORITHM = "SHA-256";
	
	public static void initEncryption() {
		Encryption.setEncryption(new EncryptionNone());
	}
	
	private SecureRandom mRandom = null;
	private MessageDigest mHashingFunction = null;
			
	public EncryptionNone() {
        try {
            mRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e){
            throw new RuntimeException("No secure random available!");
        }
	}
	
	@Override
	public byte[] decryptSymmetric(byte[] data, byte[] key, int blocks) throws EncryptionException {
		int length = blocks * Encryption.SYM_BLOCK_LENGTH;
		byte[] dataDecrypted = LowLevel.cutData(data, Encryption.SYM_OVERHEAD, length - Encryption.SYM_OVERHEAD);
		byte[] hashSaved = LowLevel.cutData(data, 0, Encryption.MAC_LENGTH);
		byte[] hashReal = getHash(dataDecrypted);
		
		for (int i = 0; i < Encryption.MAC_LENGTH; ++i)
			if (hashSaved[i] != hashReal[i])
				throw new EncryptionException(new Exception(LowLevel.toHex(dataDecrypted)));
		return dataDecrypted;
	}
	
	@Override
	public byte[] decryptSymmetric(byte[] data, byte[] key)
			throws EncryptionException {
		return decryptSymmetric(data, key, data.length / Encryption.SYM_BLOCK_LENGTH);
	}

	@Override
	public byte[] decryptSymmetricWithMasterKey(byte[] data)
			throws EncryptionException {
		return decryptSymmetricWithMasterKey(data, false);
	}

	@Override
	public byte[] encryptSymmetric(byte[] data, byte[] key) {
		int alignedLength = Encryption.getEncryption().getSymmetricAlignedLength(data.length);
		byte[] buffer = new byte[alignedLength + Encryption.MAC_LENGTH + Encryption.SYM_IV_LENGTH];
		data = LowLevel.wrapData(data, alignedLength);
		System.arraycopy(getHash(data), 0, buffer, 0, Encryption.MAC_LENGTH);
		for (int i = 0; i < Encryption.SYM_IV_LENGTH; ++i)
			buffer[Encryption.MAC_LENGTH + i] = (byte) 0x49;
		System.arraycopy(data, 0, buffer, Encryption.SYM_OVERHEAD, alignedLength);
		return buffer;
	}

	@Override
	public byte[] encryptSymmetricWithMasterKey(byte[] data)
			throws EncryptionException {
		return encryptSymmetricWithMasterKey(data, false);
	}

	@Override
	public byte[] generateRandomData(int length) {
		byte[] data = new byte[length];
		mRandom.nextBytes(data);
		return data;
	}

	@Override
	public int getSymmetricAlignedLength(int length) {
		return LowLevel.closestGreatestMultiple(length, Encryption.SYM_BLOCK_LENGTH);
	}

	@Override
	public int getSymmetricEncryptedLength(int length) {
		return getSymmetricAlignedLength(length) + Encryption.SYM_OVERHEAD;
	}

	@Override
	public byte[] getHash(byte[] data) {
		return getHashingFunction().digest(data);
	}

	@Override
	public byte[] encryptSymmetricWithMasterKey(byte[] data, boolean forceLogIn)
			throws EncryptionException {
		return encryptSymmetric(data, null);
	}

	@Override
	public byte[] decryptSymmetricWithMasterKey(byte[] data, boolean forceLogIn)
			throws EncryptionException {
		return decryptSymmetric(data, null);
	}

	@Override
	public byte[] sign(byte[] dataEncrypted) throws EncryptionException {
		return null;
	}

	@Override
	public boolean verify(byte[] data, byte[] signature, long contactId)
			throws EncryptionException {
		return false;
	}

	@Override
	public SecureRandom getRandom() {
		return mRandom;
	}

	@Override
	public MessageDigest getHashingFunction() {
		if (mHashingFunction == null) {
			try {
				mHashingFunction = MessageDigest.getInstance(HASHING_ALGORITHM);
			} catch (NoSuchAlgorithmException e) {
			}
		}
		return mHashingFunction;
	}
}
