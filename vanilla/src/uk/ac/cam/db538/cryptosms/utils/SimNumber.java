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
package uk.ac.cam.db538.cryptosms.utils;

/*
 * Class representing SIM's identification number 
 * (either its associated phone number or serial number)
 */
public class SimNumber {
	private boolean mSerial;
	private String mNumber;
	
	/**
	 * Instantiates a new sim number.
	 */
	public SimNumber() {
		setNumber("");
		setSerial(false);
	}
	
	/**
	 * Instantiates a new sim number.
	 *
	 * @param number the number
	 * @param serial the serial
	 */
	public SimNumber(String number, boolean serial) {
		setNumber(number);
		setSerial(serial);
	}
	
	public void setSerial(boolean serial) {
		this.mSerial = serial;
	}
	
	public boolean isSerial() {
		return mSerial;
	}
	
	public void setNumber(String number) {
		this.mNumber = number;
	}
	
	public String getNumber() {
		return mNumber;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		
		try {
			SimNumber another = (SimNumber) o;
			if (this.mSerial == another.mSerial) {
				if (this.mSerial)
					return (this.mNumber.compareTo(another.mNumber) == 0);
				else
					return PhoneNumber.compare(this.mNumber, another.mNumber);
			}
		} catch (Exception e) {
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return mNumber;
	}
}