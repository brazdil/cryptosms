package uk.ac.cam.db538.securesms.data;

import java.io.IOException;
import java.util.zip.DataFormatException;

import uk.ac.cam.db538.securesms.data.CompressedText.TextCharset;
import uk.ac.cam.db538.securesms.encryption.Encryption;
import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.StorageFileException;

public class TextMessage implements Message {
	// RENAME!!!
	private static final int LENGTH_FIRST_MESSAGE = 140;
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
	
	private static final int LENGTH_FIRST_MESSAGEBODY = LENGTH_FIRST_MESSAGE - OFFSET_FIRST_MESSAGEBODY;
	
	private MessageData	mStorage;
		
	public TextMessage(MessageData storage) {
		mStorage = storage;
	}
	
	public CompressedText getText() throws StorageFileException, IOException, DataFormatException {
		return CompressedText.decode(
			mStorage.getAssignedData(),
			mStorage.getAscii() ? TextCharset.ASCII7 : TextCharset.UNICODE,
			mStorage.getCompressed()
		);
	}
	
	public void setText(CompressedText text) {
		byte[] data = text.getData();

		int pos = 0;
		int remains = data.length;
		
		int lenFirst = Math.min(remains, LENGTH_FIRST_MESSAGEBODY);
		byte[] dataFirst = LowLevel.cutData(data, pos, lenFirst);
		
		//ArrayList<MessageDataPart> parts = new ArrayList<MessageDataPart>();
		
	}
	
	public MessageData getStorage() {
		return mStorage;
	}
}
