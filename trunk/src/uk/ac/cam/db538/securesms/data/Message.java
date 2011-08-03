package uk.ac.cam.db538.securesms.data;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.StorageFileException;

public class Message {
    protected MessageData	mStorage;
    
    public Message(MessageData storage) {
		mStorage = storage;
	}
	
    protected byte[] getData() throws StorageFileException, IOException {
    	int index = 0, length = 0;
    	byte[] temp;
    	ArrayList<byte[]> data = new ArrayList<byte[]>();
    	
    	try {
			while ((temp = mStorage.getAssignedData(index++)) != null) {
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
