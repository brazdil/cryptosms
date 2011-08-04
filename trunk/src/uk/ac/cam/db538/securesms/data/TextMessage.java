package uk.ac.cam.db538.securesms.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import uk.ac.cam.db538.securesms.Encryption;
import uk.ac.cam.db538.securesms.data.CompressedText.TextCharset;
import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.SessionKeys;
import uk.ac.cam.db538.securesms.storage.StorageFileException;

public class TextMessage extends Message {
	private static final int LENGTH_FIRST_HEADER = 1;
	private static final int LENGTH_FIRST_ID = 1;
	private static final int LENGTH_FIRST_DATALENGTH = 2;
	private static final int LENGTH_FIRST_ENCRYPTION = Encryption.ENCRYPTION_OVERHEAD;
	
	private static final int OFFSET_FIRST_HEADER = 0;
	private static final int OFFSET_FIRST_ID = OFFSET_FIRST_HEADER + LENGTH_FIRST_HEADER;
	private static final int OFFSET_FIRST_DATALENGTH = OFFSET_FIRST_ID + LENGTH_FIRST_ID;
	private static final int OFFSET_FIRST_ENCRYPTION = OFFSET_FIRST_DATALENGTH + LENGTH_FIRST_DATALENGTH;
	private static final int OFFSET_FIRST_MESSAGEBODY = OFFSET_FIRST_ENCRYPTION + LENGTH_FIRST_ENCRYPTION;
	
	public static final int LENGTH_FIRST_MESSAGEBODY = LENGTH_MESSAGE - OFFSET_FIRST_MESSAGEBODY;
	
	private static final int LENGTH_NEXT_HEADER = 1;
	private static final int LENGTH_NEXT_ID = 1;
	private static final int LENGTH_NEXT_INDEX = 1;
	
	private static final int OFFSET_NEXT_HEADER = 0;
	private static final int OFFSET_NEXT_ID = OFFSET_NEXT_HEADER + LENGTH_NEXT_HEADER;
	private static final int OFFSET_NEXT_INDEX = OFFSET_NEXT_ID + LENGTH_NEXT_ID;
	private static final int OFFSET_NEXT_MESSAGEBODY = OFFSET_NEXT_INDEX + LENGTH_NEXT_INDEX;
	
	public static final int LENGTH_NEXT_MESSAGEBODY = LENGTH_MESSAGE - OFFSET_NEXT_MESSAGEBODY;

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
			len = Math.min(remains, (index == 0) ? LENGTH_FIRST_MESSAGEBODY : LENGTH_NEXT_MESSAGEBODY);
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
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		ByteBuffer buf = ByteBuffer.allocate(LENGTH_MESSAGE);
		int index = 0, offset;
		
		SessionKeys keys =  mStorage.getParent().getSessionKeysForSIM(context);
		if (keys == null)
			throw new MessageException("No keys found");
		
		// get the data, add random data to fit the messages exactly and encrypt it
		byte[] data = getStoredData();
		int totalBytes = LENGTH_FIRST_MESSAGEBODY;
		while (totalBytes <= data.length)
			totalBytes += LENGTH_NEXT_MESSAGEBODY;
		data = Encryption.encryptSymmetric(LowLevel.wrapData(data, totalBytes), keys.getSessionKey_Out());

		// first message (always)
		byte header = MESSAGE_FIRST;
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
		header = MESSAGE_NEXT;
		try {
			while (true) {
				buf = ByteBuffer.allocate(LENGTH_MESSAGE);
				++index;
				buf.put(header);
				buf.put(keys.getNextID_Out());
				buf.put(LowLevel.getBytesUnsignedByte(index));
				buf.put(LowLevel.cutData(data, offset, LENGTH_NEXT_MESSAGEBODY));
				offset += LENGTH_NEXT_MESSAGEBODY;
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
		int count = 1; 
		int remains = text.getData().length - LENGTH_FIRST_MESSAGEBODY;
		while (remains > 0) {
			count++;
			remains -= LENGTH_NEXT_MESSAGEBODY;
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
		int len = text.getDataLength() - LENGTH_FIRST_MESSAGEBODY;
		while (len > 0)
			len -= LENGTH_NEXT_MESSAGEBODY;
		return -len;
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
							default: // ERROR
								if (!error[0]) {
									error[0] = true;
									listener.onError();
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
									keys = mStorage.getParent().getSessionKeysForSIM(context);
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
}
