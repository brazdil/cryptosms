package uk.ac.cam.db538.cryptosms.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.telephony.SmsManager;

import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionPki;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.utils.CompressedText;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import uk.ac.cam.db538.cryptosms.utils.CompressedText.TextCharset;

public class TextMessage extends Message {
	// same for all parts
	private static final int LENGTH_ID = 1;
	private static final int OFFSET_ID = OFFSET_HEADER + LENGTH_HEADER;

	// first part specific
	private static final int LENGTH_FIRST_DATALENGTH = 2;
	public static final int LENGTH_FIRST_ENCRYPTION = Encryption.ENCRYPTION_OVERHEAD;
	
	private static final int OFFSET_FIRST_DATALENGTH = OFFSET_ID + LENGTH_ID;
	private static final int OFFSET_FIRST_ENCRYPTION = OFFSET_FIRST_DATALENGTH + LENGTH_FIRST_DATALENGTH;
	private static final int OFFSET_FIRST_MESSAGEBODY = OFFSET_FIRST_ENCRYPTION + LENGTH_FIRST_ENCRYPTION;
	
	public static final int LENGTH_FIRST_MESSAGEBODY = MessageData.LENGTH_MESSAGE - OFFSET_FIRST_MESSAGEBODY;
	
	// following parts specific
	private static final int LENGTH_PART_INDEX = 1;
	
	private static final int OFFSET_PART_INDEX = OFFSET_ID + LENGTH_ID;
	private static final int OFFSET_PART_MESSAGEBODY = OFFSET_PART_INDEX + LENGTH_PART_INDEX;
	
	public static final int LENGTH_PART_MESSAGEBODY = MessageData.LENGTH_MESSAGE - OFFSET_PART_MESSAGEBODY;

	public TextMessage(MessageData storage) {
		super(storage);
	}
	
	public CompressedText getText() throws StorageFileException, IOException, DataFormatException {
		return CompressedText.decode(
			getStoredData(),
			mStorage.getAscii() ? TextCharset.ASCII : TextCharset.UNICODE,
			mStorage.getCompressed()
		);
	}
	
	public void setText(CompressedText text) throws IOException, StorageFileException, MessageException {
		byte[] data = text.getData();

		// initialise
		int pos = 0, index = 0, len;
		int remains = data.length;
		mStorage.setAscii(text.getCharset() == TextCharset.ASCII);
		mStorage.setCompressed(text.isCompressed());
		mStorage.setNumberOfParts(TextMessage.computeNumberOfMessageParts(text));
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
	public ArrayList<byte[]> getBytes(Context context) throws StorageFileException, IOException, MessageException {
		EncryptionInterface crypto = Encryption.getEncryption();
		
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		ByteBuffer buf = ByteBuffer.allocate(MessageData.LENGTH_MESSAGE);
		int index = 0, offset;
		
		SessionKeys keys =  StorageUtils.getSessionKeysForSIM(mStorage.getParent(), context);
		if (keys == null)
			throw new MessageException("No keys found");
		
		// get the data, add random data to fit the messages exactly and encrypt it
		byte[] data = getStoredData();
		int alignedLength = crypto.getAlignedLength(data.length);
		int totalBytes = LENGTH_FIRST_MESSAGEBODY;
		while (totalBytes <= alignedLength)
			totalBytes += LENGTH_PART_MESSAGEBODY;

		try {
			data = crypto.encryptSymmetric(LowLevel.wrapData(data, totalBytes), keys.getSessionKey_Out());
		} catch (EncryptionException e1) {
			throw new MessageException(e1.getMessage());
		}
		totalBytes += LENGTH_FIRST_ENCRYPTION; 
		
		// first message (always)
		byte header = HEADER_MESSAGE_FIRST;
		if (mStorage.getAscii())
			header |= (byte) 0x20;
		if (mStorage.getCompressed())
			header |= (byte) 0x10;
		buf.put(header);
		buf.put(keys.getNextID_Out());
		buf.put(LowLevel.getBytesUnsignedShort(getStoredDataLength()));
		buf.put(LowLevel.cutData(data, 0, LENGTH_FIRST_ENCRYPTION + LENGTH_FIRST_MESSAGEBODY));
		list.add(buf.array());
		
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
	public int computeNumberOfMessageParts() throws StorageFileException, IOException, DataFormatException, MessageException {
		return computeNumberOfMessageParts(getText());
	}
	
	/**
	 * Returns the number of messages necessary to send the given text
	 * @return
	 * @throws DataFormatException 
	 * @throws IOException 
	 * @throws StorageFileException 
	 * @throws MessageException 
	 */
	public static int computeNumberOfMessageParts(CompressedText text) throws MessageException {
		return computeNumberOfMessageParts(text.getData().length);
	}
	
	/**
	 * Returns the number of messages necessary to send given number of bytes
	 * @return
	 * @throws DataFormatException 
	 * @throws IOException 
	 * @throws StorageFileException 
	 * @throws MessageException 
	 */
	public static int computeNumberOfMessageParts(int dataLength) throws MessageException {
		dataLength = Encryption.getEncryption().getAlignedLength(dataLength);
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
	public static int remainingBytesInLastMessagePart(CompressedText text) {
		int length = text.getDataLength();
		
		int inThisMessage = length - LENGTH_FIRST_MESSAGEBODY;
		while (inThisMessage > 0)
			inThisMessage -= LENGTH_PART_MESSAGEBODY;
		inThisMessage = -inThisMessage;

		int untilEndOfBlock = (Encryption.AES_BLOCK_LENGTH - (length % Encryption.AES_BLOCK_LENGTH)) % Encryption.AES_BLOCK_LENGTH;
		int remainsBytes = inThisMessage - untilEndOfBlock;
		if (remainsBytes < 0)
			remainsBytes += LENGTH_PART_MESSAGEBODY;
		int remainsBlocks = remainsBytes / Encryption.AES_BLOCK_LENGTH;
			
		int remainsReal = (remainsBlocks * Encryption.AES_BLOCK_LENGTH) + untilEndOfBlock;
		
		return remainsReal;
	}

	/**
	 * Takes the byte arrays created by getBytes() method and sends 
	 * them to the given phone number
	 */
	@Override
	public void sendSMS(String phoneNumber, final Context context, final MessageSentListener listener)
			throws StorageFileException, IOException, MessageException {
		ArrayList<byte[]> dataSMS = getBytes(context);
		final boolean[] sent = new boolean[dataSMS.size()];
		final boolean[] notified = new boolean[dataSMS.size()];
		final boolean[] error = new boolean[1];
		error[0] = false;
		for (int i = 0; i < dataSMS.size(); ++i)
			sent[i] = notified[i] = false;

		for (int i = 0; i < dataSMS.size(); ++i) {
			final int index = i;
			byte[] dataPart = dataSMS.get(i);
			mStorage.setPartDelivered(i, false);
			internalSmsSend(phoneNumber, dataPart, context,
					new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							Resources res = context.getResources();
							
							// SENT notification
							notified[index] = true;
							switch (getResultCode()) {
							case Activity.RESULT_OK:
								sent[index] = true;
								boolean allSent = true;
								for (boolean b : sent)
									allSent = allSent && b;
								if (allSent)
									listener.onMessageSent();
								break;
							case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
								if (!error[0]) {
									error[0] = true;
									listener.onError(res.getString(R.string.error_sending_generic));
								}
								break;
							case SmsManager.RESULT_ERROR_NO_SERVICE:
								if (!error[0]) {
									error[0] = true;
									listener.onError(res.getString(R.string.error_sending_no_service));
								}
								break;
							case SmsManager.RESULT_ERROR_NULL_PDU:
								if (!error[0]) {
									error[0] = true;
									listener.onError(res.getString(R.string.error_sending_null_pdu));
								}
								break;
							case SmsManager.RESULT_ERROR_RADIO_OFF:
								if (!error[0]) {
									error[0] = true;
									listener.onError(res.getString(R.string.error_sending_radio_off));
								}
								break;
							default: // ERROR
								if (!error[0]) {
									error[0] = true;
									listener.onError(res.getString(R.string.error_sending_unknown));
								}
								break;
							}

							boolean allNotified = true, oneSent = false;
							for (boolean b : notified)
								allNotified = allNotified && b;
							for (boolean b : sent)
								oneSent = oneSent || b;
							if (allNotified && oneSent) {
								// it at least something was sent, increment the ID and session keys
								SessionKeys keys;
								try {
									keys = StorageUtils.getSessionKeysForSIM(mStorage.getParent(), context);
									keys.incrementOut(1);
									keys.saveToFile();
								} catch (StorageFileException e) {
								} catch (IOException e) {
								}
							}
						}
					});
		}
	}
	
	/**
	 * Returns message ID for both first and following parts of text messages
	 * @param data
	 * @return
	 */
	public static int getMessageID(byte[] data) {
		return LowLevel.getUnsignedByte(data[OFFSET_ID]);
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

	/**
	 * Expects encrypted data of bot first and non-first part of text message 
	 * and returns its index
	 * @param data
	 * @return
	 */
	public static int getMessageIndex(byte[] data) {
		if (getMessageType(data) == MessageType.MESSAGE_FIRST)
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
		while (count < totalDataLength) {
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
		while (count < totalDataLength) {
			prev = count;
			count += LENGTH_PART_MESSAGEBODY;
		}
		return prev;
	}
}
