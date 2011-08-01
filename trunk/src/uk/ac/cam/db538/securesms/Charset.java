package uk.ac.cam.db538.securesms;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;

public class Charset {
	private static final String CHARSET_ASCII = "US-ASCII";
	private static final String CHARSET_LATIN = "ISO-8859-1";
	private static final String CHARSET_UTF8 = "UTF-8";

	/**
	 * Checks whether each character in String is representable by 7-bit ASCII
	 * @param text
	 * @return
	 */
	public static boolean isConvertableToAscii(String text) {
		for (int i = 0; i < text.length(); ++i)
			if (text.charAt(i) > 127)
				return false;
		return true;
	}
	
	/**
	 * Turns a string into a series of bytes. 
	 * @param text				Encoded string
	 * @param bufferLength		Maximum size of the resulting array
	 * @param charset			Resulting charset
	 * @return
	 */
	private static byte[] toBytes(String text, int bufferLength, String charset) {
		ByteBuffer buffer = ByteBuffer.allocate(bufferLength);

		byte[] latin = null;
		try {
			latin = text.getBytes(charset);
		} catch (UnsupportedEncodingException e) {
			return null;
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
	 * Takes a byte array with characters in it, and turns it into a string
	 * @param bytesData		Data to be processed
	 * @param offset		Offset in the array
	 * @param len			Length of data
	 * @return
	 */
	private static String fromBytes(byte[] bytesData, int offset, int len, String charset) {
		int length = 0;
		while (length < len && bytesData[offset + length] != 0)
			++length;
		
		try {
			return new String(bytesData, offset, Math.min(len, length), CHARSET_ASCII);
		} catch (UnsupportedEncodingException ex) {
			return null;
		}		
	}

	/**
	 * Turns a string into an 8-bit ASCII series of bytes. 
	 * @param text				Encoded string
	 * @param bufferLength		Maximum size of the resulting array
	 * @return
	 */
	public static byte[] toLatin(String text, int bufferLength) {
		return toBytes(text, bufferLength, CHARSET_LATIN);
	}
	
	/**
	 * Takes a byte array with 8-bit ASCII characters in it, and turns it into a string
	 * @param latinData		Data to be processed
	 * @return
	 */
	public static String fromLatin(byte[] latinData) {
		return fromLatin(latinData, 0, latinData.length);
	}
	
	/**
	 * Takes a byte array with 8-bit ASCII characters in it, and turns it into a string
	 * @param latinData		Data to be processed
	 * @param offset		Offset in the array
	 * @param len			Length of data
	 * @return
	 */
	public static String fromLatin(byte[] latinData, int offset, int len) {
		return fromBytes(latinData, offset, len, CHARSET_LATIN);
	}

	/**
	 * Turns a string into an 7-bit ASCII series of bytes. 
	 * @param text				Encoded string
	 * @param bufferLength		Maximum size of the resulting array
	 * @return
	 */
	public static byte[] toAscii(String text, int bufferLength) {
		return toBytes(text, bufferLength, CHARSET_ASCII);
	}
	
	/**
	 * Takes a byte array with 7-bit ASCII characters in it, and turns it into a string
	 * @param asciiData		Data to be processed
	 * @return
	 */
	public static String fromAscii(byte[] asciiData) {
		return fromAscii(asciiData, 0, asciiData.length);
	}
	
	/**
	 * Takes a byte array with 7-bit ASCII characters in it, and turns it into a string
	 * @param asciiData		Data to be processed
	 * @param offset		Offset in the array
	 * @param len			Length of data
	 * @return
	 */
	public static String fromAscii(byte[] asciiData, int offset, int len) {
		return fromBytes(asciiData, offset, len, CHARSET_ASCII);
	}

	/**
	 * Turns a string into an UTF-8 series of bytes. 
	 * @param text				Encoded string
	 * @param bufferLength		Maximum size of the resulting array
	 * @return
	 */
	public static byte[] toUtf8(String text, int bufferLength) {
		return toBytes(text, bufferLength, CHARSET_ASCII);
	}
	
	/**
	 * Takes a byte array with UTF-8 characters in it, and turns it into a string
	 * @param utfData		Data to be processed
	 * @return
	 */
	public static String fromUtf8(byte[] utfData) {
		return fromUtf8(utfData, 0, utfData.length);
	}
	
	/**
	 * Takes a byte array with UTF-8 characters in it, and turns it into a string
	 * @param utfData		Data to be processed
	 * @param offset		Offset in the array
	 * @param len			Length of data
	 * @return
	 */
	public static String fromUtf8(byte[] utfData, int offset, int len) {
		return fromBytes(utfData, offset, len, CHARSET_UTF8);
	}

}
