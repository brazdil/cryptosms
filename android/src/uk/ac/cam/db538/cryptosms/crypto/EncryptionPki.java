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

import uk.ac.cam.db538.cryptosms.crypto.AesCbc;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.Pki.PkiNotReadyException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.BadInputException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.DeclinedException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.NotConnectedException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.PKIErrorException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.TimeoutException;

/*
 * Class handling encryption through PKI
 */
public final class EncryptionPki implements EncryptionInterface {
	private EncryptionNone mEncryptionNone = null;
	
	/**
	 * Instantiates a new encryption pki.
	 */
	public EncryptionPki() {
		mEncryptionNone = new EncryptionNone();
	}
	
	// METHODS 
	
	/**
	 * Returns byte array with random data.
	 *
	 * @param length the length
	 * @return the byte[]
	 */
	@Override
	public byte[] generateRandomData(int length) {
		return mEncryptionNone.generateRandomData(length);
	}

	/**
	 * Returns SHA-512 hash of given data.
	 *
	 * @param data the data
	 * @return the hash
	 */
	@Override
	public byte[] getHash(byte[] data) {
		return mEncryptionNone.getHash(data);
	}
	
	/**
	 * Returns the length of data after encryption.
	 * Encryption adds some overhead (IV and MAC) and the data is also aligned to 16-byte blocks with random stuff
	 *
	 * @param length the length
	 * @return the symmetric encrypted length
	 */
	@Override
	public int getSymmetricEncryptedLength(int length) {
		return mEncryptionNone.getSymmetricEncryptedLength(length);
	}
	
	/**
	 * Returns the least multiple of AES_BLOCKSIZE greater than the argument.
	 *
	 * @param length the length
	 * @return the symmetric aligned length
	 */
	@Override
	public int getSymmetricAlignedLength(int length) {
		return mEncryptionNone.getSymmetricAlignedLength(length);
	}

	/**
	 * Encrypts data with Master Key stored with PKI.
	 *
	 * @param data the data
	 * @param forceLogIn the force log in
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	@Override
	public byte[] encryptSymmetricWithMasterKey(byte[] data, boolean forceLogIn) throws EncryptionException {
		try {
			return encryptSymmetric(data, Pki.getMasterKey(forceLogIn));
		} catch (PkiNotReadyException e) {
			throw new EncryptionException(e);
		}
	}

	/**
	 * Encrypts data with Master Key stored with PKI.
	 *
	 * @param data the data
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	@Override
	public byte[] encryptSymmetricWithMasterKey(byte[] data) throws EncryptionException {
		return encryptSymmetricWithMasterKey(data, false);
	}
	
	/**
	 * Encrypts data with given key.
	 *
	 * @param data the data
	 * @param key the key
	 * @return the byte[]
	 */
	@Override
	public byte[] encryptSymmetric(byte[] data, byte[] key) {
		// align data for MAC checking
		data = LowLevel.wrapData(data, getSymmetricAlignedLength(data.length));
		// generate everything
		byte[] iv = generateRandomData(Encryption.SYM_IV_LENGTH);
		byte[] mac = getHash(data);
		// encrypt
		byte[] dataEncrypted = AesCbc.encrypt(data, iv, key, true, false);
		
		// save everything
		byte[] result = new byte[dataEncrypted.length + Encryption.SYM_OVERHEAD];
		// MAC
		System.arraycopy(mac, 0, result, 0, Encryption.HMAC_LENGTH);
		// IV 
		System.arraycopy(iv, 0, result, Encryption.HMAC_LENGTH, Encryption.SYM_IV_LENGTH);
		//data
		System.arraycopy(dataEncrypted, 0, result, Encryption.SYM_OVERHEAD, dataEncrypted.length);
		
		return result;
	}
	
	/**
	 * Decrypts data with Master Key stored with PKI.
	 *
	 * @param data the data
	 * @param forceLogIn the force log in
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	@Override
	public byte[] decryptSymmetricWithMasterKey(byte[] data, boolean forceLogIn) throws EncryptionException {
		try {
			return decryptSymmetric(data, Pki.getMasterKey(forceLogIn));
		} catch (PkiNotReadyException e) {
			throw new EncryptionException(e);
		}
	}

	/**
	 * Decrypts data with Master Key stored with PKI.
	 *
	 * @param data the data
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	@Override
	public byte[] decryptSymmetricWithMasterKey(byte[] data) throws EncryptionException {
		return decryptSymmetricWithMasterKey(data, false);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#decryptSymmetric(byte[], byte[], int)
	 */
	@Override
	public byte[] decryptSymmetric(byte[] data, byte[] key, int blocks)
			throws EncryptionException {
		int length = blocks * Encryption.SYM_BLOCK_LENGTH;
		// cut the file up
		byte[] macSaved = LowLevel.cutData(data, 0, Encryption.HMAC_LENGTH);
		byte[] iv = LowLevel.cutData(data, Encryption.HMAC_LENGTH, Encryption.SYM_IV_LENGTH);
		byte[] dataEncrypted = LowLevel.cutData(data, Encryption.SYM_OVERHEAD, length - Encryption.SYM_OVERHEAD);
		
		// decrypt
		byte[] dataDecrypted = AesCbc.decrypt(dataEncrypted, iv, key, false);
		// generate new MAC
		byte[] macReal = getHash(dataDecrypted);
		
		// compare MACs
		if (compareMACs(macSaved, macReal))
			return dataDecrypted;
		else
			throw new WrongKeyDecryptionException();
	}

	/**
	 * Decrypts data with given key.
	 *
	 * @param data the data
	 * @param key the key
	 * @return the byte[]
	 * @throws EncryptionException the encryption exception
	 */
	@Override
	public byte[] decryptSymmetric(byte[] data, byte[] key) throws EncryptionException {
		return decryptSymmetric(data, key, data.length / Encryption.SYM_BLOCK_LENGTH);
	}
	
	private boolean compareMACs(byte[] saved, byte[] actual) {
		// compare MACs
		boolean isCorrect = true;
		for (int i = 0; i < Encryption.HMAC_LENGTH; ++i)
			isCorrect = isCorrect && saved[i] == actual[i];
		return isCorrect;
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#sign(byte[])
	 */
	@Override
	public byte[] sign(byte[] data) throws EncryptionException {
		try {
			byte[] signature = Pki.getPkiWrapper().sign(data);
			if (signature.length != Encryption.ASYM_SIGNATURE_LENGTH)
				throw new EncryptionException();
			return signature;
		} catch (TimeoutException e) {
			throw new EncryptionException(e);
		} catch (PKIErrorException e) {
			throw new EncryptionException(e);
		} catch (DeclinedException e) {
			throw new EncryptionException(e);
		} catch (NotConnectedException e) {
			throw new EncryptionException(e);
		} catch (BadInputException e) {
			throw new EncryptionException(e);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#verify(byte[], byte[], long)
	 */
	@Override
	public boolean verify(byte[] data, byte[] signature, long contactId)
			throws EncryptionException {
		try {
			return Pki.getPkiWrapper().verify(signature, data, contactId);
		} catch (TimeoutException e) {
			throw new EncryptionException(e);
		} catch (PKIErrorException e) {
			throw new EncryptionException(e);
		} catch (DeclinedException e) {
			throw new EncryptionException(e);
		} catch (NotConnectedException e) {
			throw new EncryptionException(e);
		} catch (BadInputException e) {
			throw new EncryptionException(e);
		}
	}

	@Override
	public SecureRandom getRandom() {
		return mEncryptionNone.getRandom();
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface#getHMAC(byte[], byte[])
	 */
	@Override
	public byte[] getHMAC(byte[] data, byte[] key) {
		return mEncryptionNone.getHMAC(data, key);
	}
}
