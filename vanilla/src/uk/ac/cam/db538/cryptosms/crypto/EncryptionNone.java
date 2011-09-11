package uk.ac.cam.db538.cryptosms.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.params.KeyParameter;

import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public class EncryptionNone implements EncryptionInterface {
	public static void initEncryption() {
		Encryption.setEncryption(new EncryptionNone());
	}
	
	private SecureRandom mRandom = null;
			
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
		byte[] hashSaved = LowLevel.cutData(data, 0, Encryption.HMAC_LENGTH);
		byte[] hashReal = getHMAC(dataDecrypted, key);
		
		for (int i = 0; i < Encryption.HMAC_LENGTH; ++i)
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
		byte[] buffer = new byte[alignedLength + Encryption.HMAC_LENGTH + Encryption.SYM_IV_LENGTH];
		data = LowLevel.wrapData(data, alignedLength);
		System.arraycopy(getHMAC(data, key), 0, buffer, 0, Encryption.HMAC_LENGTH);
		for (int i = 0; i < Encryption.SYM_IV_LENGTH; ++i)
			buffer[Encryption.HMAC_LENGTH + i] = (byte) 0x49;
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
		SHA256Digest sha256 = new SHA256Digest();
		byte[] result = new byte[Encryption.HASH_LENGTH];
		sha256.update(data, 0, data.length);
		if (sha256.doFinal(result, 0) == Encryption.HASH_LENGTH)
			return result;
		else
			throw new RuntimeException("SHA-256 internal error");
	}

	@Override
	public byte[] getHMAC(byte[] data, byte[] key) {
		HMac mac = new HMac(new SHA256Digest());
		mac.init(new KeyParameter(key));
		byte[] result = new byte[Encryption.HMAC_LENGTH];
		mac.update(data, 0, data.length);
		if (mac.doFinal(result, 0) == Encryption.HMAC_LENGTH)
			return result;
		else
			throw new RuntimeException("HMAC internal error");
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
}
