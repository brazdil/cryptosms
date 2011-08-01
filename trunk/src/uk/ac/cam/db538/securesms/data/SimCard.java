package uk.ac.cam.db538.securesms.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class SimCard {
	
	private static SimCard mSingleton = new SimCard();
	
	private SimCard() {
		
	}
	
	public static SimCard getSingleton() {
		return mSingleton;
	}
	
	static void setSingleton(SimCard singleton) {
		mSingleton = singleton;
	}
	
	/**
	 * Returns whether the Airplane Mode is on/off
	 * @param context
	 * @return
	 */
	public boolean getAirplaneMode(Context context) {
		try {
			return (Settings.System.getInt(
					context.getContentResolver(), 
					Settings.System.AIRPLANE_MODE_ON, 0) == 1);
		} catch (Exception ex) {
			return true;
		}
	}
	
	/**
	 * Returns the phone number of currently active SIM
	 * @param context
	 * @return
	 */
	public String getSimPhoneNumber(Context context) {
		if (getAirplaneMode(context))
			return null;
		
		TelephonyManager tMgr =(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return tMgr.getLine1Number(); //"+447879116797"
	}
	
	/**
	 * Returns the phone number of currently active SIM
	 * @param context
	 * @return
	 */
	public String getSimSerialNumber(Context context) {
		if (getAirplaneMode(context))
			return null;
		
		TelephonyManager tMgr =(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return tMgr.getSimSerialNumber();
	}
	
	public static interface OnSimStateListener {
		public void onChange();
	}
	
	/**
	 * Registers new listener with the SimStateChanged events
	 * @param context
	 * @param listener
	 */
	public void registerSimStateListener(Context context, OnSimStateListener listener) {
		IntentFilter intentFilter = new IntentFilter("android.intent.action.SERVICE_STATE");
		
		final OnSimStateListener lst = listener;
		BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				lst.onChange();
			}
		};

		context.registerReceiver(receiver, intentFilter);
	}

}
