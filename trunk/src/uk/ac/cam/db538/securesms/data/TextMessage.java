package uk.ac.cam.db538.securesms.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.content.Context;

import uk.ac.cam.db538.securesms.data.CompressedText.TextCharset;
import uk.ac.cam.db538.securesms.data.Message.MessageException;
import uk.ac.cam.db538.securesms.encryption.Encryption;
import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.SessionKeys;
import uk.ac.cam.db538.securesms.storage.StorageFileException;

public class TextMessage extends Message {
	private static final int LENGTH_FIRST_HEADER = 1;
	private static final int LENGTH_FIRST_ID = 1;
	private static final int LENGTH_FIRST_DATALENGTH = 2;
	private static final int LENGTH_FIRST_IV = Encryption.KEY_LENGTH;
	private static final int LENGTH_FIRST_MAC = Encryption.KEY_LENGTH;
	
	private static final int OFFSET_FIRST_HEADER = 0;
	private static final int OFFSET_FIRST_ID = OFFSET_FIRST_HEADER + LENGTH_FIRST_HEADER;
	private static final int OFFSET_FIRST_DATALENGTH = OFFSET_FIRST_ID + LENGTH_FIRST_ID;
	private static final int OFFSET_FIRST_IV = OFFSET_FIRST_DATALENGTH + LENGTH_FIRST_DATALENGTH;
	private static final int OFFSET_FIRST_MAC = OFFSET_FIRST_IV + LENGTH_FIRST_IV;
	private static final int OFFSET_FIRST_MESSAGEBODY = OFFSET_FIRST_MAC + LENGTH_FIRST_MAC;
	
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
	public ArrayList<byte[]> getSMSData(Context context) throws StorageFileException, IOException, MessageException {
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		ByteBuffer buf = ByteBuffer.allocate(LENGTH_MESSAGE);
		int index = 0;
		
		SessionKeys keys =  mStorage.getParent().getSessionKeysForSIM(context);
		if (keys == null)
			throw new MessageException("No keys found");

		// first message (always)
		byte header = MESSAGE_FIRST;
		if (mStorage.getAscii())
			header |= (byte) 0x20;
		if (mStorage.getCompressed())
			header |= (byte) 0x10;
		buf.put(header);
		buf.put((byte)(keys.getNextID_Out()));
		buf.put(LowLevel.getBytesUnsignedShort(getStoredDataLength()));
		buf.put(Encryption.encryptSymmetric(mStorage.getPartData(index), keys.getSessionKey_Out()));
		list.add(buf.array());
		
		header = MESSAGE_NEXT;
		try {
			while (true) {
				buf.clear();
				++index;
				buf.put(header);
				buf.put((byte)index);
				buf.put(Encryption.encryptSymmetric(mStorage.getPartData(index), keys.getSessionKey_Out()));
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
		int len = text.getLength() - LENGTH_FIRST_MESSAGEBODY;
		while (len > 0)
			len -= LENGTH_NEXT_MESSAGEBODY;
		return -len;
	}
}
