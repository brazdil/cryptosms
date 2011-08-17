package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.telephony.SmsManager;
import android.util.Log;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;

public abstract class Message {
	// same for all messages
	protected static final int LENGTH_HEADER = 1;
	protected static final int OFFSET_HEADER = 0;
	protected static final int LENGTH_ID = 1;
	protected static final int OFFSET_ID = OFFSET_HEADER + LENGTH_HEADER;

	public static class MessageException extends Exception {
		private static final long serialVersionUID = 4922446456153260918L;
		
		public MessageException() {
			super();
		}

		public MessageException(String message) {
			super(message);
		}
	}
	
	public static interface MessageSendingListener {
		public void onMessageSent();
		public boolean onPartSent(int index);
		public void onError(String message);
	}
	
	protected static final byte HEADER_KEYS_FIRST = (byte) 0x00;	// no bits set
	protected static final byte HEADER_KEYS_PART = (byte) 0x40;	// second bit set
	protected static final byte HEADER_MESSAGE_FIRST = (byte) 0x80;		// first bit set
	protected static final byte HEADER_MESSAGE_PART = (byte) 0xC0;			// first and second bit set
	
	public static enum MessageType {
		KEYS_FIRST,
		KEYS_PART,
		MESSAGE_FIRST,
		MESSAGE_PART,
		UNKNOWN
	}
	
    private static final String SENT_SMS_ACTION = "CRYPTOSMS_SMS_SENT"; 
    
    private void internalSmsSend(String phoneNumber, byte[] data, Context context) {
    	Intent sentIntent = new Intent(SENT_SMS_ACTION);
    	PendingIntent sentPI = PendingIntent.getBroadcast(
    								context.getApplicationContext(), 0, 
    								sentIntent, 0);
    	// send the data
    	SmsManager.getDefault().sendDataMessage(phoneNumber, null, MyApplication.getSmsPort(), data, sentPI, null);
    }

    public abstract ArrayList<byte[]> getBytes() throws StorageFileException, MessageException, EncryptionException;

	/**
	 * Takes the byte arrays created by getBytes() method and sends 
	 * them to the given phone number
	 */
	public void sendSMS(final String phoneNumber, Context context, final MessageSendingListener listener)
			throws StorageFileException, MessageException, EncryptionException {
		final ArrayList<byte[]> dataSms = getBytes();
		
		context.registerReceiver(new BroadcastReceiver() {
			private int indexLast = 0;
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Resources res = context.getResources();
				// check that it arrived OK
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Log.d(MyApplication.APP_TAG, "Sent " + indexLast);
					if (listener.onPartSent(indexLast)) {
						++indexLast;
						if (indexLast < dataSms.size())
							internalSmsSend(phoneNumber, dataSms.get(indexLast), context);
						else {
							context.unregisterReceiver(this);
							listener.onMessageSent();
						}
					} else 
						context.unregisterReceiver(this);
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					context.unregisterReceiver(this);
					listener.onError(res.getString(R.string.error_sending_generic));
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					context.unregisterReceiver(this);
					listener.onError(res.getString(R.string.error_sending_no_service));
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					context.unregisterReceiver(this);
					listener.onError(res.getString(R.string.error_sending_null_pdu));
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					context.unregisterReceiver(this);
					listener.onError(res.getString(R.string.error_sending_radio_off));
					break;
				default: // ERROR
					context.unregisterReceiver(this);
					listener.onError(res.getString(R.string.error_sending_unknown));
					break;
				}
			}
		}, new IntentFilter(SENT_SMS_ACTION));
		
		internalSmsSend(phoneNumber, dataSms.get(0), context);
	}
    
    public static MessageType getMessageType(byte[] data) {
    	byte headerType = (byte) (data[0] & 0xC0); // takes first two bits only
    	if (headerType == HEADER_KEYS_FIRST)
    		return MessageType.KEYS_FIRST;
    	else if (headerType == HEADER_KEYS_PART)
    		return MessageType.KEYS_PART;
    	else if (headerType == HEADER_MESSAGE_FIRST)
    		return MessageType.MESSAGE_FIRST;
    	else if (headerType == HEADER_MESSAGE_PART)
    		return MessageType.MESSAGE_PART;
    	else
    		return MessageType.UNKNOWN;
    }
}
