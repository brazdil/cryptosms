package uk.ac.cam.db538.cryptosms.data;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageType;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import android.content.Context;
import android.text.format.Time;
import android.util.Log;

public class Pending {
	
	public static class ProcessingException extends Exception {
		private static final long serialVersionUID = 2227666669460561421L;

		public ProcessingException(String message) {
			super(message);
		}
	}
	
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
	
	private static void processMessage(ArrayList<Pending> listParts, int totalBytes, SessionKeys keys) throws ProcessingException {
		int partCount = listParts.size();
		int totalDataLength = TextMessage.LENGTH_FIRST_MESSAGEBODY + (partCount - 1) * TextMessage.LENGTH_PART_MESSAGEBODY;
		
		// get all the data in one byte array
		byte[] dataEncrypted = new byte[TextMessage.LENGTH_FIRST_ENCRYPTION + totalDataLength];
		for (Pending p : listParts) {
			Log.d(MyApplication.APP_TAG, "SMS data: " + LowLevel.toHex(p.getData()));
			int index = TextMessage.getMessageIndex(p.getData());
			if (index >= 0 && index < partCount) {
				System.arraycopy(TextMessage.getMessageData(p.getData()), 0, 
				                 dataEncrypted, TextMessage.LENGTH_FIRST_ENCRYPTION + TextMessage.getExpectedDataOffset(totalDataLength, index), 
				                 TextMessage.getExpectedDataLength(totalDataLength, index));
				if (index == 0)
					System.arraycopy(TextMessage.getMessageEncryptionData(p.getData()), 0, 
			                 dataEncrypted, 0, 
			                 TextMessage.LENGTH_FIRST_ENCRYPTION);
			} else {
				throw new ProcessingException("Index of message part out of expected bounds");
			}
		}
		dataEncrypted = LowLevel.cutData(dataEncrypted, 0, dataEncrypted.length - (dataEncrypted.length % Encryption.AES_BLOCK_LENGTH));
		Log.d(MyApplication.APP_TAG, "Encrypted data: " + LowLevel.toHex(dataEncrypted));
		Log.d(MyApplication.APP_TAG, "Encrypted data length: " + dataEncrypted.length);
		Log.d(MyApplication.APP_TAG, "Decryption key: " + LowLevel.toHex(keys.getSessionKey_In()));
		
		// decrypt it
		byte[] dataDecrypted = null;
		try {
			dataDecrypted = Encryption.getEncryption().decryptSymmetric(dataEncrypted, keys.getSessionKey_In());
			Log.d(MyApplication.APP_TAG, "Decrypted data: " + LowLevel.toHex(dataDecrypted));
		} catch (EncryptionException e) {
			Log.d(MyApplication.APP_TAG, "Decrypted data: " + e.getMessage());
			throw new ProcessingException("Bad decryption key"); 
		}
		
		if (dataDecrypted != null) {
			// take only the relevant part
			byte[] dataPlain = LowLevel.cutData(dataDecrypted, 0, totalBytes);
			Log.d(MyApplication.APP_TAG, "Plain data: " + LowLevel.toHex(dataPlain));
		} else {
			throw new ProcessingException("Couldn't decrypt");
		}
	}

	/**
	 * check whether we can put together any complete messages
	 * from the user and potentially show notifications
	 * @param phoneNumber
	 * @param context
	 * @param database
	 * @throws ProcessingException 
	 */
	public static void processPending(Context context) throws ProcessingException {
		DbPendingAdapter database = new DbPendingAdapter(context);
		database.open();
		ArrayList<Pending> listPending = database.getAllEntries();
		database.close();
		
		boolean found;
		do {
			// let's look for messages of type MESSAGE_FIRST
			found = false;
			Pending pendingFirst = null;
			for (Pending p : listPending)
				if (p.getMessageType() == MessageType.MESSAGE_FIRST) {
					pendingFirst = p;
					break;
				}
			// have we found one?
			if (pendingFirst != null) {
				found = true;
				listPending.remove(pendingFirst);
				
				// do we have Session Keys for this person?
				SessionKeys keys = null;
				try {
					keys = StorageUtils.getSessionKeysForSIM(Conversation.getConversation(pendingFirst.getSender()), context);
				} catch (StorageFileException e1) {
				} catch (IOException e1) {
				}
				
				if (keys != null) {
					int ID = TextMessage.getMessageID(pendingFirst.getData());
					int totalBytes = TextMessage.getMessageDataLength(pendingFirst.getData());
					int partCount = -1;
					try {
						partCount = TextMessage.computeNumberOfMessageParts(totalBytes);
					} catch (MessageException e) {
						throw new ProcessingException("Invalid data length");
					}
					if (partCount > 0) {
						// look for other parts
						ArrayList<Pending> listParts = new ArrayList<Pending>();
						boolean[] foundParts = new boolean[partCount];
						int index;
						listParts.add(pendingFirst);
						foundParts[0] = true;
						for (Pending p : listPending)
							if (p.getMessageType() == MessageType.MESSAGE_PART &&
								TextMessage.getMessageID(p.getData()) == ID &&
								(index = TextMessage.getMessageIndex(p.getData())) < partCount) {
								if (foundParts[index]) {
									throw new ProcessingException("Multiple parts with the same index");
								} else  {
									foundParts[index] = true;
									listParts.add(p);
								}
							}
						// have we found them all?
						boolean foundAll = true;
						for (boolean b: foundParts)
							foundAll = foundAll && b;
						if (foundAll && listParts.size() == partCount)
							processMessage(listParts, totalBytes, keys);
						// else missing parts, but those still might arrive
					} else {
						throw new ProcessingException("Invalid data length");
					}
				} else {
					throw new ProcessingException("No keys found");
				}
			}
					
		} while (found);
	}
	
	/**
	 * Remove messages that have been here for too long
	 * @param database
	 */
	public static void clearPending(DbPendingAdapter database) {
		// TODO: implement
	}
}
