package uk.ac.cam.db538.securesms.data;

import java.nio.ByteBuffer;

import uk.ac.cam.db538.securesms.encryption.Encryption;

public class LowLevel {
	/**
	 * Expects four bytes and returns an unsigned integer stored in a long that's represented by the bytes.
	 */
	public static long getUnsignedInt(byte[] data) {
		return getUnsignedInt(data, 0);
	}

	/**
	 * Expects four bytes (in an array at specified offset) and returns an unsigned integer stored in a long that's represented by the bytes.
	 */
	public static long getUnsignedInt(byte[] data, int offset) {
		if (offset > data.length - 4)
			throw new IndexOutOfBoundsException();

		long result = data[offset] & 0xFF;
		result <<= 8;
		result |= (data[offset + 1] & 0xFF);
		result <<= 8;
		result |= (data[offset + 2] & 0xFF);
		result <<= 8;
		result |= data[offset + 3] & 0xFF;
		return result;
	}
	
	/**
	 * Expects two bytes and returns a short
	 */
	public static short getShort(byte[] data) {
		return getShort(data, 0);
	}

	/**
	 * Expects two bytes and returns a short
	 */
	public static short getShort(byte[] data, int offset) {
		if (offset > data.length - 2)
			throw new IndexOutOfBoundsException();

		int result = data[offset] & 0xFF;
		result <<= 8;
		result |= (data[offset + 1] & 0xFF);
		return (short) result;
	}

	/**
	 * Expects an unsigned integer stored in the 4 low bytes of a long and returns an array of 4 bytes that represent them.
	 */
	public static byte[] getBytes(long integer) {
		byte[] result = new byte[4];
		result[0] = (byte) ((integer >> 24) & 0xFF);
		result[1] = (byte) ((integer >> 16) & 0xFF);
		result[2] = (byte) ((integer >> 8) & 0xFF);
		result[3] = (byte) (integer & 0xFF);
		return result;
	}
	
	/**
	 * Expects a short and returns an array of 2 bytes that represents it.
	 */
	public static byte[] getBytes(short integer) {
		byte[] result = new byte[2];
		result[0] = (byte) ((integer >> 8) & 0xFF);
		result[1] = (byte) (integer & 0xFF);
		return result;
	}

	/**
	 * Inserts data into an array of specified length. Puts random data behind to fill the rest.
	 * @param data
	 * @param length
	 * @return
	 */
	public static byte[] wrapData(byte[] data, int length) {
		ByteBuffer buffer = ByteBuffer.allocate(length);
		if (data.length >= length)
			buffer.put(data, 0, length);
		else {
			buffer.put(data);
			buffer.put(Encryption.generateRandomData(length - data.length));
		}
		return buffer.array();
	}
	
	/**
	 * Cuts out byte array from a bigger array
	 * @param data
	 * @param offset
	 * @param length
	 * @return
	 */
	public static byte[] cutData(byte[] data, int offset, int length) {
		byte[] result = new byte[length];
		System.arraycopy(data, offset, result, 0, length);
		return result;
	}
}
