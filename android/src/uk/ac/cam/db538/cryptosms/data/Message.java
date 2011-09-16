/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
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

/*
 * Base class for all text messages
 */
public abstract class Message {
	// same for all messages
	protected static final int LENGTH_HEADER = 1;
	protected static final int OFFSET_HEADER = 0;

	public static class MessageException extends Exception {
		private static final long serialVersionUID = 4922446456153260918L;
		
		/**
		 * Instantiates a new message exception.
		 */
		public MessageException() {
			super();
		}

		/**
		 * Instantiates a new message exception.
		 *
		 * @param message the message
		 */
		public MessageException(String message) {
			super(message);
		}
	}
	
	public static interface MessageSendingListener {
		
		/**
		 * On all parts sent.
		 */
		public void onMessageSent();
		
		/**
		 * On single part sent.
		 *
		 * @param index index of the part 
		 */
		public void onPartSent(int index);
		
		/**
		 * On error.
		 *
		 * @param ex the exception
		 */
		public void onError(Exception ex);
	}
	
	// USE ONLY THE TOP 2 BITS!!!
	protected static final byte HEADER_TEXT_FIRST = (byte) 0x00;      // 00000000
	protected static final byte HEADER_TEXT_OTHER = (byte) 0x80;      // 10000000 
	protected static final byte HEADER_HANDSHAKE = (byte) 0x40; // 01000000
	protected static final byte HEADER_CONFIRM = (byte) 0xC0;   // 11000000
	
	public static enum MessageType {
		HANDSHAKE,
		CONFIRM,
		TEXT,
		UNKNOWN,
		NONE
	}
	
    private static final String SENT_SMS_ACTION = "CRYPTOSMS_SMS_SENT"; 
    private static long mMessageCounter = 0;

    protected abstract ArrayList<byte[]> getBytes() throws StorageFileException, MessageException, EncryptionException;
    protected abstract void onMessageSent(String phoneNumber) throws StorageFileException;
    protected abstract void onPartSent(String phoneNumber, int index) throws StorageFileException;
    
	/**
	 * Takes the byte arrays created by getBytes() method and sends
	 * them to the given phone number.
	 *
	 * @param phoneNumber the phone number
	 * @param context the context
	 * @param listener the listener
	 * @throws StorageFileException the storage file exception
	 * @throws MessageException the message exception
	 * @throws EncryptionException the encryption exception
	 */
	public void sendSMS(final String phoneNumber, Context context, final MessageSendingListener listener)
			throws StorageFileException, MessageException, EncryptionException {
		final ArrayList<byte[]> dataSms = getBytes();

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
						// notify and save
						deliveryConfirms[intentIndex] = true;
						try {
							onPartSent(phoneNumber, intentIndex);
							listener.onPartSent(intentIndex);
						} catch (StorageFileException e) {
							listener.onError(e);
						}
						// check we have all
						boolean all = true;
						for (boolean b : deliveryConfirms)
							all = all && b;
						if (all) {
							try {
								Message.this.onMessageSent(phoneNumber);
								listener.onMessageSent();
							} catch (StorageFileException e) {
								listener.onError(e);
							}
						}
						break;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
						listener.onError(new Exception(res.getString(R.string.error_sending_generic)));
						break;
					case SmsManager.RESULT_ERROR_NO_SERVICE:
						listener.onError(new Exception(res.getString(R.string.error_sending_no_service)));
						break;
					case SmsManager.RESULT_ERROR_NULL_PDU:
						listener.onError(new Exception(res.getString(R.string.error_sending_null_pdu)));
						break;
					case SmsManager.RESULT_ERROR_RADIO_OFF:
						listener.onError(new Exception(res.getString(R.string.error_sending_radio_off)));
						break;
					default: // ERROR
						listener.onError(new Exception(res.getString(R.string.error_sending_unknown)));
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
	    	int lenData = Math.min(dataSms.get(i).length, MessageData.LENGTH_MESSAGE);
	    	int lenRandom = MessageData.LENGTH_MESSAGE - lenData;
	    	System.arraycopy(dataSms.get(i), 0, dataPart, 0, lenData);
	    	System.arraycopy(Encryption.getEncryption().generateRandomData(lenRandom), 0, dataPart, lenData, lenRandom);
	    	
	    	// send the data
	    	SmsManager.getDefault().sendDataMessage(phoneNumber, null, MyApplication.getSmsPort(), dataPart, sentPI, null);
		}
	}
	
	protected static byte getMessageHeader(byte[] data) {
		return (byte) (data[OFFSET_HEADER] & 0xC0);
	}
    
	/**
	 * Returns the message type from given data
	 *
	 * @param data the data
	 * @return the message type
	 */
	public static MessageType getMessageType(byte[] data) {
    	switch (getMessageHeader(data)) {
    	case HEADER_HANDSHAKE:
    		return MessageType.HANDSHAKE;
    	case HEADER_CONFIRM:
    		return MessageType.CONFIRM;
    	case HEADER_TEXT_FIRST:
    	case HEADER_TEXT_OTHER:
    		return MessageType.TEXT;
    	default:
    		return MessageType.UNKNOWN;
    	}
    }
}
