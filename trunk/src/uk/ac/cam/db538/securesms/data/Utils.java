package uk.ac.cam.db538.securesms.data;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.storage.Conversation;
import uk.ac.cam.db538.securesms.storage.Header;
import uk.ac.cam.db538.securesms.storage.SessionKeys;
import uk.ac.cam.db538.securesms.storage.Storage;
import uk.ac.cam.db538.securesms.storage.StorageFileException;
import uk.ac.cam.db538.securesms.storage.SessionKeys.SessionKeysStatus;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.telephony.PhoneNumberUtils;

public class Utils {

	private static void changeAllSessionKeysFromSerialToPhoneNumber(String serial, String phoneNumber, boolean lockAllow) throws StorageFileException, IOException {
		Storage.getDatabase().lockFile(lockAllow);
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
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			Storage.getDatabase().unlockFile(lockAllow);
		}
	}
	
	private static void changeAllSessionKeysFromPhoneNumberToSerial(String phoneNumber, String serial, boolean lockAllow) throws StorageFileException, IOException {
		Storage.getDatabase().lockFile(lockAllow);
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
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			Storage.getDatabase().unlockFile(lockAllow);
		}
	}

	/**
	 * Tries to find session keys for this particular SIM,
	 * either by phone number of (if not available) by SIM's serial number
	 * @param context
	 * @param conv
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public static SessionKeys getSessionKeysForSIM(Context context, Conversation conv) throws StorageFileException, IOException { 
		return getSessionKeysForSIM(context, conv, true);
	}
	
	private static ArrayList<String> getAllSimNumbersStored(boolean lockAllow) throws StorageFileException, IOException {
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
	private static boolean warningSIMNotPresent = false;
	
	/**
	 * Tries to find session keys for this particular SIM,
	 * either by phone number or (if not available) by SIM's serial number
	 * @param context
	 * @param conv
	 * @lockAllow		Allow to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public static SessionKeys getSessionKeysForSIM(final Context context, Conversation conv, boolean lockAllow) throws StorageFileException, IOException {
		Storage.getDatabase().lockFile(lockAllow);
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
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			Storage.getDatabase().unlockFile(lockAllow);
		}
	}
	
	/**
	 * Tries to find session keys for this SIM and if it succeeds, 
	 * returns whether they have been successfully exchanged.
	 * @param context
	 * @param conv
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public static boolean hasKeysExchangedForSIM(Context context, Conversation conv) throws StorageFileException, IOException {
		return hasKeysExchangedForSIM(context, conv, true);
	}

	/**
	 * Tries to find session keys for this SIM and if it succeeds, 
	 * returns whether they have been successfully exchanged.
	 * @param context
	 * @param conv
	 * @param lockAllow		Allow storage file locks
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public static boolean hasKeysExchangedForSIM(Context context, Conversation conv, boolean lockAllow) throws StorageFileException, IOException {
		SessionKeys keys = getSessionKeysForSIM(context, conv, lockAllow);
		if (keys == null)
			return false;
		return keys.getStatus() == SessionKeysStatus.KEYS_EXCHANGED;
	}

	/**
	 * Checks whether there is a phone number available for this SIM card.
	 * @param context
	 * @return
	 * @throws StorageFileException 
	 * @throws IOException 
	 */
	public static boolean checkSimPhoneNumberAvailable(final Context context) throws IOException, StorageFileException {
		return checkSimPhoneNumberAvailable(context, true);
	}
	
	/**
	 * Checks whether there is a phone number available for this SIM card.
	 * @param context
	 * @return
	 * @throws StorageFileException 
	 * @throws IOException 
	 */
	public static boolean checkSimPhoneNumberAvailable(final Context context, boolean lockAllow) throws IOException, StorageFileException {
		final Resources res = context.getResources();
		String simNumber = SimCard.getSimPhoneNumber(context);
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
			final String simSerial = SimCard.getSimSerialNumber(context);

			Storage.getDatabase().lockFile(lockAllow);
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
													Utils.changeAllSessionKeysFromPhoneNumberToSerial(phoneNumbers.get(item), simSerial, false);
												} catch (StorageFileException e) {
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
			} catch (StorageFileException ex) {
				throw ex;
			} catch (IOException ex) {
				throw ex;
			} finally {
				Storage.getDatabase().unlockFile(lockAllow);
			}
			
			return (simSerial != null && simSerial.length() > 0);
		} else {
			// everything is fine
			return true;
		}
	}

	public static String formatPhoneNumber(String phoneNumber) {
		return PhoneNumberUtils.stripSeparators(phoneNumber);
	}
	
	public static void dialogDatabaseError(Context context, StorageFileException ex) {
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
