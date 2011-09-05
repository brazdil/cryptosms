package uk.ac.cam.db538.cryptosms.crypto;

import java.security.SecureRandom;

import uk.ac.cam.db538.crypto.AesCbc;
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
		System.arraycopy(mac, 0, result, 0, Encryption.MAC_LENGTH);
		// IV 
		System.arraycopy(iv, 0, result, Encryption.MAC_LENGTH, Encryption.SYM_IV_LENGTH);
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
	
	/**
	 * Decrypts data with given key
	 * @param data
	 * @return
	 * @throws EncryptionException
	 */
	@Override
	public byte[] decryptSymmetric(byte[] data, byte[] key) throws EncryptionException {
		// cut the file up
		byte[] macSaved = LowLevel.cutData(data, 0, Encryption.MAC_LENGTH);
		byte[] iv = LowLevel.cutData(data, Encryption.MAC_LENGTH, Encryption.SYM_IV_LENGTH);
		byte[] dataEncrypted = LowLevel.cutData(data, Encryption.SYM_OVERHEAD, data.length - Encryption.SYM_OVERHEAD);
		
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
	
	private boolean compareMACs(byte[] saved, byte[] actual) {
		// compare MACs
		boolean isCorrect = true;
		for (int i = 0; i < Encryption.MAC_LENGTH; ++i)
			isCorrect = isCorrect && saved[i] == actual[i];
		return isCorrect;
	}

	@Override
	public byte[] sign(byte[] data) throws EncryptionException {
		try {
			byte[] signature = Pki.getPkiWrapper().sign(data);
			if (signature.length != Encryption.ASYM_SIGNATURE_LENGTH)
				throw new EncryptionException();
				
			byte[] signedData = new byte[data.length + Encryption.ASYM_SIGNATURE_LENGTH];
			System.arraycopy(data, 0, signedData, 0, data.length);
			System.arraycopy(signature, 0, signedData, data.length, Encryption.ASYM_SIGNATURE_LENGTH);
			
			return signedData;
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
	public byte[] verify(byte[] signedData, long contactId)
			throws EncryptionException {
		int unsignedLength = signedData.length - Encryption.ASYM_SIGNATURE_LENGTH;
		byte[] unsignedData = LowLevel.cutData(signedData, 0, unsignedLength);
		byte[] signature = LowLevel.cutData(signedData, unsignedLength, Encryption.ASYM_SIGNATURE_LENGTH);
		
		boolean verified = false;
		try {
			verified = Pki.getPkiWrapper().verify(signature, unsignedData, contactId);
		} catch (TimeoutException e) {
			new EncryptionException(e);
		} catch (PKIErrorException e) {
			new EncryptionException(e);
		} catch (DeclinedException e) {
			new EncryptionException(e);
		} catch (NotConnectedException e) {
			new EncryptionException(e);
		} catch (BadInputException e) {
			new EncryptionException(e);
		}
		
		if (verified)
			return unsignedData;
		else
			throw new WrongKeyDecryptionException();
	}

	@Override
	public SecureRandom getRandom() {
		return mEncryptionNone.getRandom();
	}
}
