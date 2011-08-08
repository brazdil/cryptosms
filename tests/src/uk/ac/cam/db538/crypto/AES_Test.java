package uk.ac.cam.db538.crypto;

import uk.ac.cam.db538.securesms.Charset;
import uk.ac.cam.db538.securesms.CustomAsserts;
import uk.ac.cam.db538.securesms.Encryption;
import junit.framework.TestCase;

public class AES_Test extends TestCase {
	
	public static byte[] HEX(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}

	private static final byte[] KEY_128 = HEX("2b7e151628aed2a6abf7158809cf4f3c");
	private static final byte[] KEY_192 = HEX("8e73b0f7da0e6452c810f32b809079e562f8ead2522c6b7b");
	private static final byte[] KEY_256 = HEX("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4");
	
	private static final byte[] DATA1 = HEX("6bc1bee22e409f96e93d7e117393172a");
	private static final byte[] DATA2 = HEX("ae2d8a571e03ac9c9eb76fac45af8e51");
	private static final byte[] DATA3 = HEX("30c81c46a35ce411e5fbc1191a0a52ef");
	private static final byte[] DATA4 = HEX("f69f2445df4f9b17ad2b417be66c3710");
	
	private static final byte[] IV1 = HEX("000102030405060708090A0B0C0D0E0F");
	private static final byte[] IV2 = HEX("7649ABAC8119B246CEE98E9B12E9197D");
	private static final byte[] IV3 = HEX("5086CB9B507219EE95DB113A917678B2");
	private static final byte[] IV4 = HEX("73BED6B8E3C1743B7116E69E22229516");
	private static final byte[] IV5 = HEX("4F021DB243BC633D7178183A9FA071E8");
	private static final byte[] IV6 = HEX("B4D9ADA9AD7DEDF4E5E738763F69145A");
	private static final byte[] IV7 = HEX("571B242012FB7AE07FA9BAAC3DF102E0");
	private static final byte[] IV8 = HEX("F58C4C04D6E5F1BA779EABFB5F7BFBD6");
	private static final byte[] IV9 = HEX("9CFC4E967EDB808D679F777BC6702C7D");
	private static final byte[] IV10 = HEX("39F23369A9D9BACFA530E26304231461");
	
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
			AesCbc.encrypt(testData, iv, aesAlgorithm, true, false),
			expectedResult
			);
		CustomAsserts.assertArrayEquals(
				AesCbc.decrypt(expectedResult, iv, aesAlgorithm, false),
				testData
				);
	}

	private void assertEncryptionCBC(byte[] key, byte[] testData, byte[] iv, byte[] expectedResult) {
		CustomAsserts.assertArrayEquals(
			AesCbc.encrypt(testData, iv, key, true, false),
			expectedResult
			);
		CustomAsserts.assertArrayEquals(
				AesCbc.decrypt(expectedResult, iv, key, false),
				testData
				);
	}

	public void testAES_1() {
		AesAlgorithm aesAlgorithm = new AesAlgorithm();
		
		// AES/ECB
		aesAlgorithm.setKey(KEY_128);
		assertEncryptionECB(aesAlgorithm, DATA1, HEX("3ad77bb40d7a3660a89ecaf32466ef97"));
		assertEncryptionECB(aesAlgorithm, DATA2, HEX("f5d3d58503b9699de785895a96fdbaaf"));
		assertEncryptionECB(aesAlgorithm, DATA3, HEX("43b1cd7f598ece23881b00e3ed030688"));
		assertEncryptionECB(aesAlgorithm, DATA4, HEX("7b0c785e27e8ad3f8223207104725dd4"));
		
		aesAlgorithm.setKey(KEY_192);
		assertEncryptionECB(aesAlgorithm, DATA1, HEX("bd334f1d6e45f25ff712a214571fa5cc"));
		assertEncryptionECB(aesAlgorithm, DATA2, HEX("974104846d0ad3ad7734ecb3ecee4eef"));
		assertEncryptionECB(aesAlgorithm, DATA3, HEX("ef7afd2270e2e60adce0ba2face6444e"));
		assertEncryptionECB(aesAlgorithm, DATA4, HEX("9a4b41ba738d6c72fb16691603c18e0e"));

		aesAlgorithm.setKey(KEY_256);
		assertEncryptionECB(aesAlgorithm, DATA1, HEX("f3eed1bdb5d2a03c064b5a7e3db181f8"));
		assertEncryptionECB(aesAlgorithm, DATA2, HEX("591ccb10d410ed26dc5ba74a31362870"));
		assertEncryptionECB(aesAlgorithm, DATA3, HEX("b6ed21b99ca6f4f9f153e7b1beafed1d"));
		assertEncryptionECB(aesAlgorithm, DATA4, HEX("23304b7a39f9f3ff067d8d8f9e24ecc7"));

		// AES/CBC
		aesAlgorithm.setKey(KEY_128);
		assertEncryptionCBC(aesAlgorithm, DATA1, IV1, HEX("7649abac8119b246cee98e9b12e9197d"));
		assertEncryptionCBC(aesAlgorithm, DATA2, IV2, HEX("5086cb9b507219ee95db113a917678b2"));
		assertEncryptionCBC(aesAlgorithm, DATA3, IV3, HEX("73bed6b8e3c1743b7116e69e22229516"));
		assertEncryptionCBC(aesAlgorithm, DATA4, IV4, HEX("3ff1caa1681fac09120eca307586e1a7"));
		
		aesAlgorithm.setKey(KEY_192);
		assertEncryptionCBC(aesAlgorithm, DATA1, IV1, HEX("4f021db243bc633d7178183a9fa071e8"));
		assertEncryptionCBC(aesAlgorithm, DATA2, IV5, HEX("b4d9ada9ad7dedf4e5e738763f69145a"));
		assertEncryptionCBC(aesAlgorithm, DATA3, IV6, HEX("571b242012fb7ae07fa9baac3df102e0"));
		assertEncryptionCBC(aesAlgorithm, DATA4, IV7, HEX("08b0e27988598881d920a9e64f5615cd"));

		aesAlgorithm.setKey(KEY_256);
		assertEncryptionCBC(aesAlgorithm, DATA1, IV1, HEX("f58c4c04d6e5f1ba779eabfb5f7bfbd6"));
		assertEncryptionCBC(aesAlgorithm, DATA2, IV8, HEX("9cfc4e967edb808d679f777bc6702c7d"));
		assertEncryptionCBC(aesAlgorithm, DATA3, IV9, HEX("39f23369a9d9bacfa530e26304231461"));
		assertEncryptionCBC(aesAlgorithm, DATA4, IV10, HEX("b2eb05e2c39be9fcda6c19078c6a9d1b"));
	}
	
	public void testAES_2() {
		byte[] KEY, IV, PLAINTEXT;
		
		// 128-bit AES CBC
		KEY = HEX("00000000000000000000000000000000");
		IV = HEX("00000000000000000000000000000000");
		assertEncryptionCBC(KEY, HEX("f34481ec3cc627bacd5dc3fb08f273e6"), IV, HEX("0336763e966d92595a567cc9ce537f5e"));
		assertEncryptionCBC(KEY, HEX("9798c4640bad75c7c3227db910174e72"), IV, HEX("a9a1631bf4996954ebc093957b234589"));
		assertEncryptionCBC(KEY, HEX("96ab5c2ff612d9dfaae8c31f30c42168"), IV, HEX("ff4f8391a6a40ca5b25d23bedd44a597"));
		assertEncryptionCBC(KEY, HEX("6a118a874519e64e9963798a503f1d35"), IV, HEX("dc43be40be0e53712f7e2bf5ca707209"));
		assertEncryptionCBC(KEY, HEX("cb9fceec81286ca3e989bd979b0cb284"), IV, HEX("92beedab1895a94faa69b632e5cc47ce"));
		assertEncryptionCBC(KEY, HEX("b26aeb1874e47ca8358ff22378f09144"), IV, HEX("459264f4798f6a78bacb89c15ed3d601"));
		assertEncryptionCBC(KEY, HEX("58c8e00b2631686d54eab84b91f0aca1"), IV, HEX("08a4e2efec8a8e3312ca7460b9040bbf"));
		
		// 256-bit AES CBC
		KEY = HEX("0000000000000000000000000000000000000000000000000000000000000000");
		IV = HEX("00000000000000000000000000000000");
		assertEncryptionCBC(KEY, HEX("014730f80ac625fe84f026c60bfd547d"), IV, HEX("5c9d844ed46f9885085e5d6a4f94c7d7"));
		assertEncryptionCBC(KEY, HEX("0b24af36193ce4665f2825d7b4749c98"), IV, HEX("a9ff75bd7cf6613d3731c77c3b6d0c04"));
		assertEncryptionCBC(KEY, HEX("761c1fe41a18acf20d241650611d90f1"), IV, HEX("623a52fcea5d443e48d9181ab32c7421"));
		assertEncryptionCBC(KEY, HEX("8a560769d605868ad80d819bdba03771"), IV, HEX("38f2c7ae10612415d27ca190d27da8b4"));
		assertEncryptionCBC(KEY, HEX("91fbef2d15a97816060bee1feaa49afe"), IV, HEX("1bc704f1bce135ceb810341b216d7abe"));

		IV = HEX("00000000000000000000000000000000");
		PLAINTEXT = HEX("00000000000000000000000000000000");
		assertEncryptionCBC(HEX("c47b0294dbbbee0fec4757f22ffeee3587ca4730c3d33b691df38bab076bc558"), PLAINTEXT, IV, HEX("46f2fb342d6f0ab477476fc501242c5f"));
		assertEncryptionCBC(HEX("28d46cffa158533194214a91e712fc2b45b518076675affd910edeca5f41ac64"), PLAINTEXT, IV, HEX("4bf3b0a69aeb6657794f2901b1440ad4"));
		assertEncryptionCBC(HEX("c1cc358b449909a19436cfbb3f852ef8bcb5ed12ac7058325f56e6099aab1a1c"), PLAINTEXT, IV, HEX("352065272169abf9856843927d0674fd"));
		assertEncryptionCBC(HEX("984ca75f4ee8d706f46c2d98c0bf4a45f5b00d791c2dfeb191b5ed8e420fd627"), PLAINTEXT, IV, HEX("4307456a9e67813b452e15fa8fffe398"));
		assertEncryptionCBC(HEX("b43d08a447ac8609baadae4ff12918b9f68fc1653f1269222f123981ded7a92f"), PLAINTEXT, IV, HEX("4663446607354989477a5c6f0f007ef4"));
		assertEncryptionCBC(HEX("1d85a181b54cde51f0e098095b2962fdc93b51fe9b88602b3f54130bf76a5bd9"), PLAINTEXT, IV, HEX("531c2c38344578b84d50b3c917bbb6e1"));
		assertEncryptionCBC(HEX("dc0eba1f2232a7879ded34ed8428eeb8769b056bbaf8ad77cb65c3541430b4cf"), PLAINTEXT, IV, HEX("fc6aec906323480005c58e7e1ab004ad"));
		assertEncryptionCBC(HEX("f8be9ba615c5a952cabbca24f68f8593039624d524c816acda2c9183bd917cb9"), PLAINTEXT, IV, HEX("a3944b95ca0b52043584ef02151926a8"));
		assertEncryptionCBC(HEX("797f8b3d176dac5b7e34a2d539c4ef367a16f8635f6264737591c5c07bf57a3e"), PLAINTEXT, IV, HEX("a74289fe73a4c123ca189ea1e1b49ad5"));
		assertEncryptionCBC(HEX("6838d40caf927749c13f0329d331f448e202c73ef52c5f73a37ca635d4c47707"), PLAINTEXT, IV, HEX("b91d4ea4488644b56cf0812fa7fcf5fc"));
		assertEncryptionCBC(HEX("ccd1bc3c659cd3c59bc437484e3c5c724441da8d6e90ce556cd57d0752663bbc"), PLAINTEXT, IV, HEX("304f81ab61a80c2e743b94d5002a126b"));
		assertEncryptionCBC(HEX("13428b5e4c005e0636dd338405d173ab135dec2a25c22c5df0722d69dcc43887"), PLAINTEXT, IV, HEX("649a71545378c783e368c9ade7114f6c"));
		assertEncryptionCBC(HEX("07eb03a08d291d1b07408bf3512ab40c91097ac77461aad4bb859647f74f00ee"), PLAINTEXT, IV, HEX("47cb030da2ab051dfc6c4bf6910d12bb"));
		assertEncryptionCBC(HEX("90143ae20cd78c5d8ebdd6cb9dc1762427a96c78c639bccc41a61424564eafe1"), PLAINTEXT, IV, HEX("798c7c005dee432b2c8ea5dfa381ecc3"));
		assertEncryptionCBC(HEX("b7a5794d52737475d53d5a377200849be0260a67a2b22ced8bbef12882270d07"), PLAINTEXT, IV, HEX("637c31dc2591a07636f646b72daabbe7"));
		assertEncryptionCBC(HEX("fca02f3d5011cfc5c1e23165d413a049d4526a991827424d896fe3435e0bf68e"), PLAINTEXT, IV, HEX("179a49c712154bbffbe6e7a84a18e220"));
	}
	
	public void testAES3() {
		// Case #1: Encrypting 16 bytes (1 block) using AES-CBC with 128-bit key
		assertEncryptionCBC(HEX("06a9214036b8a15b512e03d534120006"), 
		                    Charset.toAscii8("Single block msg"), 
		                    HEX("3dafba429d9eb430b422da802c9fac41"), 
		                    HEX("e353779c1079aeb82708942dbe77181a"));

		// Case #2: Encrypting 32 bytes (2 blocks) using AES-CBC with 128-bit key
		assertEncryptionCBC(HEX("c286696d887c9aa0611bbb3e2025a45a"), 
		                    HEX("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"), 
		                    HEX("562e17996d093d28ddb3ba695a2e6f58"), 
		                    HEX("d296cd94c2cccf8a3a863028b5e1dc0a7586602d253cfff91b8266bea6d61ab1"));
		
		// Case #3: Encrypting 48 bytes (3 blocks) using AES-CBC with 128-bit key
  		assertEncryptionCBC(HEX("6c3ea0477630ce21a2ce334aa746c2cd"), 
                            Charset.toAscii8("This is a 48-byte message (exactly 3 AES blocks)"),
                            HEX("c782dc4c098c66cbd9cd27d825682c81"), 
                            HEX("d0a02b3836451753d493665d33f0e8862dea54cdb293abc7506939276772f8d5021c19216bad525c8579695d83ba2684"));

		// Case #4: Encrypting 64 bytes (4 blocks) using AES-CBC with 128-bit key
  		assertEncryptionCBC(HEX("56e47a38c5598974bc46903dba290349"), 
                            HEX("a0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedf"),
                            HEX("8ce82eefbea0da3c44699ed7db51b7d9"), 
                            HEX("c30e32ffedc0774e6aff6af0869f71aa0f3af07a9a31a9c684db207eb0ef8e4e35907aa632c3ffdf868bb7b29d3d46ad83ce9f9a102ee99d49a53e87f4c3da55"));
	}
	
	public void testAES4() {
		byte[] testData, encryptedData, decryptedData;
		byte[] iv = Encryption.generateRandomData(Encryption.IV_LENGTH);
		byte[] key = Encryption.generateRandomData(Encryption.KEY_LENGTH);

		// 125-byte data
		testData = Charset.toAscii8("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc euismod malesuada urna at cursus. Morbi magna felis, mattis id.");
		encryptedData = AesCbc.encrypt(testData, iv, key, true, true);
		decryptedData = AesCbc.decrypt(encryptedData, iv, key, true);
		CustomAsserts.assertArrayEquals(decryptedData, testData);

		// 121-byte data
		testData = Charset.toAscii8("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam sed elit diam, sed bibendum felis. Nunc sed massa metus.");
		encryptedData = AesCbc.encrypt(testData, iv, key, true, true);
		decryptedData = AesCbc.decrypt(encryptedData, iv, key, true);
		CustomAsserts.assertArrayEquals(decryptedData, testData);

		// 112-byte data
		testData = Charset.toAscii8("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis ullamcorper felis id tortor dictum a tempor metus.");
		encryptedData = AesCbc.encrypt(testData, iv, key, true, true);
		decryptedData = AesCbc.decrypt(encryptedData, iv, key, true);
		CustomAsserts.assertArrayEquals(decryptedData, testData);
	}
}
