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
package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.content.Context;

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

/*
 * Class representing an encrypted text message
 */
public class TextMessage extends Message {
	// first part specific
	protected static final int LENGTH_ID = 1;
	protected static final int OFFSET_ID = OFFSET_HEADER + LENGTH_HEADER;
	protected static final int LENGTH_INDEX = 1;
	protected static final int OFFSET_INDEX = OFFSET_ID + LENGTH_ID;;
	protected static final int OFFSET_DATA = OFFSET_INDEX + LENGTH_INDEX;
	protected static final int LENGTH_DATA = MessageData.LENGTH_MESSAGE - OFFSET_DATA;
	
	private MessageData mStorage;
	private int mToBeHashed;
    
	/**
	 * Instantiates a new text message.
	 *
	 * @param storage the storage
	 */
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
		mStorage.setNumberOfParts(getMessagePartCount(text.getDataLength()));
		// save
		while (remains > 0) {
			len = Math.min(remains, LENGTH_DATA);
			mStorage.setPartData(index++, LowLevel.cutData(data, pos, len));
			pos += len;
			remains -= len;
		}
		mStorage.saveToFile();
	}
	
	public int getToBeHashed() {
		return mToBeHashed;
	}
	
	public void setToBeHashed(int toBeHashed) {
		mToBeHashed = toBeHashed;
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
		
		int countParts = getMessagePartCount(lengthText);
		ArrayList<byte[]> listParts = new ArrayList<byte[]>(countParts);

		int index = 0;
		byte[] headerAndId = new byte[2];
		byte indexByte;
		
		Encryption.getEncryption().getRandom().nextBytes(headerAndId);
		headerAndId[0] &= (byte) 0x3F; // set first two bits to 0
		headerAndId[0] |= HEADER_TEXT_FIRST; // set first two bits to HEADER_TEXT_FIRST
		
		for (int i = 0; i < countParts; ++i) {
			int lengthData = Math.min(LENGTH_DATA, dataEncrypted.length - i * LENGTH_DATA);
			int lengthPart = OFFSET_DATA + lengthData;
			byte[] dataPart = new byte[lengthPart];
			
			if (index == 0)
				indexByte = LowLevel.getBytesUnsignedByte(countParts); // first part contains number of parts
			else
				indexByte = LowLevel.getBytesUnsignedByte(index);

			dataPart[OFFSET_HEADER] = headerAndId[0];
			dataPart[OFFSET_ID] = headerAndId[1];
			dataPart[OFFSET_INDEX] = indexByte;
			System.arraycopy(dataEncrypted, i * LENGTH_DATA, dataPart, OFFSET_DATA, lengthData);
			
			listParts.add(dataPart);
			index++;
			
			headerAndId[0] &= (byte) 0x3F; // set first two bits to 0
			headerAndId[0] |= HEADER_TEXT_OTHER; // set first two bits to HEADER_TEXT_OTHER
		}
		
		return listParts;
	}
	
	
	
	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.data.Message#sendSMS(java.lang.String, android.content.Context, uk.ac.cam.db538.cryptosms.data.Message.MessageSendingListener)
	 */
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
		
		/**
		 * Instantiates a new joining exception.
		 *
		 * @param reason the reason
		 */
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

	/**
	 * Parses the text message.
	 *
	 * @param idGroup the id group
	 * @return the parses the result
	 */
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
			int countParts = -1;
			for (Pending p : idGroup)
				if (getMessageIndex(p.getData()) == 0) {
					countParts = getMessagePartCount(p.getData());
					break;
				}
			if (countParts < 0)
				// first part not found
				return new ParseResult(idGroup, PendingParseResult.MISSING_PARTS, null);
			else if (countParts == 0)
				// length zero???
				return new ParseResult(idGroup, PendingParseResult.CORRUPTED_DATA, null);
			
			// join the parts
			byte[] dataJoined = null;
			try {
				dataJoined = joinParts(idGroup, countParts);
			} catch (JoiningException ex) {
				return new ParseResult(idGroup, ex.getReason(), null);
			}
			
			// decrypt
			byte[] dataDecrypted = null;
			int toBeHashed = 1;
			
			int blocksTotalMin = LowLevel.roundUpDivision(Encryption.SYM_OVERHEAD, Encryption.SYM_BLOCK_LENGTH);
			int blocksMin = Math.max(blocksTotalMin, (countParts - 1) * LENGTH_DATA / Encryption.SYM_BLOCK_LENGTH);
			int blocksMax = Math.max(blocksTotalMin, countParts * LENGTH_DATA / Encryption.SYM_BLOCK_LENGTH);
			
			for (int blocks = blocksMin; blocks <= blocksMax; ++blocks) {
				toBeHashed = 1;
				byte[] keyIn = keys.getSessionKey_In();
				// try hashing the key until it fits
				while (dataDecrypted == null && toBeHashed <= 10) {
					try {
						 dataDecrypted = crypto.decryptSymmetric(dataJoined, keyIn, blocks);
					} catch (EncryptionException e) {
						// this is bad
						return new ParseResult(idGroup, PendingParseResult.INTERNAL_ERROR, null);
					} catch (WrongKeyDecryptionException e) {
						// this is OK, we'll just try another one
						keyIn = crypto.getHash(keyIn);
						toBeHashed++;
					}
				}
				
				if (dataDecrypted != null)
					break;
			}
			
			// was it decrypted?
			if (dataDecrypted == null)
				return new ParseResult(idGroup, PendingParseResult.COULD_NOT_DECRYPT, null);
			
			// save to conversation			
			MessageData msgData = MessageData.createMessageData(conv);
			msgData.setMessageType(uk.ac.cam.db538.cryptosms.storage.MessageData.MessageType.INCOMING);
			TextMessage msgText = new TextMessage(msgData);
			msgText.setToBeHashed(toBeHashed);
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
	 * Returns message ID for both first and following parts of text messages.
	 *
	 * @param data the data
	 * @return the message id
	 */
	public static int getMessageId(byte[] data) {
		byte[] id = new byte[2];
		id[0] = (byte)(data[OFFSET_HEADER] & 0x3F); // ignore first two bits
		id[1] = data[OFFSET_ID];
		return LowLevel.getUnsignedShort(id);
	}
	
	/**
	 * Expects encrypted data of both first and non-first part of text message
	 * and returns its index.
	 *
	 * @param data the data
	 * @return the message index
	 */
	public static int getMessageIndex(byte[] data) {
		if ((data[OFFSET_HEADER] & 0xC0) == HEADER_TEXT_FIRST)
			return 0;
		else
			return LowLevel.getUnsignedByte(data[OFFSET_INDEX]);
	}
	
	/**
	 * Returns how many bytes are left till another message part will be necessary.
	 *
	 * @param lenText the len text
	 * @return the remaining bytes
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
	protected static int getMessagePartCount(byte[] data) {
		return LowLevel.getUnsignedByte(data[OFFSET_INDEX]);
	}

	private static int getLengthComplete(int lenText) {
		return Encryption.getEncryption().getSymmetricEncryptedLength(lenText);
	}
	
	/**
	 * Gets the message part count.
	 *
	 * @param lenText the len text
	 * @return the message part count
	 */
	public static int getMessagePartCount(int lenText) {
		return LowLevel.roundUpDivision(getLengthComplete(lenText), LENGTH_DATA);
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
