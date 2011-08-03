package uk.ac.cam.db538.securesms.data;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.StorageFileException;

public class Message {
	public static final int LENGTH_MESSAGE = 140;

	public static class MessageException extends Exception {
		private static final long serialVersionUID = 4922446456153260918L;
		
		public MessageException() {
			super();
		}

		public MessageException(String message) {
			super(message);
		}
	}
	
	protected MessageData	mStorage;
    
    public Message(MessageData storage) {
		mStorage = storage;
	}
    
    public MessageData getStorage() {
    	return mStorage;
    }
	
    public byte[] getStoredData() throws StorageFileException, IOException {
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
                   
}
