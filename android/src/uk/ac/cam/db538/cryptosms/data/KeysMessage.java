package uk.ac.cam.db538.cryptosms.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.util.Log;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.storage.Header;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public class KeysMessage extends Message {
	// first part specific
	private static final int OFFSET_FIRST_DATA = OFFSET_ID + LENGTH_ID;
	public static final int LENGTH_FIRST_DATA = MessageData.LENGTH_MESSAGE - OFFSET_FIRST_DATA;
	
	// following parts specific
	private static final int OFFSET_PART_DATA = OFFSET_PART_INDEX + LENGTH_PART_INDEX;
	
	public static final int LENGTH_PART_DATA = MessageData.LENGTH_MESSAGE - OFFSET_PART_DATA;
	
	public static final int LENGTH_KEYS = 2 * Encryption.SYM_KEY_LENGTH;
	
	private byte[] mDataEncryptedAndSigned;
	private byte[] mKeyOut, mKeyIn;
	private byte mId;
	
	public KeysMessage(byte[] keyOut, byte[] keyIn) {
		mKeyOut = keyOut;
		mKeyIn = keyIn;
	}
	
	public KeysMessage(long contactId, String contactKey) throws StorageFileException, EncryptionException {
		mKeyOut = Encryption.getEncryption().generateRandomData(Encryption.SYM_KEY_LENGTH);
		mKeyIn = Encryption.getEncryption().generateRandomData(Encryption.SYM_KEY_LENGTH);
		
		// generate a pair of keys 
		byte[] data = new byte[LENGTH_KEYS];
		System.arraycopy(mKeyOut, 0, data, 0, Encryption.SYM_KEY_LENGTH);
		System.arraycopy(mKeyIn, 0, data, Encryption.SYM_KEY_LENGTH, Encryption.SYM_KEY_LENGTH);
		
		// encrypt and sign
		mDataEncryptedAndSigned = 
			Encryption.getEncryption().sign(
				Encryption.getEncryption().encryptAsymmetric(data, contactId, contactKey)
			);
		
		// get an ID for this keys
		mId = Header.getHeader().incrementKeyId();
		Header.getHeader().saveToFile();
	}
	
	public byte[] getKeyOut() {
		return mKeyOut;
	}
	
	public byte[] getKeyIn() {
		return mKeyIn;
	}

	/**
	 * Returns data ready to be sent via SMS
	 * @return
	 * @throws IOException 
	 * @throws StorageFileException 
	 * @throws MessageException 
	 */
	@Override
	public ArrayList<byte[]> getBytes() throws StorageFileException, MessageException {
		EncryptionInterface crypto = Encryption.getEncryption();
		
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		ByteBuffer buf = ByteBuffer.allocate(MessageData.LENGTH_MESSAGE);
		int index = 0, offset;
		
		Log.d(MyApplication.APP_TAG, "Keys data: " + LowLevel.toHex(mDataEncryptedAndSigned));
		
		// align to fit data messages exactly
		int alignedLength = mDataEncryptedAndSigned.length;
		int totalBytes = LENGTH_FIRST_DATA;
		while (totalBytes <= alignedLength)
			totalBytes += LENGTH_PART_DATA;
		mDataEncryptedAndSigned = LowLevel.wrapData(mDataEncryptedAndSigned, totalBytes);
		
		// first message (always)
		byte header = HEADER_KEYS_FIRST;
		buf.put(header);
		buf.put(mId);
		buf.put(LowLevel.cutData(mDataEncryptedAndSigned, 0, LENGTH_FIRST_DATA));
		list.add(buf.array());
		Log.d(MyApplication.APP_TAG, "SMS data: " + LowLevel.toHex(buf.array()));
		
		offset = LENGTH_FIRST_DATA;
		header = HEADER_KEYS_PART;
		try {
			while (true) {
				buf = ByteBuffer.allocate(MessageData.LENGTH_MESSAGE);
				++index;
				buf.put(header);
				buf.put(mId);
				buf.put(LowLevel.getBytesUnsignedByte(index));
				buf.put(LowLevel.cutData(mDataEncryptedAndSigned, offset, LENGTH_PART_DATA));
				offset += LENGTH_PART_DATA;
				list.add(buf.array());
			}
		} catch (IndexOutOfBoundsException e) {
			// end
		}
		
		return list;
	}
	
	public static int getEncryptedDataLength() {
		int dataLength = LENGTH_KEYS;
		dataLength = Encryption.getEncryption().getAsymmetricAlignedLength(dataLength);
		dataLength += Encryption.MAC_LENGTH;
		dataLength += Encryption.ASYM_SIGNATURE_LENGTH;
		return dataLength;
	}
	
	/**
	 * Returns the number of messages necessary to send given number of bytes
	 * @return
	 */
	public static int getPartsCount() {
		int dataLength = getEncryptedDataLength();
		int count = 1; 
		int remains = dataLength - LENGTH_FIRST_DATA;
		while (remains > 0) {
			count++;
			remains -= LENGTH_PART_DATA;
		}
		if (count > 255)
			return 255;
		return count;
	}
	

	/**
	 * Expects encrypted data of both first and non-first part of text message 
	 * and returns its index
	 * @param data
	 * @return
	 */
	public static int getMessageIndex(byte[] data) {
		if (getMessageType(data) == MessageType.KEYS_FIRST)
			return (short) 0;
		else
			return LowLevel.getUnsignedByte(data[OFFSET_PART_INDEX]);
	}
	
	/**
	 * Returns stored encrypted data for both first and following parts of text messages
	 * @param data
	 * @return
	 */
	public static byte[] getMessageData(byte[] data) {
		if (getMessageType(data) == MessageType.KEYS_FIRST)
			return LowLevel.cutData(data, OFFSET_FIRST_DATA, LENGTH_FIRST_DATA);
		else
			return LowLevel.cutData(data, OFFSET_PART_DATA, LENGTH_PART_DATA);
	}
	
	/**
	 * Returns the length of relevant data expected in given message part
	 * @param dataLength
	 * @param index
	 * @return
	 */
	public static int getDataPartLength(int index) {
		if (index == 0)
			return LENGTH_FIRST_DATA;
		else 
			return LENGTH_PART_DATA;
	}

	/**
	 * Returns the offset of relevant data expected in given message part
	 * @param dataLength
	 * @param index
	 * @return
	 */
	public static int getDataPartOffset(int index) {
		if (index == 0)
			return 0;
		else
			return LENGTH_FIRST_DATA + (index - 1) * LENGTH_PART_DATA;
	}

	public static int getTotalDataLength() {
		return getDataPartOffset(getPartsCount());
	}
}
