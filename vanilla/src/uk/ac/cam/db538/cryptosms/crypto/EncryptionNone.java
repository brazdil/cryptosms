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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.params.KeyParameter;

import uk.ac.cam.db538.cryptosms.utils.LowLevel;

/*
 * Class implementing the EncryptionInterface but not encrypting anything at all
 */
public class EncryptionNone implements EncryptionInterface {
	
	/**
	 * Inits the encryption.
	 */
	public static void initEncryption() {
		Encryption.setEncryption(new EncryptionNone());
	}
	
	private SecureRandom mRandom = null;
			
	/**
	 * Instantiates a new encryption none.
	 */
	public EncryptionNone() {
        try {
            mRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e){
            throw new RuntimeException("No secure random available!");
        }
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#decryptSymmetric(byte[], byte[], int)
	 */
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
	
	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#decryptSymmetric(byte[], byte[])
	 */
	@Override
	public byte[] decryptSymmetric(byte[] data, byte[] key)
			throws EncryptionException {
		return decryptSymmetric(data, key, data.length / Encryption.SYM_BLOCK_LENGTH);
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#decryptSymmetricWithMasterKey(byte[])
	 */
	@Override
	public byte[] decryptSymmetricWithMasterKey(byte[] data)
			throws EncryptionException {
		return decryptSymmetricWithMasterKey(data, false);
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#encryptSymmetric(byte[], byte[])
	 */
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

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#encryptSymmetricWithMasterKey(byte[])
	 */
	@Override
	public byte[] encryptSymmetricWithMasterKey(byte[] data)
			throws EncryptionException {
		return encryptSymmetricWithMasterKey(data, false);
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#generateRandomData(int)
	 */
	@Override
	public byte[] generateRandomData(int length) {
		byte[] data = new byte[length];
		mRandom.nextBytes(data);
		return data;
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#getSymmetricAlignedLength(int)
	 */
	@Override
	public int getSymmetricAlignedLength(int length) {
		return LowLevel.closestGreatestMultiple(length, Encryption.SYM_BLOCK_LENGTH);
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#getSymmetricEncryptedLength(int)
	 */
	@Override
	public int getSymmetricEncryptedLength(int length) {
		return getSymmetricAlignedLength(length) + Encryption.SYM_OVERHEAD;
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#getHash(byte[])
	 */
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

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#getHMAC(byte[], byte[])
	 */
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

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#encryptSymmetricWithMasterKey(byte[], boolean)
	 */
	@Override
	public byte[] encryptSymmetricWithMasterKey(byte[] data, boolean forceLogIn)
			throws EncryptionException {
		return encryptSymmetric(data, null);
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#decryptSymmetricWithMasterKey(byte[], boolean)
	 */
	@Override
	public byte[] decryptSymmetricWithMasterKey(byte[] data, boolean forceLogIn)
			throws EncryptionException {
		return decryptSymmetric(data, null);
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#sign(byte[])
	 */
	@Override
	public byte[] sign(byte[] dataEncrypted) throws EncryptionException {
		return null;
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#verify(byte[], byte[], long)
	 */
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
