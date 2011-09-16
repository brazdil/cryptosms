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

import org.joda.time.DateTime;

import uk.ac.cam.db538.cryptosms.data.Message.MessageType;

/*
 * Represents a pending message in the database
 */
public class Pending {
	
	public static class ProcessingException extends Exception {
		private static final long serialVersionUID = 2227666669460561421L;

		/**
		 * Instantiates a new processing exception.
		 *
		 * @param message the message
		 */
		public ProcessingException(String message) {
			super(message);
		}
	}
	
	private String mSender;
	private DateTime mTimeStamp;
	private byte[] mData;
	private long mRowIndex;
	
	/**
	 * Instantiates a new pending.
	 *
	 * @param sender the sender
	 * @param data the data
	 */
	public Pending(String sender, byte[] data) {
		this(sender, new DateTime(System.currentTimeMillis()), data);
	}
	
	/**
	 * Instantiates a new pending.
	 *
	 * @param sender the sender
	 * @param timeStamp the time stamp
	 * @param data the data
	 */
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
}
