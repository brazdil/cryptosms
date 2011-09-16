/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package uk.ac.cam.db538.cryptosms.utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.spongycastle.crypto.RuntimeCryptoException;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;


/*
 * Class with static methods for encoding/decoding text in different charsets
 */
public class Charset {
	private static final String CHARSET_ASCII = "US-ASCII";

	/**
	 * Checks whether each character in String is representable by 7-bit ASCII.
	 *
	 * @param text the text
	 * @return true, if is convertable to ascii
	 */
	public static boolean isConvertableToAscii(String text) {
		for (int i = 0; i < text.length(); ++i)
			if (text.charAt(i) > 127)
				return false;
		return true;
	}
	
	/**
	 * Computes how many bytes a text would occupy if it was
	 * encoded in 7-bit ASCII.
	 *
	 * @param text the text
	 * @return the int
	 */
	public static int computeLengthInAscii7(String text) {
		int len = text.length();
		return 7 * len / 8 + ((len % 8 == 0) ? 0 : 1);
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
			buffer.put(Encryption.getEncryption().generateRandomData(bufferLength - latin.length - 1));
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
	 *
	 * @param text 			Encoded string
	 * @return the byte[]
	 */
	public static byte[] toAscii8(String text) {
		return toAscii8(text, text.length());
	}

	/**
	 * Turns a string into an 8-bit ASCII series of bytes.
	 *
	 * @param text 			Encoded string
	 * @param bufferLength 	Maximum size of the resulting array
	 * @return the byte[]
	 */
	public static byte[] toAscii8(String text, int bufferLength) {
		return toBytes(text, bufferLength, CHARSET_ASCII);
	}
	
	/**
	 * Takes a byte array with 8-bit ASCII characters in it, and turns it into a string.
	 *
	 * @param latinData 	Data to be processed
	 * @return the string
	 */
	public static String fromAscii8(byte[] latinData) {
		return fromAscii8(latinData, 0, latinData.length);
	}
	
	/**
	 * Takes a byte array with 8-bit ASCII characters in it, and turns it into a string.
	 *
	 * @param latinData 	Data to be processed
	 * @param offset 	Offset in the array
	 * @param len 		Length of data
	 * @return the string
	 */
	public static String fromAscii8(byte[] latinData, int offset, int len) {
		return fromBytes(latinData, offset, len, CHARSET_ASCII);
	}

	/**
	 * Turns a string into an 7-bit ASCII series of bytes.
	 *
	 * @param text 			Encoded string
	 * @return the byte[]
	 */
	public static byte[] toAscii7(String text) {
		return toAscii7(text, computeLengthInAscii7(text));
	}

	/**
	 * Turns a string into an 7-bit ASCII series of bytes.
	 *
	 * @param text 			Encoded string
	 * @param bufferLength 	Maximum size of the resulting array
	 * @return the byte[]
	 */
	public static byte[] toAscii7(String text, int bufferLength) {
		int bigLength = text.length();
		
		byte[] asciiData = toBytes(text, bigLength + 1, CHARSET_ASCII); // +1 is for the ending zero
		byte[] compressedData = new byte[bufferLength];
		
		for (int i = 0; i < bufferLength; ++i)
			compressedData[i] = (byte) 0x00;
		
		int posCompressed = 0;
		int start = 7;
		for (int i = 0; i < bigLength; ++i) {
			if (start == 7) {
				// only position where space is left after the filling
				compressedData[posCompressed] = (byte) (asciiData[i] << 1);
				start = 0;
			} else {
				compressedData[posCompressed++] |= (byte) (asciiData[i] >> (6 - start));
				if (start != 6)
					compressedData[posCompressed] |= (byte) (asciiData[i] << (2 + start));
				++start;
			}
		}
		
		return compressedData;		
	}
	
	/**
	 * Takes a byte array with 7-bit ASCII characters in it, and turns it into a string.
	 *
	 * @param asciiData 	Data to be processed
	 * @return the string
	 */
	public static String fromAscii7(byte[] asciiData) {
		return fromAscii7(asciiData, 0, asciiData.length);
	}
	
	/**
	 * Takes a byte array with 7-bit ASCII characters in it, and turns it into a string.
	 *
	 * @param compressedData the compressed data
	 * @param offset 	Offset in the array
	 * @param len 		Length of data
	 * @return the string
	 */
	public static String fromAscii7(byte[] compressedData, int offset, int len) {
		int bufferLength = len;
		int bigLength = 8 * bufferLength / 7 + ((bufferLength % 7 == 0) ? 0 : 1);
		byte[] asciiData = new byte[bigLength + 1];  // +1 is for the ending zero
		
		for (int i = 0; i < bigLength + 1; ++i)
			asciiData[i] = (byte) 0x00;

		int posCompressed = 0;
		int start = 7;
		int temp;
		for (int i = 0; posCompressed < compressedData.length; ++i) {
			if (start == 7) {
				// only position where all the data fit in one byte
				asciiData[i] = (byte) (((int) compressedData[offset + posCompressed] & 0xFF) >> 1);
				start = 0;
			} else {
				temp = ((((int)compressedData[posCompressed++]) & 0xFF) << (6 - start)) & 0x7F;
				if (start != 6 && posCompressed < compressedData.length)
					temp |= ((((int)compressedData[posCompressed]) & 0xFF) >> (2 + start)) & 0x7F;
				asciiData[i] = (byte) temp;
				++start;
			}
		}
		
		return fromBytes(asciiData, 0, bigLength + 1, CHARSET_ASCII);
	}

	/**
	 * Turns a string into a Unicode series of bytes.
	 *
	 * @param text 			Encoded string
	 * @return the byte[]
	 */
	public static byte[] toUTF16(String text) {
		try {
			return text.getBytes("UTF-16");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeCryptoException();
		}
	}

	/**
	 * Takes a byte array with Unicode characters in it, and turns it into a string.
	 *
	 * @param utfData 	Data to be processed
	 * @return the string
	 */
	public static String fromUTF16(byte[] utfData) {
		try {
			return new String(utfData, "UTF-16");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Turns a string into a Unicode series of bytes.
	 *
	 * @param text 			Encoded string
	 * @return the byte[]
	 */
	public static byte[] toUTF8(String text) {
		try {
			return text.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Takes a byte array with Unicode characters in it, and turns it into a string.
	 *
	 * @param utfData 	Data to be processed
	 * @return the string
	 */
	public static String fromUTF8(byte[] utfData) {
		try {
			return new String(utfData, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
