package uk.ac.cam.db538.securesms.utils;

import java.io.IOException;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.database.DatabaseFileException;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

public class Common {
	
	private static boolean warningSIMError = false;
	private static boolean warningSIMNotPresent = false;
	
	/**
	 * Checks whether there is a phone number available for this SIM card.
	 * @param context
	 * @return
	 */
	public static boolean checkSimNumberAvailable(Context context) {
		Resources res = context.getResources();
		String simNumber = getSimNumber(context);
		if (simNumber == null) {
			// no SIM present (possibly Airplane mode)
			if (!warningSIMError) {
				new AlertDialog.Builder(context)
				.setTitle(res.getString(R.string.error_no_sim_available))
				.setMessage(res.getString(R.string.error_no_sim_available_details))
				.setPositiveButton(res.getString(R.string.ok), new DummyOnClickListener())
				.show();
				warningSIMError = true;
			}
			return false;
		} else if (simNumber.length() == 0) {
			// unknown number, but SIM present
			if (warningSIMError) {
				// user has already been notified, but didn't set it manually
				return false;
			} else {
				new AlertDialog.Builder(context)
				.setTitle(res.getString(R.string.error_no_sim_number))
				.setMessage(res.getString(R.string.error_no_sim_number_details))
				.setPositiveButton(res.getString(R.string.read_only), new DummyOnClickListener())
				.setNegativeButton(res.getString(R.string.setup), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Take to settings
					}
				})
				.show();
				warningSIMError = true;
				// return recursively;
				return checkSimNumberAvailable(context);
			}
		} else {
			// everything is fine
			return true;
		}
	}
	
	/**
	 * Returns the phone number of currently active SIM
	 * @param context
	 * @return
	 */
	public static String getSimNumber(Context context) {
		// airplane mode
		boolean airplaneMode = Settings.System.getInt(
			      context.getContentResolver(), 
			      Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		if (airplaneMode)
			return null;
		
		TelephonyManager tMgr =(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String simNumber = tMgr.getLine1Number();
		if (simNumber != null && simNumber.length() == 0)
			return "+447879116797";
		else
			return simNumber;
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
