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
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public abstract class Message {
	// same for all messages
	protected static final int LENGTH_HEADER = 1;
	protected static final int OFFSET_HEADER = 0;
	protected static final int LENGTH_ID = 1;
	protected static final int OFFSET_ID = OFFSET_HEADER + LENGTH_HEADER;
	protected static final int LENGTH_PART_INDEX = 1;
	protected static final int OFFSET_PART_INDEX = OFFSET_ID + LENGTH_ID;;

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
		public void onPartSent(int index);
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
    private static long mMessageCounter = 0;

    public abstract ArrayList<byte[]> getBytes() throws StorageFileException, MessageException, EncryptionException;
    
	/**
	 * Takes the byte arrays created by getBytes() method and sends 
	 * them to the given phone number
	 */
	public void sendSMS(final String phoneNumber, Context context, final MessageSendingListener listener)
			throws StorageFileException, MessageException, EncryptionException {
		final ArrayList<byte[]> dataSms = getBytes();
		int size = dataSms.size();
		final boolean[] deliveryConfirms = new boolean[size];
		for (int i = 0; i < size; ++i) {
			String intentName = SENT_SMS_ACTION + (mMessageCounter++);
			final int intentIndex = i;
			context.registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Resources res = context.getResources();
					context.unregisterReceiver(this);
					// check that it arrived OK
					switch (getResultCode()) {
					case Activity.RESULT_OK:
						Log.d(MyApplication.APP_TAG, "Sent " + intentIndex);
						// notify and save
						deliveryConfirms[intentIndex] = true;
						listener.onPartSent(intentIndex);
						// check we have all
						boolean all = true;
						for (boolean b : deliveryConfirms)
							all = all && b;
						if (all)
							listener.onMessageSent();
						break;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
						listener.onError(res.getString(R.string.error_sending_generic));
						break;
					case SmsManager.RESULT_ERROR_NO_SERVICE:
						listener.onError(res.getString(R.string.error_sending_no_service));
						break;
					case SmsManager.RESULT_ERROR_NULL_PDU:
						listener.onError(res.getString(R.string.error_sending_null_pdu));
						break;
					case SmsManager.RESULT_ERROR_RADIO_OFF:
						listener.onError(res.getString(R.string.error_sending_radio_off));
						break;
					default: // ERROR
						listener.onError(res.getString(R.string.error_sending_unknown));
						break;
					}
				}
			}, new IntentFilter(intentName));
		
	    	Intent sentIntent = new Intent(intentName);
	    	PendingIntent sentPI = PendingIntent.getBroadcast(
	    								context.getApplicationContext(), 0, 
	    								sentIntent, 0);
	    	
	    	Log.d(MyApplication.APP_TAG, sentIntent.toString());
	    	
	    	// send the data
	    	SmsManager.getDefault().sendDataMessage(phoneNumber, null, MyApplication.getSmsPort(), dataSms.get(i), sentPI, null);
		}
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

	/**
	 * Returns message ID for both first and following parts of text messages
	 * @param data
	 * @return
	 */
	public static int getMessageID(byte[] data) {
		return LowLevel.getUnsignedByte(data[OFFSET_ID]);
	}
	
	/**
	 * Expects encrypted data of both first and non-first part of text message 
	 * and returns its index
	 * @param data
	 * @return
	 */
	public static int getMessageIndex(byte[] data) {
		switch (getMessageType(data)) {
		case MESSAGE_FIRST:
		case KEYS_FIRST:
			return (short) 0;
		default:
			return LowLevel.getUnsignedByte(data[OFFSET_PART_INDEX]);
		}
	}
}
