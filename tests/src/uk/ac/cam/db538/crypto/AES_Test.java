package uk.ac.cam.db538.crypto;

import uk.ac.cam.db538.securesms.CustomAsserts;
import junit.framework.TestCase;

public class AES_Test extends TestCase {
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}

	private static final byte[] KEY_128 = hexStringToByteArray("2b7e151628aed2a6abf7158809cf4f3c");
	private static final byte[] KEY_192 = hexStringToByteArray("8e73b0f7da0e6452c810f32b809079e562f8ead2522c6b7b");
	private static final byte[] KEY_256 = hexStringToByteArray("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4");
	
	private static final byte[] DATA1 = hexStringToByteArray("6bc1bee22e409f96e93d7e117393172a");
	private static final byte[] DATA2 = hexStringToByteArray("ae2d8a571e03ac9c9eb76fac45af8e51");
	private static final byte[] DATA3 = hexStringToByteArray("30c81c46a35ce411e5fbc1191a0a52ef");
	private static final byte[] DATA4 = hexStringToByteArray("f69f2445df4f9b17ad2b417be66c3710");
	
	private static final byte[] IV1 = hexStringToByteArray("000102030405060708090A0B0C0D0E0F");
	private static final byte[] IV2 = hexStringToByteArray("7649ABAC8119B246CEE98E9B12E9197D");
	private static final byte[] IV3 = hexStringToByteArray("5086CB9B507219EE95DB113A917678B2");
	private static final byte[] IV4 = hexStringToByteArray("73BED6B8E3C1743B7116E69E22229516");
	private static final byte[] IV5 = hexStringToByteArray("4F021DB243BC633D7178183A9FA071E8");
	private static final byte[] IV6 = hexStringToByteArray("B4D9ADA9AD7DEDF4E5E738763F69145A");
	private static final byte[] IV7 = hexStringToByteArray("571B242012FB7AE07FA9BAAC3DF102E0");
	private static final byte[] IV8 = hexStringToByteArray("F58C4C04D6E5F1BA779EABFB5F7BFBD6");
	private static final byte[] IV9 = hexStringToByteArray("9CFC4E967EDB808D679F777BC6702C7D");
	private static final byte[] IV10 = hexStringToByteArray("39F23369A9D9BACFA530E26304231461");
	
	private void assertEncryptionECB(AesAlgorithm aesAlgorithm, byte[] testData, byte[] expectedResult) {
		CustomAsserts.assertArrayEquals(
			aesAlgorithm.encrypt(testData),
			expectedResult
			);
		CustomAsserts.assertArrayEquals(
			aesAlgorithm.decrypt(expectedResult),
			testData
			);
	}
	
	private void assertEncryptionCBC(AesAlgorithm aesAlgorithm, byte[] testData, byte[] iv, byte[] expectedResult) {
		CustomAsserts.assertArrayEquals(
			AesCbc.encrypt(testData, iv, aesAlgorithm),
			expectedResult
			);
		CustomAsserts.assertArrayEquals(
				AesCbc.decrypt(expectedResult, iv, aesAlgorithm),
				testData
				);
	}

	public void testAES() {
		AesAlgorithm aesAlgorithm = new AesAlgorithm();
		
		// AES/ECB
		aesAlgorithm.setKey(KEY_128);
		assertEncryptionECB(aesAlgorithm, DATA1, hexStringToByteArray("3ad77bb40d7a3660a89ecaf32466ef97"));
		assertEncryptionECB(aesAlgorithm, DATA2, hexStringToByteArray("f5d3d58503b9699de785895a96fdbaaf"));
		assertEncryptionECB(aesAlgorithm, DATA3, hexStringToByteArray("43b1cd7f598ece23881b00e3ed030688"));
		assertEncryptionECB(aesAlgorithm, DATA4, hexStringToByteArray("7b0c785e27e8ad3f8223207104725dd4"));
		
		aesAlgorithm.setKey(KEY_192);
		assertEncryptionECB(aesAlgorithm, DATA1, hexStringToByteArray("bd334f1d6e45f25ff712a214571fa5cc"));
		assertEncryptionECB(aesAlgorithm, DATA2, hexStringToByteArray("974104846d0ad3ad7734ecb3ecee4eef"));
		assertEncryptionECB(aesAlgorithm, DATA3, hexStringToByteArray("ef7afd2270e2e60adce0ba2face6444e"));
		assertEncryptionECB(aesAlgorithm, DATA4, hexStringToByteArray("9a4b41ba738d6c72fb16691603c18e0e"));

		aesAlgorithm.setKey(KEY_256);
		assertEncryptionECB(aesAlgorithm, DATA1, hexStringToByteArray("f3eed1bdb5d2a03c064b5a7e3db181f8"));
		assertEncryptionECB(aesAlgorithm, DATA2, hexStringToByteArray("591ccb10d410ed26dc5ba74a31362870"));
		assertEncryptionECB(aesAlgorithm, DATA3, hexStringToByteArray("b6ed21b99ca6f4f9f153e7b1beafed1d"));
		assertEncryptionECB(aesAlgorithm, DATA4, hexStringToByteArray("23304b7a39f9f3ff067d8d8f9e24ecc7"));

		// AES/CBC
		aesAlgorithm.setKey(KEY_128);
		assertEncryptionCBC(aesAlgorithm, DATA1, IV1, hexStringToByteArray("7649abac8119b246cee98e9b12e9197d"));
		assertEncryptionCBC(aesAlgorithm, DATA2, IV2, hexStringToByteArray("5086cb9b507219ee95db113a917678b2"));
		assertEncryptionCBC(aesAlgorithm, DATA3, IV3, hexStringToByteArray("73bed6b8e3c1743b7116e69e22229516"));
		assertEncryptionCBC(aesAlgorithm, DATA4, IV4, hexStringToByteArray("3ff1caa1681fac09120eca307586e1a7"));
		
		aesAlgorithm.setKey(KEY_192);
		assertEncryptionCBC(aesAlgorithm, DATA1, IV1, hexStringToByteArray("4f021db243bc633d7178183a9fa071e8"));
		assertEncryptionCBC(aesAlgorithm, DATA2, IV5, hexStringToByteArray("b4d9ada9ad7dedf4e5e738763f69145a"));
		assertEncryptionCBC(aesAlgorithm, DATA3, IV6, hexStringToByteArray("571b242012fb7ae07fa9baac3df102e0"));
		assertEncryptionCBC(aesAlgorithm, DATA4, IV7, hexStringToByteArray("08b0e27988598881d920a9e64f5615cd"));

		aesAlgorithm.setKey(KEY_256);
		assertEncryptionCBC(aesAlgorithm, DATA1, IV1, hexStringToByteArray("f58c4c04d6e5f1ba779eabfb5f7bfbd6"));
		assertEncryptionCBC(aesAlgorithm, DATA2, IV8, hexStringToByteArray("9cfc4e967edb808d679f777bc6702c7d"));
		assertEncryptionCBC(aesAlgorithm, DATA3, IV9, hexStringToByteArray("39f23369a9d9bacfa530e26304231461"));
		assertEncryptionCBC(aesAlgorithm, DATA4, IV10, hexStringToByteArray("b2eb05e2c39be9fcda6c19078c6a9d1b"));
	}
}
