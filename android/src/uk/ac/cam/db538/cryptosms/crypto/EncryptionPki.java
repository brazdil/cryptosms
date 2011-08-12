package uk.ac.cam.db538.cryptosms.crypto;

import java.nio.ByteBuffer;
import java.security.*;

import android.util.Log;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.utils.Charset;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.DeclinedException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.NotConnectedException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.PKIErrorException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.TimeoutException;

public final class EncryptionPki implements EncryptionInterface {
	private static final String KEY_STORAGE = "SECURESMS_MASTER_KEY";
	private static final String HASHING_ALGORITHM = "SHA-256";
	
	private SecureRandom mRandom = null;

	public EncryptionPki() {
	}
	
	// METHODS 
	
	/**
	 * Returns byte array with random data
	 * @param length
	 * @return
	 */
	public byte[] generateRandomData(int length) {
		if (mRandom == null)
			try {
				mRandom = SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		byte[] data = new byte[length];
		mRandom.nextBytes(data);
		return data;
	}

	/**
	 * Returns SHA-512 hash of given data 
	 * @param data
	 * @return
	 */
	public byte[] getHash(byte[] data) {
		try {
			MessageDigest digester = MessageDigest.getInstance(HASHING_ALGORITHM);
			return digester.digest(data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns the length of data after encryption.
	 * Encryption adds some overhead (IV and MAC) and the data is also aligned to 16-byte blocks with random stuff
	 * @param length
	 * @return
	 */
	public int getEncryptedLength(int length) {
		return getAlignedLength(length) + Encryption.ENCRYPTION_OVERHEAD;
	}
	
	/**
	 * Returns the least multiple of AES_BLOCKSIZE greater than the argument
	 * @param length
	 * @return
	 */
	public int getAlignedLength(int length) {
		return length + (Encryption.AES_BLOCK_LENGTH - (length % Encryption.AES_BLOCK_LENGTH)) % Encryption.AES_BLOCK_LENGTH;
	}

	/**
	 * Encrypts data with Master Key stored with PKI
	 * @param data
	 * @return
	 * @throws EncryptionException
	 */
	public byte[] encryptSymmetricWithMasterKey(byte[] data) throws EncryptionException {
		generateMasterKey();

		ByteBuffer result = ByteBuffer.allocate(data.length + Encryption.ENCRYPTION_OVERHEAD);
		// MAC
		result.put(getHash(data));
		// Encryption through PKI (prepends IV)
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = MyApplication.getSingleton().getPki().encryptSymmetric(data, KEY_STORAGE, generateRandomData(Encryption.IV_LENGTH), true);
		} catch (TimeoutException e1) {
			throw new EncryptionException(e1);
		} catch (PKIErrorException e1) {
			throw new EncryptionException(e1);
		} catch (DeclinedException e1) {
			throw new EncryptionException(e1);
		} catch (NotConnectedException e1) {
			throw new EncryptionException(e1);
		}
		result.put(dataEncrypted);
		
		return result.array();
	}
	
	/**
	 * Encrypts data with given key
	 * @param data
	 * @return
	 * @throws EncryptionException
	 */
	public byte[] encryptSymmetric(byte[] data, byte[] key) throws EncryptionException {
		// Encryption through PKI (prepends IV)
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = MyApplication.getSingleton().getPki().encryptSymmetric(data, key, generateRandomData(Encryption.IV_LENGTH), true);
		} catch (TimeoutException e1) {
			throw new EncryptionException(e1);
		} catch (PKIErrorException e1) {
			throw new EncryptionException(e1);
		} catch (DeclinedException e1) {
			throw new EncryptionException(e1);
		} catch (NotConnectedException e1) {
			throw new EncryptionException(e1);
		}

		ByteBuffer result = ByteBuffer.allocate(dataEncrypted.length + Encryption.MAC_LENGTH);
		// MAC
		result.put(getHash(data));
		// IV and data
		result.put(dataEncrypted);
		
		return result.array();
	}
	
	/**
	 * Decrypts data with Master Key stored with PKI
	 * @param data
	 * @return
	 * @throws EncryptionException
	 */
	public byte[] decryptSymmetricWithMasterKey(byte[] data) throws EncryptionException {
		generateMasterKey();

		byte[] macSaved = LowLevel.cutData(data, 0, Encryption.MAC_LENGTH);
		byte[] dataEncrypted = LowLevel.cutData(data, Encryption.MAC_LENGTH, data.length - Encryption.MAC_LENGTH);
		
		// Decryption through PKI (removes IV)
		byte[] dataDecrypted = null;
		try {
			dataDecrypted = MyApplication.getSingleton().getPki().decryptSymmetric(dataEncrypted, KEY_STORAGE, true, true);
		} catch (TimeoutException e1) {
			throw new EncryptionException(e1);
		} catch (PKIErrorException e1) {
			throw new EncryptionException(e1);
		} catch (DeclinedException e1) {
			throw new EncryptionException(e1);
		} catch (NotConnectedException e1) {
			throw new EncryptionException(e1);
		}
		byte[] macReal = getHash(dataDecrypted);
		
		boolean isCorrect = true;
		for (int i = 0; i < Encryption.MAC_LENGTH; ++i)
			isCorrect = isCorrect && macSaved[i] == macReal[i];
		if (isCorrect)
			return dataDecrypted;
		else
			throw new EncryptionException(new WrongKeyException());
	}
	
	/**
	 * Decrypts data with given key
	 * @param data
	 * @return
	 * @throws EncryptionException
	 */
	public byte[] decryptSymmetric(byte[] data, byte[] key) throws EncryptionException {
		byte[] macSaved = LowLevel.cutData(data, 0, Encryption.MAC_LENGTH);
		byte[] dataEncrypted = LowLevel.cutData(data, Encryption.MAC_LENGTH, data.length - Encryption.MAC_LENGTH);
		
		// Decryption through PKI (removes IV)
		byte[] dataDecrypted = null;
		try {
			dataDecrypted = MyApplication.getSingleton().getPki().decryptSymmetric(dataEncrypted, key, true, true);
		} catch (TimeoutException e1) {
			throw new EncryptionException(e1);
		} catch (PKIErrorException e1) {
			throw new EncryptionException(e1);
		} catch (DeclinedException e1) {
			throw new EncryptionException(e1);
		} catch (NotConnectedException e1) {
			throw new EncryptionException(e1);
		}
		byte[] macReal = getHash(dataDecrypted);
		
		boolean isCorrect = true;
		for (int i = 0; i < Encryption.MAC_LENGTH; ++i)
			isCorrect = isCorrect && macSaved[i] == macReal[i];
		if (isCorrect)
			return dataDecrypted;
		else
			throw new EncryptionException(new WrongKeyException());
	}

	/**
	 * Checks whether a Master Key is already stored with PKI and if not, generates a new one and stores it there
	 * @throws EncryptionException
	 */
	public void generateMasterKey() throws EncryptionException {
		try {
			PKIwrapper pki = MyApplication.getSingleton().getPki();
			if (!pki.hasDataStore(KEY_STORAGE))
				pki.setDataStore(KEY_STORAGE, generateRandomData(Encryption.KEY_LENGTH));
		} catch (NotConnectedException e) {
			throw new EncryptionException(e);
		} catch (TimeoutException e) {
			throw new EncryptionException(e);
		} catch (DeclinedException e) {
			throw new EncryptionException(e);
		} catch (PKIErrorException e) {
			throw new EncryptionException(e);
		}
	}
	
	/**
	 * Checks whether a Master Key is already stored with PKI and if not, generates a new one and stores it there
	 * @throws EncryptionException
	 */
	public byte[] getMasterKey() throws EncryptionException {
		try {
			PKIwrapper pki = MyApplication.getSingleton().getPki();
			if (pki.hasDataStore(KEY_STORAGE))
				return pki.getDataStore(KEY_STORAGE);
			else
				return new byte[0];
		} catch (NotConnectedException e) {
			throw new EncryptionException(e);
		} catch (TimeoutException e) {
			throw new EncryptionException(e);
		} catch (DeclinedException e) {
			throw new EncryptionException(e);
		} catch (PKIErrorException e) {
			throw new EncryptionException(e);
		}
	}

	public boolean testEncryption() throws EncryptionException {
		PKIwrapper PKI = MyApplication.getSingleton().getPki();
		
		Log.d(MyApplication.APP_TAG, "ENCRYPTION TEST");
		byte[] KEY_256 = LowLevel.fromHex("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4");
		byte[] DATA = Charset.toAscii8("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc euismod malesuada urna at cursus. Morbi magna felis, mattis id.AAA");
		byte[] IV = LowLevel.fromHex("39F23369A9D9BACFA530E26304231461");
		byte[] EXPECTED = LowLevel.fromHex("39F23369A9D9BACFA530E26304231461B15AF6E11EAC0584EB38528E27E17490298115F39D41DE251F9F35726BE1BE47E5D0CFBFFCB4B6B98EC2CBF82AC82B68A83F0B595E6A7E54CB31C7399AA23E941850909B4FA33438B403AEA03DF9F309395A4D4C329FF0F06F7DF048D871D305F507D084D69DAF680D3A4826397FE4934032028957B1988C4A7E645F37B998A6");
		byte[] RESULT_ENC = new byte[0];
		byte[] RESULT_DEC = new byte[0];
		
		Log.d(MyApplication.APP_TAG, "Waiting...");
		try {
			RESULT_ENC = PKI.encryptSymmetric(DATA, KEY_256, IV, true);
			RESULT_DEC = PKI.decryptSymmetric(EXPECTED, KEY_256, true, true);
		} catch (NotConnectedException e) {
			throw new EncryptionException(e);
		} catch (TimeoutException e) {
			throw new EncryptionException(e);
		} catch (DeclinedException e) {
			throw new EncryptionException(e);
		} catch (PKIErrorException e) {
			throw new EncryptionException(e);
		}
		
		Log.d(MyApplication.APP_TAG, "Checking...");
		boolean areSame = true;
		if (RESULT_ENC.length == EXPECTED.length) {
			for (int i = 0; i < RESULT_ENC.length; ++i)
				areSame = areSame && (RESULT_ENC[i] == EXPECTED[i]);
		} else
			areSame = false;
		if (RESULT_DEC.length == DATA.length) {
			for (int i = 0; i < RESULT_DEC.length; ++i)
				areSame = areSame && (RESULT_DEC[i] == DATA[i]);
		} else
			areSame = false;

		Log.d(MyApplication.APP_TAG, (areSame) ? "OK" : "FAIL");
		return areSame;
	}
}
