package uk.ac.cam.db538.securesms.receivers;

import android.text.format.Time;

public class Pending {
	private String mSender;
	private Time mTimeStamp;
	private byte[] mData;
	
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
}
