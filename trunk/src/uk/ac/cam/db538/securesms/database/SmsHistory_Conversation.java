package uk.ac.cam.db538.securesms.database;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.text.format.Time;

import uk.ac.cam.db538.securesms.encryption.Encryption;

public class SmsHistory_Conversation {
	
	private static final String CHARSET_LATIN = "ISO-8859-1";
	
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_PHONENUMBER = 32;
	private static final int LENGTH_TIMESTAMP = 29;
	private static final int LENGTH_SESSIONKEY = 32;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_PHONENUMBER = OFFSET_FLAGS + LENGTH_FLAGS;
	private static final int OFFSET_TIMESTAMP = OFFSET_PHONENUMBER + LENGTH_PHONENUMBER;
	private static final int OFFSET_SESSIONKEY_OUTGOING = OFFSET_TIMESTAMP + LENGTH_TIMESTAMP;
	private static final int OFFSET_SESSIONKEY_INCOMING = OFFSET_SESSIONKEY_OUTGOING + LENGTH_SESSIONKEY;

	private static final int OFFSET_RANDOMDATA = OFFSET_SESSIONKEY_INCOMING + LENGTH_SESSIONKEY;

	private static final int OFFSET_NEXTINDEX = SmsHistory.ENCRYPTED_ENTRY_SIZE - 4;
	private static final int OFFSET_PREVINDEX = OFFSET_NEXTINDEX - 4;
	private static final int OFFSET_MSGSINDEX = OFFSET_PREVINDEX - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_RANDOMDATA - OFFSET_MSGSINDEX;	

	private boolean mKeysExchanged;
	private String mPhoneNumber;
	private Time mTimeStamp;
	private byte[] mSessionKey_Out;
	private byte[] mSessionKey_In;
	private long mIndexMessages;
	private long mIndexPrev;
	private long mIndexNext;
	
	SmsHistory_Conversation(boolean keysExchanged, String phoneNumber, Time timeStamp, byte[] sessionKey_Out, byte[] sessionKey_In, long indexMessages, long indexPrev, long indexNext) {
		mKeysExchanged = keysExchanged;
		mPhoneNumber = phoneNumber;
		mTimeStamp = timeStamp;
		mSessionKey_Out = sessionKey_Out;
		mSessionKey_In = sessionKey_In;
		mIndexMessages = indexMessages;
		mIndexPrev = indexPrev;
		mIndexNext = indexNext;
	}
	
	public boolean getKeysExchanged() {
		return mKeysExchanged;
	}

	public void setKeysExchanged(boolean keysExchanged) {
		mKeysExchanged = keysExchanged;
	}

	public String getPhoneNumber() {
		return mPhoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.mPhoneNumber = phoneNumber;
	}

	public Time getTimeStamp() {
		return mTimeStamp;
	}

	public void setTimeStamp(Time timeStamp) {
		this.mTimeStamp = timeStamp;
	}

	public byte[] getSessionKey_Out() {
		return mSessionKey_Out;
	}

	public void setSessionKey_Out(byte[] sessionKeyOut) {
		mSessionKey_Out = sessionKeyOut;
	}

	public byte[] getSessionKey_In() {
		return mSessionKey_In;
	}

	public void setSessionKey_In(byte[] sessionKeyIn) {
		mSessionKey_In = sessionKeyIn;
	}

	long getIndexMessages() {
		return mIndexMessages;
	}

	void setIndexMessages(long indexMessages) {
		this.mIndexMessages = indexMessages;
	}

	long getIndexPrev() {
		return mIndexPrev;
	}

	void setIndexPrev(long indexPrev) {
		this.mIndexPrev = indexPrev;
	}

	long getIndexNext() {
		return mIndexNext;
	}

	void setIndexNext(long indexNext) {
		this.mIndexNext = indexNext;
	}

	static byte[] createData(SmsHistory_Conversation conversation) throws HistoryFileException {
		try {
			byte[] temp;
			
			ByteBuffer convBuffer = ByteBuffer.allocate(SmsHistory.ENCRYPTED_ENTRY_SIZE);
			
			// flags
			byte flags = 0;
			if (conversation.mKeysExchanged)
				flags |= (byte) ((1 << 7) & 0xFF);
			convBuffer.put(flags);
			
			// phone number
			temp = conversation.mPhoneNumber.getBytes(CHARSET_LATIN);
			byte[] bytePhoneNumber = new byte[LENGTH_PHONENUMBER];
			if (temp.length >= LENGTH_PHONENUMBER) {
				// copy only enough bytes to fit the space
				System.arraycopy(temp, 0, bytePhoneNumber, 0, LENGTH_PHONENUMBER);
			}
			else {
				// copy all the bytes and fill the rest with zeros
				Arrays.fill(bytePhoneNumber, (byte) 0x00);
				System.arraycopy(temp, 0, bytePhoneNumber, 0, temp.length);
			}
			convBuffer.put(bytePhoneNumber);
			
			// time stamp
			temp = conversation.mTimeStamp.format3339(false).getBytes(CHARSET_LATIN);
			byte[] byteTimeStamp = new byte[LENGTH_TIMESTAMP];
			if (temp.length >= LENGTH_TIMESTAMP) {
				// copy only enough bytes to fit the space (should never exceed!!!)
				System.arraycopy(temp, 0, byteTimeStamp, 0, LENGTH_PHONENUMBER);
			}
			else {
				// copy all the bytes and fill the rest with zeros
				Arrays.fill(byteTimeStamp, (byte) 0x00);
				System.arraycopy(temp, 0, byteTimeStamp, 0, temp.length);
			}
			convBuffer.put(byteTimeStamp);
	
			// session keys
			convBuffer.put(conversation.mSessionKey_Out);
			convBuffer.put(conversation.mSessionKey_In);
			
			// random data
			convBuffer.put(Encryption.getRandomData(LENGTH_RANDOMDATA));
			
			// indices
			convBuffer.put(SmsHistory.getBytes(conversation.mIndexMessages)); 
			convBuffer.put(SmsHistory.getBytes(conversation.mIndexPrev));
			convBuffer.put(SmsHistory.getBytes(conversation.mIndexNext));
			
			return Encryption.encodeWithPassphrase(convBuffer.array());
		} catch (UnsupportedEncodingException ex) {
			throw new HistoryFileException("Error in phone number or time stamp charset");
		}
	}
	
	static SmsHistory_Conversation parseData(byte[] dataEncrypted) throws HistoryFileException {
		try {
			byte[] dataPlain = Encryption.decodeWithPassphrase(dataEncrypted);
			
			byte flags = dataPlain[OFFSET_FLAGS];
			boolean keysExchanged = ((flags & (1 << 7)) == 0) ? false : true;
			byte[] dataPhoneNumber = new byte[LENGTH_PHONENUMBER];
			System.arraycopy(dataPlain, OFFSET_PHONENUMBER, dataPhoneNumber, 0, LENGTH_PHONENUMBER);
			byte[] dataTimeStamp = new byte[LENGTH_TIMESTAMP];
			System.arraycopy(dataPlain, OFFSET_TIMESTAMP, dataTimeStamp, 0, LENGTH_TIMESTAMP);
			byte[] dataSessionKey_Out = new byte[LENGTH_SESSIONKEY];
			System.arraycopy(dataPlain, OFFSET_SESSIONKEY_OUTGOING, dataSessionKey_Out, 0, LENGTH_SESSIONKEY);
			byte[] dataSessionKey_In = new byte[LENGTH_SESSIONKEY];
			System.arraycopy(dataPlain, OFFSET_SESSIONKEY_INCOMING, dataSessionKey_In, 0, LENGTH_SESSIONKEY);
			Time timeStamp = new Time();
			timeStamp.parse3339(new String(dataTimeStamp, CHARSET_LATIN));
	
			return new SmsHistory_Conversation(keysExchanged,
			                                  new String(dataPhoneNumber, CHARSET_LATIN), 
			                                  timeStamp, 
			                                  dataSessionKey_Out, 
			                                  dataSessionKey_In, 
			                                  SmsHistory.getInt(dataPlain, OFFSET_MSGSINDEX), 
			                                  SmsHistory.getInt(dataPlain, OFFSET_PREVINDEX),
			                                  SmsHistory.getInt(dataPlain, OFFSET_NEXTINDEX)
			                                  );
		} catch (UnsupportedEncodingException ex) {
			throw new HistoryFileException("Error in phone number or time stamp charset");
		}
	}
}
