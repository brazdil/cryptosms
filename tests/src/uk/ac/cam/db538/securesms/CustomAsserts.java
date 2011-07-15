package uk.ac.cam.db538.securesms;

import junit.framework.*;

public class CustomAsserts {
	   public static void assertArrayEquals(byte[] actual, byte[] expected) {
		   Assert.assertEquals("Array length mismatch", expected.length, actual.length);
		   
		   for(int i = 0; i < expected.length; ++i)
	         Assert.assertEquals("Array mismatch at index " + i + ":", expected[i], actual[i]);
	   }
}
