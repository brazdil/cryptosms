package uk.ac.cam.db538.cryptosms.utils;

public class SimNumber {
	private boolean mSerial;
	private String mNumber;
	
	public SimNumber() {
		setNumber("");
		setSerial(false);
	}
	
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
	
	@Override
	public String toString() {
		return mNumber;
	}
}