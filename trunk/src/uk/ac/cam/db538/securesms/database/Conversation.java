package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import android.text.format.Time;

/**
 * 
 * Class representing a conversation with a specific person
 * 
 * @author David Brazdil
 *
 */
public class Conversation implements Comparable<Conversation> {
	private long mIndexEntry; // READ ONLY
	private String mPhoneNumber;
	private Time mTimeStamp;
	
	Conversation(long indexEntry) {
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
			Database.getSingleton().updateConversation(this, lock);
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
			Database.getSingleton().saveConversation(this, false);
	}

	public void setPhoneNumber(String phoneNumber) {
		this.mPhoneNumber = phoneNumber;
	}

	public String getPhoneNumber() {
		return mPhoneNumber;
	}

	public void setTimeStamp(Time timeStamp) {
		this.mTimeStamp = timeStamp;
	}

	public Time getTimeStamp() {
		return mTimeStamp;
	}
	
	/**
	 * Assigns new random session keys to this conversation. Last IDs are set to zero.
	 * @param simPhoneNumber	Phone number of this SIM card.
	 * @return					Instance of SessionKyes
	 * @throws IOException 
	 * @throws DatabaseFileException 
	 */
	public SessionKeys assignSessionKeys(String simPhoneNumber) throws DatabaseFileException, IOException {
		return Database.getSingleton().createSessionKeys(this, simPhoneNumber);
	}

	/**
	 * Assigns new session keys to this conversation
	 * @param keysSent			Whether the keys have already been sent across
	 * @param keysConfirmed		Whether the other party has confirmed the keys
	 * @param simPhoneNumber	Phone number of the current SIM card		
	 * @param sessionKey_Out	Session key for sending
	 * @param lastID_Out		Last ID of sent SMS
	 * @param sessionKey_In		Session key for receiving
	 * @param lastID_In			Last ID of received SMS
	 * @return					Instance of SessionKeys
	 * @throws IOException 
	 * @throws DatabaseFileException 
	 */
	public SessionKeys assignSessionKeys(boolean keysSent, boolean keysConfirmed, String simPhoneNumber, byte[] sessionKey_Out, byte lastID_Out, byte[] sessionKey_In, byte lastID_In) throws DatabaseFileException, IOException {
		return Database.getSingleton().createSessionKeys(this, keysSent, keysConfirmed, simPhoneNumber, sessionKey_Out, lastID_Out, sessionKey_In, lastID_In);
	}
	
	@Override
	public int compareTo(Conversation another) {
		return Time.compare(this.getTimeStamp(), another.getTimeStamp());
	}
	
	
}
