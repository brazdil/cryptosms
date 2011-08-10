package uk.ac.cam.db538.cryptosms.utils;

import uk.ac.cam.db538.cryptosms.CustomAsserts;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionNone;
import junit.framework.TestCase;

public class Charset_Test extends TestCase {
	public void setUp() {
		EncryptionNone.initEncryption();
	}
	
	public void testToLatin() {
		// should trim after 5 bytes
		String strABCDE = "ABCDEFG";
		byte[] byteABCDE = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44, (byte) 0x45 };
		CustomAsserts.assertArrayEquals(Charset.toAscii8(strABCDE, 5), byteABCDE);

		// check only first four bytes - fifth is random
		String strABC = "ABC";
		byte[] byteABC = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x00, (byte) 0x00 };
		CustomAsserts.assertArrayEquals(Charset.toAscii8(strABC, 5), 0, byteABC, 4);
	}

	public void testFromLatin() {
		// simple
		byte[] byteABC = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x00, (byte) 0x00 };
		assertEquals(Charset.fromAscii8(byteABC), "ABC");

		// with offset
		byte[] byteABC2 = new byte[] { (byte) 0x00, (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x00, (byte) 0x00 };
		assertEquals(Charset.fromAscii8(byteABC2, 1, 5), "ABC");
		
		// full
		byte[] byteABCDE = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44, (byte) 0x45 };
		assertEquals(Charset.fromAscii8(byteABCDE), "ABCDE");
	}
}
