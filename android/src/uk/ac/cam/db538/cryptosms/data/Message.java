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
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;

public abstract class Message {
	// same for all messages
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
	
	public static interface MessageSendingListener {
		public void onMessageSent();
		public void onPartSent(int index);
		public void onError(String message);
	}
	
	protected static final byte HEADER_HANDSHAKE = (byte) 0x00;
	protected static final byte HEADER_CONFIRM = (byte) 0x10;
	protected static final byte HEADER_TEXT = (byte) 0x20;
	
	public static enum MessageType {
		HANDSHAKE,
		CONFIRM,
		TEXT,
		UNKNOWN,
		NONE
	}
	
    private static final String SENT_SMS_ACTION = "CRYPTOSMS_SMS_SENT"; 
    private static long mMessageCounter = 0;

    public abstract ArrayList<byte[]> getBytes() throws StorageFileException, MessageException, EncryptionException;
    public abstract byte getHeader();
    public abstract int getMessagePartCount();
    
	/**
	 * Takes the byte arrays created by getBytes() method and sends 
	 * them to the given phone number
	 */
	public void sendSMS(final String phoneNumber, Context context, final MessageSendingListener listener)
			throws StorageFileException, MessageException, EncryptionException {
		final ArrayList<byte[]> dataSms = getBytes();
		
//		// align to fit message parts exactly
//		int totalBytes = LENGTH_DATA * LowLevel.roundUpDivision(dataMessage.length, LENGTH_DATA);
//		dataMessage = LowLevel.wrapData(dataMessage, totalBytes);
//		
//		// seperate into message parts
//		ByteBuffer buf;
//		int index = 0, offset = 0;
//		byte header = getHeader(), id = getId();
//		try {
//			while (true) {
//				buf = ByteBuffer.allocate(MessageData.LENGTH_MESSAGE);
//				buf.put(header);
//				buf.put(id);
//				buf.put(LowLevel.getBytesUnsignedByte(index++));
//				buf.put(LowLevel.cutData(dataMessage, offset, LENGTH_DATA));
//				offset += LENGTH_DATA;
//				dataSms.add(buf.array());
//			}
//		} catch (IndexOutOfBoundsException e) {
//			// end
//		}
		
		// send
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
	    	
	    	byte[] dataPart = new byte[MessageData.LENGTH_MESSAGE];
	    	dataPart[OFFSET_HEADER] = getHeader();
	    	int offset = OFFSET_HEADER + LENGTH_HEADER;
	    	int lenData = Math.min(dataSms.get(i).length, MessageData.LENGTH_MESSAGE - offset);
	    	int lenRandom = MessageData.LENGTH_MESSAGE - lenData - offset;
	    	System.arraycopy(dataSms.get(i), 0, dataPart, offset, lenData);
	    	System.arraycopy(Encryption.getEncryption().generateRandomData(lenRandom), 0, dataPart, offset + lenData, lenRandom);
	    	
	    	// send the data
	    	SmsManager.getDefault().sendDataMessage(phoneNumber, null, MyApplication.getSmsPort(), dataPart, sentPI, null);
		}
	}
	
	protected static byte getMessageHeader(byte[] data) {
		return data[OFFSET_HEADER];
	}
    
	public static MessageType getMessageType(byte[] data) {
    	switch (getMessageHeader(data)) {
    	case HEADER_HANDSHAKE:
    		return MessageType.HANDSHAKE;
    	case HEADER_CONFIRM:
    		return MessageType.CONFIRM;
    	case HEADER_TEXT:
    		return MessageType.TEXT;
    	default:
    		return MessageType.UNKNOWN;
    	}
    }
}
