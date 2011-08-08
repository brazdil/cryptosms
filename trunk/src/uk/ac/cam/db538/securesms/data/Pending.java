package uk.ac.cam.db538.securesms.data;

import java.util.ArrayList;

import uk.ac.cam.db538.securesms.Encryption;
import uk.ac.cam.db538.securesms.data.Message.MessageException;
import uk.ac.cam.db538.securesms.data.Message.MessageType;
import android.content.Context;
import android.text.format.Time;

public class Pending {
	private String mSender;
	private Time mTimeStamp;
	private byte[] mData;
	private long mRowIndex;
	
	public Pending(String sender, byte[] data) {
		mTimeStamp = new Time();
		mTimeStamp.setToNow();
		mSender = sender;
		mData = data;
	}
	
	public Pending(String sender, Time timeStamp, byte[] data) {
		mSender = sender;
		mTimeStamp = timeStamp;
		mData = data;
	}
	
	public MessageType getMessageType() {
		return Message.getMessageType(mData);
	}
	
	public String getSender() {
		return mSender;
	}
	
	public void setSender(String sender) {
		this.mSender = sender;
	}
	
	public Time getTimeStamp() {
		return mTimeStamp;
	}
	
	public void setTimeStamp(Time timeStamp) {
		this.mTimeStamp = timeStamp;
	}
	
	public byte[] getData() {
		return mData;
	}
	
	public void setData(byte[] data) {
		this.mData = data;
	}

	public void setRowIndex(long rowIndex) {
		this.mRowIndex = rowIndex;
	}

	public long getRowIndex() {
		return mRowIndex;
	}
	
	// STATIC STUFF

	/**
	 * check whether we can put together any complete messages
	 * from the user and potentially show notifications
	 * @param phoneNumber
	 * @param context
	 * @param database
	 */
	public static void checkPending(String phoneNumber, Context context, DbPendingAdapter database) {
		ArrayList<Pending> pendingList =  database.getEntry(phoneNumber);
		for (Pending pending : pendingList) {
			if (pending.getMessageType() == MessageType.MESSAGE_FIRST) {
				int msgID = TextMessage.getMessageID(pending.getData());
				int dataLength = TextMessage.getMessageDataLength(pending.getData());
				int textsCount = 1;
				try {
					textsCount = TextMessage.computeNumberOfMessageParts(dataLength);
				} catch (MessageException e) {
				}
				// init
				int textsFound = 1;
				int totalDataLength = dataLength + Encryption.ENCRYPTION_OVERHEAD;
				int index;
				byte[] dataEncrypted = new byte[totalDataLength];
				
				// get all message parts with the same ID
				// copy the first one over
				System.arraycopy(TextMessage.getMessageEncryptionData(pending.getData()), 0, 
                                 dataEncrypted, 0, 
                                 Encryption.ENCRYPTION_OVERHEAD);
				System.arraycopy(TextMessage.getMessageData(pending.getData()), 0, 
		                         dataEncrypted, Encryption.ENCRYPTION_OVERHEAD + TextMessage.getExpectedDataOffset(dataLength, 0), 
		                         TextMessage.getExpectedDataLength(dataLength, 0));
				// find others
				for (Pending part : pendingList) {
					if (part.getMessageType() == MessageType.MESSAGE_PART &&
						TextMessage.getMessageID(part.getData()) == msgID) {
						++textsFound;
						index = TextMessage.getMessageIndex(part.getData());
						System.arraycopy(TextMessage.getMessageData(part.getData()), 0, 
						                 dataEncrypted, Encryption.ENCRYPTION_OVERHEAD + TextMessage.getExpectedDataOffset(dataLength, index), 
						                 TextMessage.getExpectedDataLength(dataLength, index));
					}
				}
				
				// have we got all ?
				if (textsFound == textsCount) {
					
				}
			}
		}
	}
	
	/**
	 * Remove messages that have been here for too long
	 * @param database
	 */
	public static void clearPending(DbPendingAdapter database) {
		// TODO: implement
	}
}
