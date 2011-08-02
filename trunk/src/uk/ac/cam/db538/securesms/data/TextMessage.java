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
	private static final int LENGTH_HEADER = 1;
	private static final int LENGTH_ID = 1;
	private static final int LENGTH_DATALENGTH = 2;
	private static final int LENGTH_IV = Encryption.KEY_LENGTH;
	private static final int LENGTH_MAC = Encryption.KEY_LENGTH;
	
	private static final int OFFSET_HEADER = 0;
	private static final int OFFSET_ID = OFFSET_HEADER + LENGTH_HEADER;
	private static final int OFFSET_DATALENGTH = OFFSET_ID + LENGTH_ID;
	private static final int OFFSET_IV = OFFSET_DATALENGTH + LENGTH_DATALENGTH;
	private static final int OFFSET_MAC = OFFSET_IV + LENGTH_IV;
	private static final int OFFSET_MESSAGEBODY = OFFSET_MAC + LENGTH_MAC;
	
	private static final int LENGTH_MESSAGEBODY = LENGTH_FIRST_MESSAGE - OFFSET_MESSAGEBODY;
	
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
		int remains = text.getLength();
		
		//int lenFirst = Math.min(remains, )
		//byte[] dataFirst = new b		
	}
	
	public MessageData getStorage() {
		return mStorage;
	}
}
