package uk.ac.cam.db538.securesms.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class SimCard {
	
	/**
	 * Returns the phone number of currently active SIM
	 * @param context
	 * @return
	 */
	public static String getSimPhoneNumber(Context context) {
		// airplane mode
		boolean airplaneMode = Settings.System.getInt(
			      context.getContentResolver(), 
			      Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		if (airplaneMode)
			return null;
		
		TelephonyManager tMgr =(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return tMgr.getLine1Number(); //"+447879116797"
	}
	
	/**
	 * Returns the phone number of currently active SIM
	 * @param context
	 * @return
	 */
	public static String getSimSerialNumber(Context context) {
		// airplane mode
		boolean airplaneMode = Settings.System.getInt(
			      context.getContentResolver(), 
			      Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		if (airplaneMode)
			return null;
		
		TelephonyManager tMgr =(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return tMgr.getSimSerialNumber();
	}

	
	public static interface OnSimStateListener {
		public void onChange();
	}
	
	public static void registerSimStateListener(Context context, OnSimStateListener listener) {
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
