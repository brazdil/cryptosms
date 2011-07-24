package uk.ac.cam.db538.securesms.utils;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

public class Common {
	
	/**
	 * Returns the phone number of currently active SIM
	 * @param context
	 * @return
	 */
	public static String getSimNumber(Context context) {
		// TODO: JUST FOR TESTING
		return "+123456789012";

//		TelephonyManager tMgr =(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//		return tMgr.getLine1Number();
	}
	
	public static String formatPhoneNumber(String phoneNumber) {
		return PhoneNumberUtils.stripSeparators(phoneNumber);
	}
}
