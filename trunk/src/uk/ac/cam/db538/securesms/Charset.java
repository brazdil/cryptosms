package uk.ac.cam.db538.securesms;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;

public class Charset {
	private static final String CHARSET_ASCII = "US-ASCII";
	private static final String CHARSET_LATIN = "ISO-8859-1";

	public static boolean isConvertableToAscii(String text) {
		for (int i = 0; i < text.length(); ++i)
			if (text.charAt(i) > 127)
				return false;
		return true;
	}
	
	public static byte[] toAscii(String text) {
		return null; 
	}

	/**
	 * Turns a string into an ASCII series of bytes. 
	 * @param text				Encoded string
	 * @param bufferLength		Maximum size of the resulting array
	 * @return
	 */
	public static byte[] toLatin(String text, int bufferLength) {
		ByteBuffer buffer = ByteBuffer.allocate(bufferLength);

		byte[] latin = null;
		try {
			latin = text.getBytes(CHARSET_LATIN);
		} catch (UnsupportedEncodingException e) {
		}
		
		if (latin.length < bufferLength) {
			buffer.put(latin);
			buffer.put((byte) 0x00);
			buffer.put(Encryption.generateRandomData(bufferLength - latin.length - 1));
		}
		else
			buffer.put(latin, 0, bufferLength);
		
		return buffer.array();		
	}
	
	/**
	 * Takes a byte array with ASCII characters in it, and turns it into a string
	 * @param latinData		Data to be processed
	 * @return
	 */
	public static String fromLatin(byte[] latinData) {
		return fromLatin(latinData, 0, latinData.length);
	}
	
	/**
	 * Takes a byte array with ASCII characters in it, and turns it into a string
	 * @param latinData		Data to be processed
	 * @param offset		Offset in the array
	 * @param len			Length of data
	 * @return
	 */
	public static String fromLatin(byte[] latinData, int offset, int len) {
		int length = 0;
		while (length < len && latinData[offset + length] != 0)
			++length;
		
		try {
			return new String(latinData, offset, Math.min(len, length), CHARSET_LATIN);
		} catch (UnsupportedEncodingException ex) {
			return null;
		}		
	}

}
