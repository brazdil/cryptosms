package uk.ac.cam.db538.securesms.utils;

import java.io.IOException;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.database.Conversation;
import uk.ac.cam.db538.securesms.database.DatabaseFileException;
import uk.ac.cam.db538.securesms.database.SessionKeys;
import uk.ac.cam.db538.securesms.database.SessionKeys.SessionKeysStatus;
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
	
	private static boolean warningSIMNotPresent = false;
	
	/**
	 * Checks whether there is a phone number available for this SIM card.
	 * @param context
	 * @return
	 */
	public static boolean checkSimPhoneNumberAvailable(Context context) {
		Resources res = context.getResources();
		String simNumber = getSimPhoneNumber(context);
		if (simNumber == null) {
			// no SIM present (possibly Airplane mode)
			if (!warningSIMNotPresent) {
				new AlertDialog.Builder(context)
				.setTitle(res.getString(R.string.error_no_sim_available))
				.setMessage(res.getString(R.string.error_no_sim_available_details))
				.setPositiveButton(res.getString(R.string.ok), new DummyOnClickListener())
				.show();
				warningSIMNotPresent = true;
			}
			return false;
		} else if (simNumber.length() == 0) {
			// unknown number, but SIM present
			// try to get its serial number
			String simSerial = getSimSerialNumber(context);
			return (simSerial != null && simSerial.length() > 0);
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

	/**
	 * Tries to find session keys for this particular SIM,
	 * either by phone number of (if not available) by SIM's serial number
	 * @param context
	 * @param conv
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public static SessionKeys getSessionKeysForSIM(Context context, Conversation conv) throws DatabaseFileException, IOException { 
		return getSessionKeysForSIM(context, conv, true);
	}
	
	/**
	 * Tries to find session keys for this particular SIM,
	 * either by phone number of (if not available) by SIM's serial number
	 * @param context
	 * @param conv
	 * @lockAllow		Allow to be locked
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public static SessionKeys getSessionKeysForSIM(Context context, Conversation conv, boolean lockAllow) throws DatabaseFileException, IOException {
		String simNumber = Common.getSimPhoneNumber(context);
		String simSerial = Common.getSimSerialNumber(context);
		SessionKeys keysSerial = conv.getSessionKeys(simSerial, true, lockAllow);
		if (simNumber == null || simNumber.length() == 0) {
			// no phone number
			return keysSerial;
		} else {
			// we know the phone number
			SessionKeys keysNumber = conv.getSessionKeys(simNumber, false, lockAllow);

			// ? exists both with serial and phone number ?
			if (keysSerial != null) {
				// update it to use phone number
				keysSerial.setSimNumber(simNumber);
				keysSerial.setSimSerial(false);
				keysSerial.saveToFile(lockAllow);
			}

			if (keysNumber == null) {
				// don't have one for phone number
				// try by serial number
				keysNumber = keysSerial;
			} else if (keysSerial != null) {
				// there are two entries
				// choose the one with matching serial number
				keysNumber.delete(lockAllow);
				keysNumber = keysSerial;
			}
			return keysNumber;
		}
	}
	
	/**
	 * Tries to find session keys for this SIM and if it succeeds, 
	 * returns whether they have been successfully exchanged.
	 * @param context
	 * @param conv
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public static boolean hasKeysExchangedForSIM(Context context, Conversation conv) throws DatabaseFileException, IOException {
		return hasKeysExchangedForSIM(context, conv, true);
	}

	/**
	 * Tries to find session keys for this SIM and if it succeeds, 
	 * returns whether they have been successfully exchanged.
	 * @param context
	 * @param conv
	 * @param lockAllow		Allow storage file locks
	 * @return
	 * @throws DatabaseFileException
	 * @throws IOException
	 */
	public static boolean hasKeysExchangedForSIM(Context context, Conversation conv, boolean lockAllow) throws DatabaseFileException, IOException {
		SessionKeys keys = getSessionKeysForSIM(context, conv, lockAllow);
		if (keys == null)
			return false;
		return keys.getStatus() == SessionKeysStatus.KEYS_EXCHANGED;
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
