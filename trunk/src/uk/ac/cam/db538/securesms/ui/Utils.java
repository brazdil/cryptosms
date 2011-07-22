package uk.ac.cam.db538.securesms.ui;

import android.content.Context;
import android.telephony.TelephonyManager;

public class Utils {
	
	/**
	 * Returns the phone number of currently active SIM
	 * @param context
	 * @return
	 */
	public static String getSimNumber(Context context) {
		TelephonyManager tMgr =(TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

		// TODO: JUST FOR TESTING
		if (tMgr.getLine1Number() == null)
			return "+123456789012";
		
		return tMgr.getLine1Number();
	}
}
