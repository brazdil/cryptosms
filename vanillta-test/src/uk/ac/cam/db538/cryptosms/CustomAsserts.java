package uk.ac.cam.db538.cryptosms;

import junit.framework.*;

public class CustomAsserts {
	
	   public static void assertArrayEquals(byte[] actual, int offset1, byte[] expected, int offset2, int length) {
		   Assert.assertTrue("Index out of bounds", actual.length >= offset1 + length);
		   Assert.assertTrue("Index out of bounds", expected.length >= offset2 + length);
		   
		   for(int i = 0; i < length; ++i)
	         Assert.assertEquals("Array mismatch at index " + i + ":", expected[i + offset2], actual[i + offset1]);
	   }
	   
	   public static void assertArrayEquals(byte[] actual, int offset, byte[] expected, int length) {
		   assertArrayEquals(actual, offset, expected, 0, length);
	   }
	   
	   public static void assertArrayEquals(byte[] actual, byte[] expected) {
		   Assert.assertEquals("Array length mismatch", actual.length, expected.length);
		   assertArrayEquals(actual, 0, expected, expected.length);
	   }
}
