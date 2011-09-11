package uk.ac.cam.db538.cryptosms.crypto;

import java.security.MessageDigest;
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

public final class EncryptionPki implements EncryptionInterface {
	private EncryptionNone mEncryptionNone = null;
	
	public EncryptionPki() {
		mEncryptionNone = new EncryptionNone();
	}
	
	// METHODS 
	
	/**
	 * Returns byte array with random data
	 * @param length
	 * @return
	 */
	@Override
	public byte[] generateRandomData(int length) {
		return mEncryptionNone.generateRandomData(length);
	}

	/**
	 * Returns SHA-512 hash of given data 
	 * @param data
	 * @return
	 */
	@Override
	public byte[] getHash(byte[] data) {
		return mEncryptionNone.getHash(data);
	}
	
	/**
	 * Returns the length of data after encryption.
	 * Encryption adds some overhead (IV and MAC) and the data is also aligned to 16-byte blocks with random stuff
	 * @param length
	 * @return
	 */
	@Override
	public int getSymmetricEncryptedLength(int length) {
		return mEncryptionNone.getSymmetricEncryptedLength(length);
	}
	
	/**
	 * Returns the least multiple of AES_BLOCKSIZE greater than the argument
	 * @param length
	 * @return
	 */
	@Override
	public int getSymmetricAlignedLength(int length) {
		return mEncryptionNone.getSymmetricAlignedLength(length);
	}

	/**
	 * Encrypts data with Master Key stored with PKI
	 * @param data
	 * @param forceLogIn
	 * @return
	 * @throws EncryptionException
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
	 * Encrypts data with Master Key stored with PKI
	 * @param data
	 * @return
	 * @throws EncryptionException
	 */
	@Override
	public byte[] encryptSymmetricWithMasterKey(byte[] data) throws EncryptionException {
		return encryptSymmetricWithMasterKey(data, false);
	}
	
	/**
	 * Encrypts data with given key
	 * @param data
	 * @return
	 * @throws EncryptionException
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
	 * Decrypts data with Master Key stored with PKI
	 * @param data
	 * @return
	 * @throws EncryptionException
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
	 * Decrypts data with Master Key stored with PKI
	 * @param data
	 * @return
	 * @throws EncryptionException
	 */
	@Override
	public byte[] decryptSymmetricWithMasterKey(byte[] data) throws EncryptionException {
		return decryptSymmetricWithMasterKey(data, false);
	}
	
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
	 * Decrypts data with given key
	 * @param data
	 * @return
	 * @throws EncryptionException
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

	@Override
	public byte[] getHMAC(byte[] data, byte[] key) {
		return mEncryptionNone.getHMAC(data, key);
	}
}
