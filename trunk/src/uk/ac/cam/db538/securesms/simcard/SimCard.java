package uk.ac.cam.db538.securesms.simcard;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.database.Conversation;
import uk.ac.cam.db538.securesms.database.Database;
import uk.ac.cam.db538.securesms.database.DatabaseFileException;
import uk.ac.cam.db538.securesms.database.Header;
import uk.ac.cam.db538.securesms.database.SessionKeys;
import uk.ac.cam.db538.securesms.database.SessionKeys.SessionKeysStatus;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

public class SimCard {
	
	private static boolean warningSIMNotPresent = false;
	
	private static void changeAllSessionKeysFromSerialToPhoneNumber(String serial, String phoneNumber, boolean lockAllow) throws DatabaseFileException, IOException {
		Database.getDatabase().lockFile(lockAllow);
		try {
			Conversation conv = Header.getHeader(false).getFirstConversation(false);
			while (conv != null) {
				// first check whether there are any by this phone number
				SessionKeys temp, keys = conv.getFirstSessionKeys(false);
				while (keys != null) {
					temp = keys.getNextSessionKeys(false);
					if (keys.usesSimSerial() == false && PhoneNumberUtils.compare(keys.getSimNumber(), phoneNumber))
						keys.delete(false);
					keys = temp;
				}

				// update the rest
				keys = conv.getFirstSessionKeys(false);
				while (keys != null) {
					if (keys.usesSimSerial() == true && keys.getSimNumber().compareTo(serial) == 0) {
						keys.setSimSerial(false);
						keys.setSimNumber(phoneNumber);
						keys.saveToFile(false);
					}
					keys = keys.getNextSessionKeys(false);
				}
				conv = conv.getNextConversation(false);
			}
		} catch (DatabaseFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			Database.getDatabase().unlockFile(lockAllow);
		}
	}
	
	private static void changeAllSessionKeysFromPhoneNumberToSerial(String phoneNumber, String serial, boolean lockAllow) throws DatabaseFileException, IOException {
		Database.getDatabase().lockFile(lockAllow);
		try {
			Conversation conv = Header.getHeader(false).getFirstConversation(false);
			while (conv != null) {
				// first check whether there are any by this serial number
				SessionKeys temp, keys = conv.getFirstSessionKeys(false);
				while (keys != null) {
					temp = keys.getNextSessionKeys(false);
					if (keys.usesSimSerial() == true && keys.getSimNumber().compareTo(serial) == 0)
						keys.delete(false);
					keys = temp;
				}
				
				// then update the others
				keys = conv.getFirstSessionKeys(false);
				while (keys != null) {
					if (keys.usesSimSerial() == false && PhoneNumberUtils.compare(keys.getSimNumber(), phoneNumber)) {
						keys.setSimSerial(true);
						keys.setSimNumber(serial);
						keys.saveToFile(false);
					}
					keys = keys.getNextSessionKeys(false);
				}
				conv = conv.getNextConversation(false);
			}
		} catch (DatabaseFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			Database.getDatabase().unlockFile(lockAllow);
		}
	}

	public static boolean checkSimPhoneNumberAvailable(final Context context) throws IOException, DatabaseFileException {
		return checkSimPhoneNumberAvailable(context, true);
	}
	
	/**
	 * Checks whether there is a phone number available for this SIM card.
	 * @param context
	 * @return
	 * @throws DatabaseFileException 
	 * @throws IOException 
	 */
	public static boolean checkSimPhoneNumberAvailable(final Context context, boolean lockAllow) throws IOException, DatabaseFileException {
		final Resources res = context.getResources();
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
			final String simSerial = getSimSerialNumber(context);

			Database.getDatabase().lockFile(lockAllow);
			try {
				// offer import
				// make list of phone numbers that have session keys
				final ArrayList<String> phoneNumbers = getAllSimNumbersStored(false);
				// are there any?
				if (phoneNumbers.size() > 0 && !mImportShown && !mImportDeclined) {
					mImportShown = true;						
					// give user an option to choose
					new AlertDialog.Builder(context)
						.setTitle(res.getString(R.string.error_no_sim_number))
						.setMessage(res.getString(R.string.error_no_sim_number_details))
	    				.setOnCancelListener(new OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
					    		mImportDeclined = true;
							}
						})
						.setNegativeButton(res.getString(R.string.no), new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,	int which) {
								mImportDeclined = true;
							}
						})
						.setPositiveButton(res.getString(R.string.yes), new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								String[] numbers = new String[phoneNumbers.size() + 1];
								numbers = phoneNumbers.toArray(numbers);
								numbers[phoneNumbers.size()] = res.getString(R.string.error_no_sim_number_import_none);
								
								// display them in a dialog
			    				new AlertDialog.Builder(context)
			    					.setTitle(res.getString(R.string.error_no_sim_number_import))
			    					.setItems(numbers, new DialogInterface.OnClickListener() {
				    				    public void onClick(DialogInterface dialog, int item) {
				    				    	if (item < phoneNumbers.size()) {
												try {
													changeAllSessionKeysFromPhoneNumberToSerial(phoneNumbers.get(item), simSerial, false);
												} catch (DatabaseFileException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												} catch (IOException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
				    				    	} else
				    				    		mImportDeclined = true;
				    				    }
				    				})
				    				.setOnCancelListener(new OnCancelListener() {
										@Override
										public void onCancel(DialogInterface dialog) {
			    				    		mImportDeclined = true;
										}
									})
			    					.show();
							}
						})
						.show();
				}
			} catch (DatabaseFileException ex) {
				throw ex;
			} catch (IOException ex) {
				throw ex;
			} finally {
				Database.getDatabase().unlockFile(lockAllow);
			}
			
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
	
	private static ArrayList<String> getAllSimNumbersStored(boolean lockAllow) throws DatabaseFileException, IOException {
		ArrayList<String> phoneNumbers = new ArrayList<String>();
		
		Conversation conv = Header.getHeader(lockAllow).getFirstConversation(lockAllow);
		while (conv != null) {
			SessionKeys keys = conv.getFirstSessionKeys(lockAllow);
			while (keys != null) {
				if (keys.usesSimSerial() == false) {
					boolean found = false;
					for (String n : phoneNumbers)
						if (PhoneNumberUtils.compare(keys.getSimNumber(), n))
							found = true;
					if (!found)
						phoneNumbers.add(keys.getSimNumber());
				}
				keys = keys.getNextSessionKeys(lockAllow);
			}
			conv = conv.getNextConversation(lockAllow);			
		}
		
		return phoneNumbers;
	}
	
	// TODO: move this to proper saved settings
	private static boolean mImportShown = false;
	private static boolean mImportDeclined = false;
	
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
	public static SessionKeys getSessionKeysForSIM(final Context context, Conversation conv, boolean lockAllow) throws DatabaseFileException, IOException {
		final Resources res = context.getResources();
		
		Database.getDatabase().lockFile(lockAllow);
		try {
			final String simNumber = SimCard.getSimPhoneNumber(context);
			final String simSerial = SimCard.getSimSerialNumber(context);
			SessionKeys keysSerial = conv.getSessionKeys(simSerial, true, false);
			if (simNumber == null || simNumber.length() == 0) {
				// no phone number
				return keysSerial;
			} else {
				// we know the phone number
				SessionKeys keysNumber = conv.getSessionKeys(simNumber, false, lockAllow);
	
				// ? exists both with serial and phone number ?
				if (keysSerial != null)
					// update all to use phone number
					changeAllSessionKeysFromSerialToPhoneNumber(simSerial, simNumber, false);
		
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
		} catch (DatabaseFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			Database.getDatabase().unlockFile(lockAllow);
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
