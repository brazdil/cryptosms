package uk.ac.cam.db538.cryptosms.data;

import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.utils.SimNumber;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class SimCard {
	
	private static SimCard mSingleton;
	
	private Context mContext;
	
	private SimCard(Context context) {
		mContext = context;
		
		mContext.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				State.notifySimState();
			}
		}, new IntentFilter("android.intent.action.SERVICE_STATE"));
	}
	
	public static void init(Context context) {
		mSingleton = new SimCard(context);
	}

	public static SimCard getSingleton() {
		return mSingleton;
	}
	
	static void setSingleton(SimCard singleton) {
		mSingleton = singleton;
	}
	
	/**
	 * Returns whether the Airplane Mode is on/off
	 * @return
	 */
	public boolean getAirplaneMode() throws UnsupportedOperationException {
		try {
			return (Settings.System.getInt(
					mContext.getContentResolver(), 
					Settings.System.AIRPLANE_MODE_ON, 0) == 1);
		} catch (UnsupportedOperationException ex) {
			throw ex;
		} catch (Exception ex) {
			return true;
		}
	}
	
	/**
	 * Returns the phone number of currently active SIM
	 * @return
	 */
	private String getPhoneNumberString() {
		try {
			if (getAirplaneMode())
				return null;
			
			TelephonyManager tMgr =(TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
			return tMgr.getLine1Number(); //"+447879116797"
		} catch (UnsupportedOperationException ex) {
			// TESTING MODE
			return "+441234567890";
		}
	}
	
	/**
	 * Returns the phone number of currently active SIM
	 * wrapped in the SimNumber class
	 * @return
	 */
	public SimNumber getPhoneNumber() {
			String phoneNumber = getPhoneNumberString();
			if (phoneNumber == null)
				return null;
			else
				return new SimNumber(phoneNumber, false);
	}

	/**
	 * Returns the phone number of currently active SIM
	 * @return
	 */
	private String getSerialNumberString() {
		try {
			if (getAirplaneMode())
				return null;
			
			TelephonyManager tMgr =(TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
			return tMgr.getSimSerialNumber();
		} catch (UnsupportedOperationException ex) {
			// TESTING MODE
			return "12345678901234567890";
		}
	}

	/**
	 * Returns the serial number of currently active SIM
	 * wrapped in the SimNumber class
	 * @return
	 */
	public SimNumber getSerialNumber() {
		String serialNumber = getSerialNumberString();
		if (serialNumber == null)
			return null;
		else
			return new SimNumber(serialNumber, true);
	}
	
	/**
	 * Returns the best SIM number it can find (preferably phone number, serial number otherwise).
	 * Returns null if SIM is not available or the phone is in an airplane mode.
	 * @return
	 */
	public SimNumber getNumber() {
		SimNumber phone = getPhoneNumber();
		if (phone == null || phone.getNumber().length() == 0)
			return getSerialNumber();
		else
			return phone;
	}
	
	/**
	 * Calls getSimNumber() and returns TRUE if the result is not null. 
	 * @return
	 */
	public boolean isNumberAvailable() {
		return getNumber() != null;
	}
}
