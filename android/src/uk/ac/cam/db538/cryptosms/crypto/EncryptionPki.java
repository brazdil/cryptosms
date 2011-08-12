package uk.ac.cam.db538.cryptosms.crypto;

import uk.ac.cam.db538.crypto.AesCbc;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.Pki.PkiNotReadyException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

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
	public byte[] generateRandomData(int length) {
		return mEncryptionNone.generateRandomData(length);
	}

	/**
	 * Returns SHA-512 hash of given data 
	 * @param data
	 * @return
	 */
	public byte[] getHash(byte[] data) {
		return mEncryptionNone.getHash(data);
	}
	
	/**
	 * Returns the length of data after encryption.
	 * Encryption adds some overhead (IV and MAC) and the data is also aligned to 16-byte blocks with random stuff
	 * @param length
	 * @return
	 */
	public int getEncryptedLength(int length) {
		return mEncryptionNone.getEncryptedLength(length);
	}
	
	/**
	 * Returns the least multiple of AES_BLOCKSIZE greater than the argument
	 * @param length
	 * @return
	 */
	public int getAlignedLength(int length) {
		return mEncryptionNone.getAlignedLength(length);
	}

	/**
	 * Encrypts data with Master Key stored with PKI
	 * @param data
	 * @param forceLogIn
	 * @return
	 * @throws EncryptionException
	 */
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
	public byte[] encryptSymmetricWithMasterKey(byte[] data) throws EncryptionException {
		return encryptSymmetricWithMasterKey(data, false);
	}
	
	/**
	 * Encrypts data with given key
	 * @param data
	 * @return
	 * @throws EncryptionException
	 */
	public byte[] encryptSymmetric(byte[] data, byte[] key) throws EncryptionException {
		// align data for MAC checking
		data = LowLevel.wrapData(data, getAlignedLength(data.length));
		// generate everything
		byte[] iv = generateRandomData(Encryption.IV_LENGTH);
		byte[] mac = getHash(data);
		// encrypt
		byte[] dataEncrypted = AesCbc.encrypt(data, iv, key, true, false);
		
		// save everything
		byte[] result = new byte[dataEncrypted.length + Encryption.ENCRYPTION_OVERHEAD];
		// MAC
		System.arraycopy(mac, 0, result, 0, Encryption.MAC_LENGTH);
		// IV 
		System.arraycopy(iv, 0, result, Encryption.MAC_LENGTH, Encryption.IV_LENGTH);
		//data
		System.arraycopy(dataEncrypted, 0, result, Encryption.ENCRYPTION_OVERHEAD, dataEncrypted.length);
		
		return result;
	}
	
	/**
	 * Decrypts data with Master Key stored with PKI
	 * @param data
	 * @return
	 * @throws EncryptionException
	 */
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
	public byte[] decryptSymmetricWithMasterKey(byte[] data) throws EncryptionException {
		return decryptSymmetricWithMasterKey(data, false);
	}
	
	/**
	 * Decrypts data with given key
	 * @param data
	 * @return
	 * @throws EncryptionException
	 */
	public byte[] decryptSymmetric(byte[] data, byte[] key) throws EncryptionException {
		// cut the file up
		byte[] macSaved = LowLevel.cutData(data, 0, Encryption.MAC_LENGTH);
		byte[] iv = LowLevel.cutData(data, Encryption.MAC_LENGTH, Encryption.IV_LENGTH);
		byte[] dataEncrypted = LowLevel.cutData(data, Encryption.ENCRYPTION_OVERHEAD, data.length - Encryption.ENCRYPTION_OVERHEAD);
		
		// decrypt
		byte[] dataDecrypted = AesCbc.decrypt(dataEncrypted, iv, key, false);
		// generate new MAC
		byte[] macReal = getHash(dataDecrypted);
		
		// compare MACs
		boolean isCorrect = true;
		for (int i = 0; i < Encryption.MAC_LENGTH; ++i)
			isCorrect = isCorrect && macSaved[i] == macReal[i];
		if (isCorrect)
			return dataDecrypted;
		else
			throw new EncryptionException(new WrongKeyException());
	}

//	public boolean testEncryption() throws EncryptionException {
//		PKIwrapper PKI = MyApplication.getSingleton().getPki();
//		
//		Log.d(MyApplication.APP_TAG, "ENCRYPTION TEST");
//		byte[] KEY_256 = LowLevel.fromHex("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4");
//		byte[] DATA = Charset.toAscii8("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc euismod malesuada urna at cursus. Morbi magna felis, mattis id.AAA");
//		byte[] IV = LowLevel.fromHex("39F23369A9D9BACFA530E26304231461");
//		byte[] EXPECTED = LowLevel.fromHex("39F23369A9D9BACFA530E26304231461B15AF6E11EAC0584EB38528E27E17490298115F39D41DE251F9F35726BE1BE47E5D0CFBFFCB4B6B98EC2CBF82AC82B68A83F0B595E6A7E54CB31C7399AA23E941850909B4FA33438B403AEA03DF9F309395A4D4C329FF0F06F7DF048D871D305F507D084D69DAF680D3A4826397FE4934032028957B1988C4A7E645F37B998A6");
//		byte[] RESULT_ENC = new byte[0];
//		byte[] RESULT_DEC = new byte[0];
//		
//		Log.d(MyApplication.APP_TAG, "Waiting...");
//		try {
//			RESULT_ENC = PKI.encryptSymmetric(DATA, KEY_256, IV, true);
//			RESULT_DEC = PKI.decryptSymmetric(EXPECTED, KEY_256, true, true);
//		} catch (NotConnectedException e) {
//			throw new EncryptionException(e);
//		} catch (TimeoutException e) {
//			throw new EncryptionException(e);
//		} catch (DeclinedException e) {
//			throw new EncryptionException(e);
//		} catch (PKIErrorException e) {
//			throw new EncryptionException(e);
//		}
//		
//		Log.d(MyApplication.APP_TAG, "Checking...");
//		boolean areSame = true;
//		if (RESULT_ENC.length == EXPECTED.length) {
//			for (int i = 0; i < RESULT_ENC.length; ++i)
//				areSame = areSame && (RESULT_ENC[i] == EXPECTED[i]);
//		} else
//			areSame = false;
//		if (RESULT_DEC.length == DATA.length) {
//			for (int i = 0; i < RESULT_DEC.length; ++i)
//				areSame = areSame && (RESULT_DEC[i] == DATA[i]);
//		} else
//			areSame = false;
//
//		Log.d(MyApplication.APP_TAG, (areSame) ? "OK" : "FAIL");
//		return areSame;
//	}
}
