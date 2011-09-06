package uk.ac.cam.db538.cryptosms.data;

import org.joda.time.DateTime;

import uk.ac.cam.db538.cryptosms.data.Message.MessageType;

public class Pending {
	
	public static class ProcessingException extends Exception {
		private static final long serialVersionUID = 2227666669460561421L;

		public ProcessingException(String message) {
			super(message);
		}
	}
	
	private String mSender;
	private DateTime mTimeStamp;
	private byte[] mData;
	private long mRowIndex;
	
	public Pending(String sender, byte[] data) {
		this(sender, new DateTime(System.currentTimeMillis()), data);
	}
	
	public Pending(String sender, DateTime timeStamp, byte[] data) {
		mTimeStamp = timeStamp;
		mSender = sender;
		mData = data;
	}
	
	public MessageType getType() {
		return Message.getMessageType(mData);
	}
	
	public String getSender() {
		return mSender;
	}
	
	public void setSender(String sender) {
		this.mSender = sender;
	}
	
	public DateTime getTimeStamp() {
		return mTimeStamp;
	}
	
	public void setTimeStamp(DateTime timeStamp) {
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
	
//	private static void processMessage(ArrayList<Pending> listParts, int totalBytes, SessionKeys keys) throws ProcessingException {
//		int partCount = listParts.size();
//		int totalDataLength = TextMessage.LENGTH_FIRST_MESSAGEBODY + (partCount - 1) * TextMessage.LENGTH_PART_MESSAGEBODY;
//		
//		// get all the data in one byte array
//		byte[] dataEncrypted = new byte[TextMessage.LENGTH_FIRST_ENCRYPTION + totalDataLength];
//		for (Pending p : listParts) {
//			Log.d(MyApplication.APP_TAG, "SMS data: " + LowLevel.toHex(p.getData()));
//			int index = TextMessage.getMessageIndex(p.getData());
//			if (index >= 0 && index < partCount) {
//				System.arraycopy(TextMessage.getMessageData(p.getData()), 0, 
//				                 dataEncrypted, TextMessage.LENGTH_FIRST_ENCRYPTION + TextMessage.getExpectedDataOffset(totalDataLength, index), 
//				                 TextMessage.getExpectedDataLength(totalDataLength, index));
//				if (index == 0)
//					System.arraycopy(TextMessage.getMessageEncryptionData(p.getData()), 0, 
//			                 dataEncrypted, 0, 
//			                 TextMessage.LENGTH_FIRST_ENCRYPTION);
//			} else {
//				throw new ProcessingException("Index of message part out of expected bounds");
//			}
//		}
//		dataEncrypted = LowLevel.cutData(dataEncrypted, 0, dataEncrypted.length - (dataEncrypted.length % Encryption.SYM_BLOCK_LENGTH));
//		Log.d(MyApplication.APP_TAG, "Encrypted data: " + LowLevel.toHex(dataEncrypted));
//		Log.d(MyApplication.APP_TAG, "Encrypted data length: " + dataEncrypted.length);
//		Log.d(MyApplication.APP_TAG, "Decryption key: " + LowLevel.toHex(keys.getSessionKey_In()));
//		
//		// decrypt it
//		byte[] dataDecrypted = null;
//		try {
//			dataDecrypted = Encryption.getEncryption().decryptSymmetric(dataEncrypted, keys.getSessionKey_In());
//			Log.d(MyApplication.APP_TAG, "Decrypted data: " + LowLevel.toHex(dataDecrypted));
//		} catch (EncryptionException e) {
//			Log.d(MyApplication.APP_TAG, "Decrypted data: " + e.getMessage());
//			throw new ProcessingException("Bad decryption key"); 
//		}
//		
//		if (dataDecrypted != null) {
//			// take only the relevant part
//			byte[] dataPlain = LowLevel.cutData(dataDecrypted, 0, totalBytes);
//			Log.d(MyApplication.APP_TAG, "Plain data: " + LowLevel.toHex(dataPlain));
//		} else {
//			throw new ProcessingException("Couldn't decrypt");
//		}
//	}
//
//	/**
//	 * check whether we can put together any complete messages
//	 * from the user and potentially show notifications
//	 * @param phoneNumber
//	 * @param context
//	 * @param database
//	 * @throws ProcessingException 
//	 * @throws StorageFileException 
//	 */
//	public static void processPending(Context context) throws ProcessingException, StorageFileException {
//		DbPendingAdapter database = new DbPendingAdapter(context);
//		database.open();
//		ArrayList<Pending> listPending = database.getAllEntries();
//		database.close();
//		
//		boolean found;
//		do {
//			// let's look for messages of type MESSAGE_FIRST
//			found = false;
//			Pending pendingFirst = null;
//			for (Pending p : listPending)
//				if (p.getMessageType() == MessageType.MESSAGE_FIRST) {
//					pendingFirst = p;
//					break;
//				}
//			// have we found one?
//			if (pendingFirst != null) {
//				found = true;
//				listPending.remove(pendingFirst);
//				
//				// do we have Session Keys for this person?
//				SessionKeys keys = StorageUtils.getSessionKeysForSim(Conversation.getConversation(pendingFirst.getSender()));
//				
//				if (keys != null) {
//					int ID = TextMessage.getMessageID(pendingFirst.getData());
//					int totalBytes = TextMessage.getMessageDataLength(pendingFirst.getData());
//					int partCount = -1;
//					try {
//						partCount = TextMessage.getPartsCount(totalBytes);
//					} catch (MessageException e) {
//						throw new ProcessingException("Invalid data length");
//					}
//					if (partCount > 0) {
//						// look for other parts
//						ArrayList<Pending> listParts = new ArrayList<Pending>();
//						boolean[] foundParts = new boolean[partCount];
//						int index;
//						listParts.add(pendingFirst);
//						foundParts[0] = true;
//						for (Pending p : listPending)
//							if (p.getMessageType() == MessageType.MESSAGE_PART &&
//								TextMessage.getMessageID(p.getData()) == ID &&
//								(index = TextMessage.getMessageIndex(p.getData())) < partCount) {
//								if (foundParts[index]) {
//									throw new ProcessingException("Multiple parts with the same index");
//								} else  {
//									foundParts[index] = true;
//									listParts.add(p);
//								}
//							}
//						// have we found them all?
//						boolean foundAll = true;
//						for (boolean b: foundParts)
//							foundAll = foundAll && b;
//						if (foundAll && listParts.size() == partCount)
//							processMessage(listParts, totalBytes, keys);
//						// else missing parts, but those still might arrive
//					} else {
//						throw new ProcessingException("Invalid data length");
//					}
//				} else {
//					throw new ProcessingException("No keys found");
//				}
//			}
//					
//		} while (found);
//	}
}
