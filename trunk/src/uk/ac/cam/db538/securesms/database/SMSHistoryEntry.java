package uk.ac.cam.db538.securesms.database;

import android.text.format.Time;

public class SMSHistoryEntry {
	private long mIndex;
	private String mPhoneNumber;
	private String mMessageBody;
	private Time mCreationTime;
	
	public SMSHistoryEntry(long index, String phoneNumber, String messageBody, Time creationTime) {
		mIndex = index;
		mPhoneNumber = phoneNumber;
		mMessageBody = messageBody;
		mCreationTime = creationTime;
	}
	
	public long getIndex() {
		return mIndex;
	}

	public void setIndex(long index) {
		mIndex = index;
	}
	
	public String getPhoneNumber() {
		return mPhoneNumber;
	}
	
	public void setPhoneNumber(String phoneNumber) {
		mPhoneNumber = phoneNumber;
	}
	
	public String getMessageBody() {
		return mMessageBody;
	}
	
	public void setMessageBody(String messageBody) {
		mMessageBody = messageBody;
	}

	public Time getCreationTime() {
		return mCreationTime;
	}
	
	public void setCreationTime(Time creationTime) {
		mCreationTime = creationTime;
	}
}
