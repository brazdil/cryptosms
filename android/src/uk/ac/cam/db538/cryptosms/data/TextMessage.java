package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.content.Context;
import android.util.Log;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.SimCard;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.data.PendingParser.ParseResult;
import uk.ac.cam.db538.cryptosms.data.PendingParser.PendingParseResult;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
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
		return CompressedText.decode(getStoredData());	
	}
    
    public uk.ac.cam.db538.cryptosms.storage.MessageData.MessageType getType() {
    	return mStorage.getMessageType();
    }
	
	public void setText(CompressedText text) throws StorageFileException, MessageException {
		byte[] data = text.getAlignedData();

		// initialise
		int pos = 0, index = 0, len;
		int remains = data.length;
		mStorage.setAscii(text.getCharset() == TextCharset.ASCII);
		mStorage.setCompressed(text.isCompressed());
		mStorage.setNumberOfParts(getMessagePartsCount(text.getDataLength()));
		// save
		while (remains > 0) {
			len = Math.min(remains, (index == 0) ? LENGTH_FIRST_DATA : LENGTH_PART_MESSAGE_DATA);
			mStorage.setPartData(index++, LowLevel.cutData(data, pos, len));
			pos += len;
			remains -= len;
		}
		mStorage.saveToFile();
	}
	
	private static int getLengthComplete(int lenText) {
		lenText = Encryption.getEncryption().getSymmetricEncryptedLength(lenText);
		lenText += LENGTH_FIRST_MESSAGE_LENGTH;
		return lenText;
	}
	
	public static int getMessagePartsCount(int lenText) {
		return LowLevel.roundUpDivision(getLengthComplete(lenText), LENGTH_DATA);
	}
	
	private boolean mKeyIncremented = false;
	
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
		
		mKeyIncremented = false;
				
		// get the data, add random data to fit the messages exactly and encrypt it
		byte[] dataText = getStoredData();
		int lengthText = dataText.length;
		byte[] dataEncrypted = Encryption.getEncryption().encryptSymmetric(dataText, keys.getSessionKey_Out());
		int lengthComplete = getLengthComplete(lengthText);
		byte[] dataComplete = new byte[lengthComplete];
		System.arraycopy(LowLevel.getBytesUnsignedShort(lengthText), 0, dataComplete, OFFSET_FIRST_MESSAGE_LENGTH - OFFSET_DATA, LENGTH_FIRST_MESSAGE_LENGTH);
		System.arraycopy(dataEncrypted, 0, dataComplete, OFFSET_FIRST_MESSAGE_DATA - OFFSET_DATA, dataEncrypted.length);
		
		int countParts = getMessagePartsCount(lengthText);
		ArrayList<byte[]> listParts = new ArrayList<byte[]>(countParts);

		byte id = keys.getNextID_Out();
		int index = 0;
		
		Log.d(MyApplication.APP_TAG, "ENCRYPT - " + id + " - " + LowLevel.toHex(keys.getSessionKey_Out()));
		Log.d(MyApplication.APP_TAG, "DATA - " + LowLevel.toHex(dataEncrypted));
		
		for (int i = 0; i < countParts; ++i) {
			int lengthData = Math.min(LENGTH_DATA, dataComplete.length - i * LENGTH_DATA);
			int lengthPart = OFFSET_DATA + lengthData;
			byte[] dataPart = new byte[lengthPart];

			dataPart[OFFSET_HEADER] = HEADER_TEXT;
			dataPart[OFFSET_ID] = id;
			dataPart[OFFSET_INDEX] = LowLevel.getBytesUnsignedByte(index++);
			System.arraycopy(dataComplete, i * LENGTH_DATA, dataPart, OFFSET_DATA, lengthData);
			
			listParts.add(dataPart);
		}
		
		return listParts;
	}
	
	
	
	@Override
	public void sendSMS(String phoneNumber, Context context,
			MessageSendingListener listener) throws StorageFileException,
			MessageException, EncryptionException {
		mStorage.setMessageType(uk.ac.cam.db538.cryptosms.storage.MessageData.MessageType.OUTGOING);
		super.sendSMS(phoneNumber, context, listener);
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

	public static ParseResult parseTextMessage(ArrayList<Pending> idGroup) {
		EncryptionInterface crypto = Encryption.getEncryption();
		
		try {
			// check the sender
			Contact contact = Contact.getContact(MyApplication.getSingleton().getApplicationContext(), idGroup.get(0).getSender());
			if (!contact.existsInDatabase())
				return new ParseResult(idGroup, PendingParseResult.UNKNOWN_SENDER, null);
			
			String sender = idGroup.get(0).getSender();
			
			// find the keys
			Conversation conv = Conversation.getConversation(sender);
			if (conv == null)
				return new ParseResult(idGroup, PendingParseResult.NO_SESSION_KEYS, null);
			SessionKeys keys = conv.getSessionKeys(SimCard.getSingleton().getNumber());
			if (keys == null)
				return new ParseResult(idGroup, PendingParseResult.NO_SESSION_KEYS, null);
			
			// find the first part and retrieve the length of data
			int dataLength = -1;
			for (Pending p : idGroup)
				if (getMessageIndex(p.getData()) == 0) {
					dataLength = getMessageDataLength(p.getData());
					break;
				}
			if (dataLength == -1)
				// first part not found
				return new ParseResult(idGroup, PendingParseResult.MISSING_PARTS, null);
			
			// join the parts
			int countParts = getMessagePartsCount(dataLength);
			byte[] dataJoined = null;
			try {
				dataJoined = joinParts(idGroup, countParts);
			} catch (JoiningException ex) {
				return new ParseResult(idGroup, ex.getReason(), null);
			}
			
			// check the data length
			if (dataLength > dataJoined.length)
				return new ParseResult(idGroup, PendingParseResult.CORRUPTED_DATA, null);
			
			// take just the good stuff
			int offset = OFFSET_FIRST_MESSAGE_DATA - OFFSET_DATA;
			byte[] dataEncrypted = LowLevel.cutData(dataJoined, offset, getLengthComplete(dataLength) - offset);
			
			Log.d(MyApplication.APP_TAG, "DATA - " + LowLevel.toHex(dataEncrypted));

			// decrypt
			byte[] dataDecrypted = null;
			int lastId = keys.getLastID_In();
			byte[] keyIn = keys.getSessionKey_In();
			// try hashing the key until it fits
			while (dataDecrypted == null && lastId <= 0xFF) {
				Log.d(MyApplication.APP_TAG, "DECRYPT - " + lastId + " - " + LowLevel.toHex(keyIn));
				try {
					 dataDecrypted = crypto.decryptSymmetric(dataEncrypted, keyIn);
				} catch (EncryptionException e) {
					// this is bad
					return new ParseResult(idGroup, PendingParseResult.INTERNAL_ERROR, null);
				} catch (WrongKeyDecryptionException e) {
					// this is OK, we'll just try another one
					keyIn = crypto.getHash(keyIn);
					lastId++;
				}
			}
			
			// was it decrypted?
			if (dataDecrypted == null)
				return new ParseResult(idGroup, PendingParseResult.COULD_NOT_DECRYPT, null);
			
			// take just the text part
			dataDecrypted = LowLevel.cutData(dataDecrypted, 0, dataLength);
			
			// save to conversation			
			MessageData msgData = MessageData.createMessageData(conv);
			msgData.setMessageType(uk.ac.cam.db538.cryptosms.storage.MessageData.MessageType.INCOMING);
			TextMessage msgText = new TextMessage(msgData);
			try {
				msgText.setText(CompressedText.decode(dataDecrypted));
			} catch (MessageException e) {
				return new ParseResult(idGroup, PendingParseResult.INTERNAL_ERROR, null);
			} catch (DataFormatException e) {
				return new ParseResult(idGroup, PendingParseResult.COULD_NOT_DECRYPT, null);
			}
			
			// looks good, return
			return new ParseResult(idGroup, PendingParseResult.OK_TEXT_MESSAGE, msgText);
			
		} catch (StorageFileException ex) {
			return new ParseResult(idGroup, PendingParseResult.INTERNAL_ERROR, null);
		}
	}

	/**
	 * Returns message ID for both first and following parts of text messages
	 * @param data
	 * @return
	 */
	public static int getMessageId(byte[] data) {
		return LowLevel.getUnsignedByte(data[OFFSET_ID]);
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
	public static int getRemainingBytes(int lenText) {
		int lenComplete = getLengthComplete(lenText);
		int remainingBytesInBlock = (Encryption.SYM_BLOCK_LENGTH - (lenText % Encryption.SYM_BLOCK_LENGTH)) % Encryption.SYM_BLOCK_LENGTH;
		int remainingBlocksInMessage = ((LENGTH_DATA - (lenComplete % LENGTH_DATA)) % LENGTH_DATA) / Encryption.SYM_BLOCK_LENGTH;
		int remainingBytesInMessage = remainingBlocksInMessage * Encryption.SYM_BLOCK_LENGTH;
		
		return remainingBytesInBlock + remainingBytesInMessage;
	}

	/**
	 * Expects encrypted data of the first part of text message 
	 * and returns the data length stored in all the parts
	 * @param data
	 * @return
	 */
	protected static int getMessageDataLength(byte[] data) {
		return LowLevel.getUnsignedShort(data, OFFSET_FIRST_MESSAGE_LENGTH);
	}

	@Override
	protected void onMessageSent(String phoneNumber)
			throws StorageFileException {
		mStorage.setDeliveredAll(true);
		mStorage.saveToFile();
	}

	@Override
	protected void onPartSent(String phoneNumber, int index)
			throws StorageFileException {
		mStorage.setPartDelivered(index, true);
		mStorage.saveToFile();
		
		if (!mKeyIncremented) {
			// it at least something was sent, increment the ID and session keys
			SessionKeys keys = StorageUtils.getSessionKeysForSim(this.getStorage().getParent());
			if (keys != null) {
				keys.incrementOut(1);
				keys.saveToFile();
				mKeyIncremented = true;
			}
		}
		
	}
}
