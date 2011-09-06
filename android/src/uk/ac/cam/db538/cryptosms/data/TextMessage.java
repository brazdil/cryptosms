package uk.ac.cam.db538.cryptosms.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.data.PendingParser.ParseResult;
import uk.ac.cam.db538.cryptosms.data.PendingParser.PendingParseResult;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.utils.CompressedText;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import uk.ac.cam.db538.cryptosms.utils.CompressedText.TextCharset;

public class TextMessage extends Message {
	// first part specific
	protected static final int LENGTH_ID = 1;
	protected static final int OFFSET_ID = OFFSET_HEADER + LENGTH_HEADER;
	protected static final int LENGTH_INDEX = 1;
	protected static final int OFFSET_INDEX = OFFSET_ID + LENGTH_ID;;
	protected static final int OFFSET_DATA = OFFSET_INDEX + LENGTH_INDEX;
	protected static final int LENGTH_DATA = MessageData.LENGTH_MESSAGE - OFFSET_DATA;
	
	private static final int LENGTH_FIRST_MESSAGE_LENGTH = 2;
	
	private static final int OFFSET_FIRST_MESSAGE_LENGTH = OFFSET_DATA;
	private static final int OFFSET_FIRST_MESSAGE_DATA = OFFSET_FIRST_MESSAGE_LENGTH + LENGTH_FIRST_MESSAGE_LENGTH;
	
	public static final int LENGTH_FIRST_DATA = MessageData.LENGTH_MESSAGE - OFFSET_FIRST_MESSAGE_DATA;
	
	// following parts specific
	
	private static final int OFFSET_PART_MESSAGE_DATA = OFFSET_DATA;
	public static final int LENGTH_PART_MESSAGE_DATA = MessageData.LENGTH_MESSAGE - OFFSET_PART_MESSAGE_DATA;

	private MessageData mStorage;
    
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
		mStorage.setNumberOfParts(getMessagePartCount(text.getDataLength()));
		// save
		while (remains > 0) {
			len = Math.min(remains, (index == 0) ? LENGTH_FIRST_DATA : LENGTH_PART_MESSAGE_DATA);
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
	public ArrayList<byte[]> getBytes() throws StorageFileException, MessageException, EncryptionException {
		SessionKeys keys = StorageUtils.getSessionKeysForSim(mStorage.getParent());
		if (keys == null)
			throw new MessageException("No keys found");
				
		// get the data, add random data to fit the messages exactly and encrypt it
		byte[] dataEncrypted = Encryption.getEncryption().encryptSymmetric(getStoredData(), keys.getSessionKey_Out());
		byte[] dataComplete = new byte[LENGTH_FIRST_MESSAGE_LENGTH + dataEncrypted.length];
		System.arraycopy(LowLevel.getBytesUnsignedShort(dataEncrypted.length), 0, dataComplete, OFFSET_FIRST_MESSAGE_LENGTH - OFFSET_DATA, LENGTH_FIRST_MESSAGE_LENGTH);
		System.arraycopy(dataEncrypted, 0, dataComplete, OFFSET_FIRST_MESSAGE_DATA - OFFSET_DATA, dataEncrypted.length);
		
		int countParts = LowLevel.roundUpDivision(dataComplete.length, LENGTH_DATA);
		ArrayList<byte[]> listParts = new ArrayList<byte[]>(countParts);

		byte id = keys.getNextID_Out();
		long index = 0;
		
		for (int i = 0; i < countParts; ++i) {
			int lengthDataPart = Math.min(LENGTH_DATA, dataComplete.length - i * LENGTH_DATA);
			
		}
		
//		
//		// first message (always)
//		byte header = HEADER_TEXT;
//		if (mStorage.getAscii())
//			header |= (byte) 0x08;
//		if (mStorage.getCompressed())
//			header |= (byte) 0x04;
//		buf.put(header);
//		buf.put(keys.getNextID_Out());
//		buf.put(LowLevel.getBytesUnsignedShort(getStoredDataLength()));
//		buf.put(LowLevel.cutData(data, 0, LENGTH_FIRST_ENCRYPTION + LENGTH_FIRST_MESSAGEBODY));
//		list.add(buf.array());
//		Log.d(MyApplication.APP_TAG, "SMS data: " + LowLevel.toHex(buf.array()));
//		
//		offset = LENGTH_FIRST_ENCRYPTION + LENGTH_FIRST_MESSAGEBODY;
//		try {
//			while (true) {
//				buf = ByteBuffer.allocate(MessageData.LENGTH_MESSAGE);
//				++index;
//				buf.put(header);
//				buf.put(keys.getNextID_Out());
//				buf.put(LowLevel.getBytesUnsignedByte(index));
//				buf.put(LowLevel.cutData(data, offset, LENGTH_PART_MESSAGEBODY));
//				offset += LENGTH_PART_MESSAGEBODY;
//				list.add(buf.array());
//			}
//		} catch (IndexOutOfBoundsException e) {
//			// end
//		}
//		
//		return list;
		return null;
	}
	
	public static class JoiningException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 456081152855672327L;
		
		private PendingParseResult mReason;
		
		public JoiningException(PendingParseResult reason) {
			mReason = reason;
		}
		
		public PendingParseResult getReason() {
			return mReason;
		}
		
	}
	
	protected static byte[] joinParts(ArrayList<Pending> idGroup, int expectedGroupSize) throws JoiningException {
		// check we have all the parts
		// there shouldn't be more than 1
		int groupSize = idGroup.size();
		if (groupSize < expectedGroupSize || groupSize <= 0)
			throw new JoiningException(PendingParseResult.MISSING_PARTS);
		else if (groupSize > expectedGroupSize)
			throw new JoiningException(PendingParseResult.REDUNDANT_PARTS);
		
		// get the data
		byte[][] dataParts = new byte[groupSize][];
		int filledParts = 0;
		for (Pending p : idGroup) {
			byte[] dataPart = p.getData();
			int index = getMessageIndex(dataPart);
			if (index >= 0 && index < idGroup.size()) {
				// index is fine, check that there wasn't the same one already
				if (dataParts[index] == null) {
					// first time we stumbled upon this index
					// store the message part data in the array
					dataParts[index] = dataPart;
					filledParts++;
				} else
					// more parts of the same index
					throw new JoiningException(PendingParseResult.REDUNDANT_PARTS);

			} else
				// index is bigger than the number of messages in ID group
				// therefore some parts have to be missing or the data is corrupted
				throw new JoiningException(PendingParseResult.MISSING_PARTS);

		}
		// the array was filled with data, so check that there aren't any missing
		if (filledParts != expectedGroupSize)
			throw new JoiningException(PendingParseResult.MISSING_PARTS);

		
		// lets put the data together
		byte[] dataJoined = new byte[expectedGroupSize * LENGTH_DATA];
		for (int i = 0; i < expectedGroupSize; ++i) {
			try {
				// get the data 
				// it can't be too long, thanks to getMessageData
				// but it can be too short (throws IndexOutOfBounds exception
				byte[] relevantData = LowLevel.cutData(dataParts[i], OFFSET_DATA, LENGTH_DATA);
				System.arraycopy(relevantData, 0, dataJoined, i * LENGTH_DATA, LENGTH_DATA);
			} catch (RuntimeException e) {
				throw new JoiningException(PendingParseResult.CORRUPTED_DATA);
			}
		}
		
		return dataJoined;
	}

	protected static byte getMessageIdByte(byte[] data) {
		return data[OFFSET_ID];
	}

	/**
	 * Returns message ID for both first and following parts of text messages
	 * @param data
	 * @return
	 */
	public static int getMessageId(byte[] data) {
		return LowLevel.getUnsignedByte(getMessageIdByte(data));
	}
	
	/**
	 * Expects encrypted data of both first and non-first part of text message 
	 * and returns its index
	 * @param data
	 * @return
	 */
	public static int getMessageIndex(byte[] data) {
		return LowLevel.getUnsignedByte(data[OFFSET_INDEX]);
	}
	
	/**
	 * Returns how many bytes are left till another message part will be necessary
	 * @param text
	 * @return
	 */
	public static int getRemainingBytes(CompressedText text) {
		int length = text.getDataLength();
		
		int inThisMessage = length - LENGTH_FIRST_DATA;
		while (inThisMessage > 0)
			inThisMessage -= LENGTH_PART_MESSAGE_DATA;
		inThisMessage = -inThisMessage;

		int untilEndOfBlock = (Encryption.SYM_BLOCK_LENGTH - (length % Encryption.SYM_BLOCK_LENGTH)) % Encryption.SYM_BLOCK_LENGTH;
		int remainsBytes = inThisMessage - untilEndOfBlock;
		if (remainsBytes < 0)
			remainsBytes += LENGTH_PART_MESSAGE_DATA;
		int remainsBlocks = remainsBytes / Encryption.SYM_BLOCK_LENGTH;
			
		int remainsReal = (remainsBlocks * Encryption.SYM_BLOCK_LENGTH) + untilEndOfBlock;
		
		return remainsReal;
	}

	/**
	 * Expects encrypted data of the first part of text message 
	 * and returns the data length stored in all the parts
	 * @param data
	 * @return
	 */
	public static int getMessageDataLength(byte[] data) {
		return LowLevel.getUnsignedShort(data, OFFSET_FIRST_MESSAGE_LENGTH);
	}

	public static ParseResult parseTextMessage(ArrayList<Pending> idGroup) {
		return null;
	}

	@Override
	public byte getHeader() {
		return HEADER_TEXT;
	}
	
	protected int getMessagePartCount(int dataLength) {
		return 1 + LowLevel.roundUpDivision(dataLength - LENGTH_FIRST_DATA, LENGTH_PART_MESSAGE_DATA);
	}
}
