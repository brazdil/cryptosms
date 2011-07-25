package uk.ac.cam.db538.securesms.utils;

import java.io.IOException;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.database.DatabaseFileException;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
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

	public static void dialogDatabaseError(Context context, DatabaseFileException ex) {
		Resources res = context.getResources();
		
		new AlertDialog.Builder(context)
			.setTitle(res.getString(R.string.error_database))
			.setMessage(res.getString(R.string.error_database_details) + "\n" + ex.getMessage())
			.setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
			.show();
	}
	
	public static void dialogIOError(Context context, IOException ex) {
		Resources res = context.getResources();
		
		new AlertDialog.Builder(context)
			.setTitle(res.getString(R.string.error_io))
			.setMessage(res.getString(R.string.error_io_details) + "\n" + ex.getMessage())
			.setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
			.show();
	}
}
