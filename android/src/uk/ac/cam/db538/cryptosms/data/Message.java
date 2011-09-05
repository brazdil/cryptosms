package uk.ac.cam.db538.cryptosms.data;

import java.nio.ByteBuffer;
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
import uk.ac.cam.db538.cryptosms.data.PendingParser.ParseResult;
import uk.ac.cam.db538.cryptosms.data.PendingParser.PendingParseResult;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public abstract class Message {
	// same for all messages
	protected static final int LENGTH_HEADER = 1;
	protected static final int OFFSET_HEADER = 0;
	protected static final int LENGTH_ID = 1;
	protected static final int OFFSET_ID = OFFSET_HEADER + LENGTH_HEADER;
	protected static final int LENGTH_INDEX = 1;
	protected static final int OFFSET_INDEX = OFFSET_ID + LENGTH_ID;;
	protected static final int OFFSET_DATA = OFFSET_INDEX + LENGTH_INDEX;
	protected static final int LENGTH_DATA = MessageData.LENGTH_MESSAGE - OFFSET_DATA;

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

    public abstract byte[] getBytes() throws StorageFileException, MessageException, EncryptionException;
    public abstract byte getHeader();
    public abstract byte getId();
    public abstract int getMessagePartCount();
    
	/**
	 * Takes the byte arrays created by getBytes() method and sends 
	 * them to the given phone number
	 */
	public void sendSMS(final String phoneNumber, Context context, final MessageSendingListener listener)
			throws StorageFileException, MessageException, EncryptionException {
		byte[] dataMessage = getBytes();
		final ArrayList<byte[]> dataSms = new ArrayList<byte[]>(1);
		
		// align to fit message parts exactly
		int totalBytes = LENGTH_DATA * LowLevel.roundUpDivision(dataMessage.length, LENGTH_DATA);
		dataMessage = LowLevel.wrapData(dataMessage, totalBytes);
		
		// seperate into message parts
		ByteBuffer buf;
		int index = 0, offset = 0;
		byte header = getHeader(), id = getId();
		try {
			while (true) {
				buf = ByteBuffer.allocate(MessageData.LENGTH_MESSAGE);
				buf.put(header);
				buf.put(id);
				buf.put(LowLevel.getBytesUnsignedByte(index++));
				buf.put(LowLevel.cutData(dataMessage, offset, LENGTH_DATA));
				offset += LENGTH_DATA;
				dataSms.add(buf.array());
			}
		} catch (IndexOutOfBoundsException e) {
			// end
		}
		
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
	    	
	    	// send the data
	    	SmsManager.getDefault().sendDataMessage(phoneNumber, null, MyApplication.getSmsPort(), dataSms.get(i), sentPI, null);
		}
	}
	
	public static class JoiningException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 456081152855672327L;
		
		private PendingParseResult mReason;
		
		public JoiningException(PendingParseResult reason) {
			mReason = reason;
		}
		
		public PendingParseResult getReason() {
			return mReason;
		}
		
	}
	
	protected static byte[] joinParts(ArrayList<Pending> idGroup, int expectedGroupSize) throws JoiningException {
		// check we have all the parts
		// there shouldn't be more than 1
		int groupSize = idGroup.size();
		if (groupSize < expectedGroupSize || groupSize <= 0)
			throw new JoiningException(PendingParseResult.MISSING_PARTS);
		else if (groupSize > expectedGroupSize)
			throw new JoiningException(PendingParseResult.REDUNDANT_PARTS);
		
		// get the data
		byte[][] dataParts = new byte[groupSize][];
		int filledParts = 0;
		for (Pending p : idGroup) {
			byte[] dataPart = p.getData();
			int index = getMessageIndex(dataPart);
			if (index >= 0 && index < idGroup.size()) {
				// index is fine, check that there wasn't the same one already
				if (dataParts[index] == null) {
					// first time we stumbled upon this index
					// store the message part data in the array
					dataParts[index] = dataPart;
					filledParts++;
				} else
					// more parts of the same index
					throw new JoiningException(PendingParseResult.REDUNDANT_PARTS);

			} else
				// index is bigger than the number of messages in ID group
				// therefore some parts have to be missing or the data is corrupted
				throw new JoiningException(PendingParseResult.MISSING_PARTS);

		}
		// the array was filled with data, so check that there aren't any missing
		if (filledParts != expectedGroupSize)
			throw new JoiningException(PendingParseResult.MISSING_PARTS);

		
		// lets put the data together
		byte[] dataJoined = new byte[expectedGroupSize * LENGTH_DATA];
		for (int i = 0; i < expectedGroupSize; ++i) {
			try {
				// get the data 
				// it can't be too long, thanks to getMessageData
				// but it can be too short (throws IndexOutOfBounds exception
				byte[] relevantData = getMessageData(dataParts[i]);
				System.arraycopy(relevantData, 0, dataJoined, getDataPartOffset(i), Message.LENGTH_DATA);
			} catch (RuntimeException e) {
				throw new JoiningException(PendingParseResult.CORRUPTED_DATA);
			}
		}
		
		return dataJoined;
	}
	
	protected static byte getMessageHeader(byte[] data) {
		return data[OFFSET_HEADER];
	}
    
	protected static byte getMessageIdByte(byte[] data) {
		return data[OFFSET_ID];
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

	/**
	 * Returns message ID for both first and following parts of text messages
	 * @param data
	 * @return
	 */
	public static int getMessageId(byte[] data) {
		return LowLevel.getUnsignedByte(getMessageIdByte(data));
	}
	
	/**
	 * Expects encrypted data of both first and non-first part of text message 
	 * and returns its index
	 * @param data
	 * @return
	 */
	public static int getMessageIndex(byte[] data) {
		return LowLevel.getUnsignedByte(data[OFFSET_INDEX]);
	}
	
	/**
	 * Returns stored encrypted data for both first and following parts of text messages
	 * @param data
	 * @return
	 */
	public static byte[] getMessageData(byte[] data) {
		return LowLevel.cutData(data, OFFSET_DATA, LENGTH_DATA);
	}
	
	/**
	 * Returns the offset of relevant data expected in given message part
	 * @param dataLength
	 * @param index
	 * @return
	 */
	public static int getDataPartOffset(int index) {
		return index * LENGTH_DATA;
	}
}
