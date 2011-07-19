package uk.ac.cam.db538.securesms.database;

import java.io.IOException;

public class MessagePart {
	public enum MessageType {
		INCOMING,
		OUTGOING
	}

	private long mIndexEntry; // READ ONLY
	private boolean mDeliveredPart;
	private String mMessageBody;
	
	MessagePart(long indexEntry) {
		if (indexEntry > 0xFFFFFFFFL || indexEntry <= 0L)
			throw new IndexOutOfBoundsException();
		
		mIndexEntry = indexEntry;
	}

	long getIndexEntry() {
		return mIndexEntry;
	}
	
	/**
	 * Loads data from the secure file (locks the file)
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void update() throws DatabaseFileException, IOException {
		update(true);
	}
	
	/**
	 * Loads data from the secure file
	 * @param lock		Specifies whether the file should be locked during the operation.
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void update(boolean lock) throws DatabaseFileException, IOException {
		if (mIndexEntry != 0)
			Database.getSingleton().updateMessagePart(this, lock);
	}
	
	/**
	 * Saves data to the secure file (locks the file)
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void save() throws DatabaseFileException, IOException {
		save(true);
	}
	
	/**
	 * Saves data to the secure file
	 * @param lock		Specifies whether the file should be locked during the operation.
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public void save(boolean lock) throws DatabaseFileException, IOException {
		if (mIndexEntry != 0)
			Database.getSingleton().saveMessagePart(this, false);
	}

	public void setDeliveredPart(boolean deliveredPart) {
		this.mDeliveredPart = deliveredPart;
	}

	public boolean getDeliveredPart() {
		return mDeliveredPart;
	}

	public void setMessageBody(String messageBody) {
		this.mMessageBody = messageBody;
	}

	public String getMessageBody() {
		return mMessageBody;
	}
}
