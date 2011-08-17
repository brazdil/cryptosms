package uk.ac.cam.db538.cryptosms.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.util.Log;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Header;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public class KeysMessage extends Message {
	// first part specific
	private static final int OFFSET_FIRST_DATA = OFFSET_ID + LENGTH_ID;;
	public static final int LENGTH_FIRST_DATA = MessageData.LENGTH_MESSAGE - OFFSET_FIRST_DATA;
	
	// following parts specific
	private static final int LENGTH_PART_INDEX = 1;
	
	private static final int OFFSET_PART_INDEX = OFFSET_ID + LENGTH_ID;;
	private static final int OFFSET_PART_DATA = OFFSET_PART_INDEX + LENGTH_PART_INDEX;
	
	public static final int LENGTH_PART_DATA = MessageData.LENGTH_MESSAGE - OFFSET_PART_DATA;
	
	private byte[] mDataEncryptedAndSigned;
	private byte[] mKeyOut, mKeyIn;
	private byte mId;
	
	public KeysMessage(long contactId, String contactKey) throws StorageFileException, EncryptionException {
		mKeyOut = Encryption.getEncryption().generateRandomData(Encryption.SYM_KEY_LENGTH);
		mKeyIn = Encryption.getEncryption().generateRandomData(Encryption.SYM_KEY_LENGTH);
		
		// generate a pair of keys 
		byte[] data = new byte[2 * Encryption.SYM_KEY_LENGTH];
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
		int alignedLength = crypto.getAsymmetricAlignedLength(mDataEncryptedAndSigned.length);
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
	
	/**
	 * Returns the number of messages necessary to send given number of bytes
	 * @return
	 * @throws DataFormatException 
	 * @throws IOException 
	 * @throws StorageFileException 
	 * @throws MessageException 
	 */
	public static int getPartsCount() throws MessageException {
		int dataLength = 2 * Encryption.SYM_KEY_LENGTH;
		dataLength = Encryption.getEncryption().getAsymmetricAlignedLength(dataLength);
		dataLength += Encryption.ASYM_SIGNATURE_LENGTH;
		
		int count = 1; 
		int remains = dataLength - LENGTH_FIRST_DATA;
		while (remains > 0) {
			count++;
			remains -= LENGTH_PART_DATA;
		}
		if (count > 255)
			throw new MessageException("Message too long!");
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
	public static int getExpectedDataLength(int totalDataLength, int index) {
		int prev = 0, count = LENGTH_FIRST_DATA;
		while (count < totalDataLength && index-- > 0) {
			prev = count;
			count += LENGTH_PART_DATA;
		}
		return totalDataLength - prev;
	}

	/**
	 * Returns the offset of relevant data expected in given message part
	 * @param dataLength
	 * @param index
	 * @return
	 */
	public static int getExpectedDataOffset(int totalDataLength, int index) {
		int prev = 0, count = LENGTH_FIRST_DATA;
		while (count < totalDataLength && index-- > 0) {
			prev = count;
			count += LENGTH_PART_DATA;
		}
		return prev;
	}
}
