package uk.ac.cam.db538.securesms.data;

import java.io.IOException;
import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;

import uk.ac.cam.db538.securesms.MyApplication;
import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.StorageFileException;

public abstract class Message {
	public static final int LENGTH_MESSAGE = 133;
	protected static final int LENGTH_HEADER = 1;
	protected static final int OFFSET_HEADER = 0;

	public static class MessageException extends Exception {
		private static final long serialVersionUID = 4922446456153260918L;
		
		public MessageException() {
			super();
		}

		public MessageException(String message) {
			super(message);
		}
	}
	
	public static interface MessageSentListener {
		public void onMessageSent();
		public void onError(String message);
	}
	
	protected static final byte HEADER_KEY_FIRST = (byte) 0x00;	// no bits set
	protected static final byte HEADER_KEY_PART = (byte) 0x40;	// second bit set
	protected static final byte HEADER_MESSAGE_FIRST = (byte) 0x80;		// first bit set
	protected static final byte HEADER_MESSAGE_PART = (byte) 0xC0;			// first and second bit set
	
	public static enum MessageType {
		KEY_FIRST,
		KEY_PART,
		MESSAGE_FIRST,
		MESSAGE_PART,
		UNKNOWN
	}
	
	protected MessageData mStorage;
    
    public Message(MessageData storage) {
		mStorage = storage;
	}
    
    public MessageData getStorage() {
    	return mStorage;
    }
    
    /**
     * Returns the length of data stored in the storage file for this message
     * @return
     * @throws StorageFileException
     * @throws IOException
     */
    public int getStoredDataLength() throws StorageFileException, IOException {
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
     * @throws IOException
     */
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
    
    private static final String SENT_SMS_ACTION = "SENT_SMS_ACTION"; 
    
    protected void internalSmsSend(String phoneNumber, byte[] data, Context context, BroadcastReceiver sentReceiver) {
    	// prepare the receiver for SENT notification
    	Intent sentIntent = new Intent(SENT_SMS_ACTION);
    	PendingIntent sentPI = PendingIntent.getBroadcast(
    								context.getApplicationContext(), 0, 
    								sentIntent, 0);
    	context.registerReceiver(sentReceiver, new IntentFilter(SENT_SMS_ACTION));
    	
    	// send the data
    	SmsManager.getDefault().sendDataMessage(phoneNumber, null, MyApplication.getSmsPort(), data, sentPI, null);
    }
          
    public abstract ArrayList<byte[]> getBytes(Context context) throws StorageFileException, IOException, MessageException;
    public abstract void sendSMS(String phoneNumber, Context context, MessageSentListener listener) throws StorageFileException, IOException, MessageException;
    
    public static MessageType getMessageType(byte[] data) {
    	byte headerType = (byte) (data[0] & 0xC0); // takes first two bits only
    	if (headerType == HEADER_KEY_FIRST)
    		return MessageType.KEY_FIRST;
    	else if (headerType == HEADER_KEY_PART)
    		return MessageType.KEY_PART;
    	else if (headerType == HEADER_MESSAGE_FIRST)
    		return MessageType.MESSAGE_FIRST;
    	else if (headerType == HEADER_MESSAGE_PART)
    		return MessageType.MESSAGE_PART;
    	else
    		return MessageType.UNKNOWN;
    }
}
