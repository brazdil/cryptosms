package uk.ac.cam.db538.cryptosms.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.util.Log;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.utils.CompressedText;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import uk.ac.cam.db538.cryptosms.utils.CompressedText.TextCharset;

public class TextMessage extends Message {
	// first part specific
	private static final int LENGTH_FIRST_DATALENGTH = 2;
	public static final int LENGTH_FIRST_ENCRYPTION = Encryption.SYM_OVERHEAD;
	
	private static final int OFFSET_FIRST_DATALENGTH = OFFSET_ID + LENGTH_ID;
	private static final int OFFSET_FIRST_ENCRYPTION = OFFSET_FIRST_DATALENGTH + LENGTH_FIRST_DATALENGTH;
	private static final int OFFSET_FIRST_MESSAGEBODY = OFFSET_FIRST_ENCRYPTION + LENGTH_FIRST_ENCRYPTION;
	
	public static final int LENGTH_FIRST_MESSAGEBODY = MessageData.LENGTH_MESSAGE - OFFSET_FIRST_MESSAGEBODY;
	
	// following parts specific
	private static final int LENGTH_PART_INDEX = 1;
	
	private static final int OFFSET_PART_INDEX = OFFSET_ID + LENGTH_ID;
	private static final int OFFSET_PART_MESSAGEBODY = OFFSET_PART_INDEX + LENGTH_PART_INDEX;
	
	public static final int LENGTH_PART_MESSAGEBODY = MessageData.LENGTH_MESSAGE - OFFSET_PART_MESSAGEBODY;

	protected MessageData mStorage;
    
	public TextMessage(MessageData storage) {
		super();
		mStorage = storage;
	}
	
    public MessageData getStorage() {
    	return mStorage;
    }
    
    /**
     * Returns the length of data stored in the storage file for this message
     * @return
     * @throws StorageFileException
     */
    public int getStoredDataLength() throws StorageFileException {
    	int index = 0, length = 0;
    	byte[] temp;
    	try {
			while ((temp = mStorage.getPartData(index++)) != null)
				length += temp.length;
    	} catch (IndexOutOfBoundsException e) {
    		// ends this way
    	}
    	return length;
    }
	
    /**
     * Returns all the data stored in the storage file for this message
     * @return
     * @throws StorageFileException
     */
    public byte[] getStoredData() throws StorageFileException {
    	int index = 0, length = 0;
    	byte[] temp;
    	ArrayList<byte[]> data = new ArrayList<byte[]>();
    	
    	try {
			while ((temp = mStorage.getPartData(index++)) != null) {
				length += temp.length;
				data.add(temp);
			}
    	} catch (IndexOutOfBoundsException e) {
    		// ends this way
    	}
		
		temp = new byte[length];
		index = 0;
		for (byte[] part : data) {
			System.arraycopy(part, 0, temp, index, part.length);
			index += part.length;
		}
		
		return temp;
    }

    public CompressedText getText() throws StorageFileException, DataFormatException {
		return CompressedText.decode(
			getStoredData(),
			mStorage.getAscii() ? TextCharset.ASCII : TextCharset.UNICODE,
			mStorage.getCompressed()
		);
	}
	
	public void setText(CompressedText text) throws StorageFileException, MessageException {
		byte[] data = text.getData();

		// initialise
		int pos = 0, index = 0, len;
		int remains = data.length;
		mStorage.setAscii(text.getCharset() == TextCharset.ASCII);
		mStorage.setCompressed(text.isCompressed());
		mStorage.setNumberOfParts(TextMessage.getPartsCount(text));
		// save
		while (remains > 0) {
			len = Math.min(remains, (index == 0) ? LENGTH_FIRST_MESSAGEBODY : LENGTH_PART_MESSAGEBODY);
			mStorage.setPartData(index++, LowLevel.cutData(data, pos, len));
			pos += len;
			remains -= len;
		}
		mStorage.saveToFile();
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
		
		SessionKeys keys = StorageUtils.getSessionKeysForSim(mStorage.getParent());
		if (keys == null)
			throw new MessageException("No keys found");
		
		// get the data, add random data to fit the messages exactly and encrypt it
		byte[] data = getStoredData();
		Log.d(MyApplication.APP_TAG, "Text data: " + LowLevel.toHex(data));
		int alignedLength = crypto.getSymmetricAlignedLength(data.length);
		int totalBytes = LENGTH_FIRST_MESSAGEBODY;
		while (totalBytes <= alignedLength)
			totalBytes += LENGTH_PART_MESSAGEBODY;

		int alignedTotalBytes = totalBytes - (totalBytes % Encryption.SYM_BLOCK_LENGTH);
		data = LowLevel.wrapData(data, alignedTotalBytes);
		Log.d(MyApplication.APP_TAG, "Aligned data: " + LowLevel.toHex(data));

		data = crypto.encryptSymmetric(data, keys.getSessionKey_Out());
		Log.d(MyApplication.APP_TAG, "Encrypted data: " + LowLevel.toHex(data));
		Log.d(MyApplication.APP_TAG, "Session key: " + LowLevel.toHex(keys.getSessionKey_Out()));
		totalBytes += LENGTH_FIRST_ENCRYPTION; 
		data = LowLevel.wrapData(data, totalBytes);
		
		// first message (always)
		byte header = HEADER_MESSAGE_FIRST;
		if (mStorage.getAscii())
			header |= (byte) 0x08;
		if (mStorage.getCompressed())
			header |= (byte) 0x04;
		buf.put(header);
		buf.put(keys.getNextID_Out());
		buf.put(LowLevel.getBytesUnsignedShort(getStoredDataLength()));
		buf.put(LowLevel.cutData(data, 0, LENGTH_FIRST_ENCRYPTION + LENGTH_FIRST_MESSAGEBODY));
		list.add(buf.array());
		Log.d(MyApplication.APP_TAG, "SMS data: " + LowLevel.toHex(buf.array()));
		
		offset = LENGTH_FIRST_ENCRYPTION + LENGTH_FIRST_MESSAGEBODY;
		header = HEADER_MESSAGE_PART;
		try {
			while (true) {
				buf = ByteBuffer.allocate(MessageData.LENGTH_MESSAGE);
				++index;
				buf.put(header);
				buf.put(keys.getNextID_Out());
				buf.put(LowLevel.getBytesUnsignedByte(index));
				buf.put(LowLevel.cutData(data, offset, LENGTH_PART_MESSAGEBODY));
				offset += LENGTH_PART_MESSAGEBODY;
				list.add(buf.array());
			}
		} catch (IndexOutOfBoundsException e) {
			// end
		}
		
		return list;
	}
	
	/**
	 * Returns the number of messages necessary to send the assigned message
	 * @return
	 * @throws DataFormatException 
	 * @throws IOException 
	 * @throws StorageFileException 
	 * @throws MessageException 
	 */
	public int getPartsCount() throws StorageFileException, DataFormatException, MessageException {
		return getPartsCount(getText());
	}
	
	/**
	 * Returns the number of messages necessary to send the given text
	 * @return
	 * @throws DataFormatException 
	 * @throws IOException 
	 * @throws StorageFileException 
	 * @throws MessageException 
	 */
	public static int getPartsCount(CompressedText text) throws MessageException {
		return getPartsCount(text.getData().length);
	}
	
	/**
	 * Returns the number of messages necessary to send given number of bytes
	 * @return
	 * @throws DataFormatException 
	 * @throws IOException 
	 * @throws StorageFileException 
	 * @throws MessageException 
	 */
	public static int getPartsCount(int dataLength) throws MessageException {
		dataLength = Encryption.getEncryption().getSymmetricAlignedLength(dataLength);
		int count = 1; 
		int remains = dataLength - LENGTH_FIRST_MESSAGEBODY;
		while (remains > 0) {
			count++;
			remains -= LENGTH_PART_MESSAGEBODY;
		}
		if (count > 255)
			throw new MessageException("Message too long!");
		return count;
	}
	
	/**
	 * Returns how many bytes are left till another message part will be necessary
	 * @param text
	 * @return
	 */
	public static int getRemainingBytes(CompressedText text) {
		int length = text.getDataLength();
		
		int inThisMessage = length - LENGTH_FIRST_MESSAGEBODY;
		while (inThisMessage > 0)
			inThisMessage -= LENGTH_PART_MESSAGEBODY;
		inThisMessage = -inThisMessage;

		int untilEndOfBlock = (Encryption.SYM_BLOCK_LENGTH - (length % Encryption.SYM_BLOCK_LENGTH)) % Encryption.SYM_BLOCK_LENGTH;
		int remainsBytes = inThisMessage - untilEndOfBlock;
		if (remainsBytes < 0)
			remainsBytes += LENGTH_PART_MESSAGEBODY;
		int remainsBlocks = remainsBytes / Encryption.SYM_BLOCK_LENGTH;
			
		int remainsReal = (remainsBlocks * Encryption.SYM_BLOCK_LENGTH) + untilEndOfBlock;
		
		return remainsReal;
	}

	/**
	 * Returns stored encrypted data for both first and following parts of text messages
	 * @param data
	 * @return
	 */
	public static byte[] getMessageData(byte[] data) {
		if (getMessageType(data) == MessageType.MESSAGE_FIRST)
			return LowLevel.cutData(data, OFFSET_FIRST_MESSAGEBODY, LENGTH_FIRST_MESSAGEBODY);
		else
			return LowLevel.cutData(data, OFFSET_PART_MESSAGEBODY, LENGTH_PART_MESSAGEBODY);
	}
	
	/**
	 * Returns stored encryption overhead for first part of text messages
	 * @param data
	 * @return
	 */
	public static byte[] getMessageEncryptionData(byte[] data) {
			return LowLevel.cutData(data, OFFSET_FIRST_ENCRYPTION, LENGTH_FIRST_ENCRYPTION);
	}

	/**
	 * Returns the length of relevant data expected in given message part
	 * @param dataLength
	 * @param index
	 * @return
	 */
	public static int getExpectedDataLength(int totalDataLength, int index) {
		int prev = 0, count = LENGTH_FIRST_MESSAGEBODY;
		while (count < totalDataLength && index-- > 0) {
			prev = count;
			count += LENGTH_PART_MESSAGEBODY;
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
		int prev = 0, count = LENGTH_FIRST_MESSAGEBODY;
		while (count < totalDataLength && index-- > 0) {
			prev = count;
			count += LENGTH_PART_MESSAGEBODY;
		}
		return prev;
	}

	/**
	 * Expects encrypted data of the first part of text message 
	 * and returns the data length stored in all the parts
	 * @param data
	 * @return
	 */
	public static int getMessageDataLength(byte[] data) {
		return LowLevel.getUnsignedShort(data, OFFSET_FIRST_DATALENGTH);
	}

}
