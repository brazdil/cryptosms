package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.Preferences;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys.SimNumber;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.telephony.PhoneNumberUtils;

public class Utils {
	
	private static final String PREFS_IMPORT_NEVER = "IMPORT_NEVER";
	private static boolean mImportShown = false;
	private static boolean warningSIMNotPresent = false;
	
	/**
	 * Checks whether there is a phone number available for this SIM card.
	 * @param context
	 * @return
	 * @throws StorageFileException 
	 * @throws IOException 
	 */
	public static boolean checkSimPhoneNumberAvailable(final Context context) throws StorageFileException {
		final Resources res = context.getResources();
		String simNumber = SimCard.getSingleton().getSimPhoneNumber(context);
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
			final String simSerial = SimCard.getSingleton().getSimSerialNumber(context);

			// offer import
			// check preferences
			String declinedSim = Preferences.getSingleton().getPreferences().getString(PREFS_IMPORT_NEVER, null);
			if (!mImportShown && (declinedSim == null || simSerial.compareTo(declinedSim) != 0)) {
				// make list of phone numbers that have session keys
				final ArrayList<SimNumber> phoneNumbers = Conversation.filterOnlyPhoneNumbers(Conversation.getAllSimNumbersStored());
				// are there any?
				if (phoneNumbers.size() > 0) {
					mImportShown = true;				
					// give user an option to choose
					new AlertDialog.Builder(context)
						.setTitle(res.getString(R.string.error_no_sim_number))
						.setMessage(res.getString(R.string.error_no_sim_number_details))
						.setNegativeButton(res.getString(R.string.never), new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,	int which) {
								SharedPreferences.Editor editor = Preferences.getSingleton().getPreferences().edit();
								editor.putString(PREFS_IMPORT_NEVER, simSerial);
								editor.commit();
							}
						})
						.setNeutralButton(res.getString(R.string.later), new DummyOnClickListener())
						.setPositiveButton(res.getString(R.string.yes), new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								moveSessionKeys(context, phoneNumbers);
							}
						})
						.show();
				}
			}
			return (simSerial != null && simSerial.length() > 0);
		} else {
			// everything is fine
			return true;
		}
	}
	
	/**
	 * Lets the user transfer some of the existing keys to different SIM / phone number
	 * @param context
	 */
	public static void moveSessionKeys(Context context) {
		//TODO: Nicer import!!!
		// Let the user choose which contacts, etc...
		Resources res = context.getResources();
		SimNumber simNumber = SimCard.getSingleton().getSimPhoneNumberWrapped(context);
		if (simNumber == null) // no SIM or Airplane mode
			return;
		
		try {
			ArrayList<SimNumber> phoneNumbers = Conversation.filterOutNumber(
													Conversation.filterOnlyPhoneNumbers(
			                                        	Conversation.getAllSimNumbersStored()
			                                        ),
			                                        simNumber
			                                    );
			if (phoneNumbers.size() > 0)
				moveSessionKeys(context, phoneNumbers);
			else
				new AlertDialog.Builder(context)
					.setTitle(res.getString(R.string.menu_move_sessions_none))
					.setMessage(res.getString(R.string.menu_move_sessions_none_details))
					.setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
					.show();

		} catch (StorageFileException ex) {
			State.fatalException(ex);
			return;
		}
	}
	
	private static void moveSessionKeys(final Context context, final ArrayList<SimNumber> phoneNumbers) {
		Resources res = context.getResources();
		
		final SimNumber simNumber = SimCard.getSingleton().getSimPhoneNumberWrapped(context);
		if (simNumber == null)
			return;

		String[] numbers = new String[phoneNumbers.size() + 1];
		int i = 0;
		for (SimNumber n : phoneNumbers)
			numbers[i++] = n.getNumber();
		numbers[i] = res.getString(R.string.error_no_sim_number_import_none);
		
		// display them in a dialog
		new AlertDialog.Builder(context)
			.setTitle(res.getString(R.string.error_no_sim_number_import))
			.setItems(numbers, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	if (item < phoneNumbers.size()) {
						try {
							if (simNumber.getNumber().length() == 0)
								Conversation.changeAllSessionKeys(phoneNumbers.get(item), SimCard.getSingleton().getSimSerialNumberWrapped(context));
							else
								Conversation.changeAllSessionKeys(phoneNumbers.get(item), simNumber);
						} catch (StorageFileException ex) {
							State.fatalException(ex);
							return;
						}
			    	}
			    }
			})
			.show();
	}
	
	public static String formatPhoneNumber(String phoneNumber) {
		return PhoneNumberUtils.stripSeparators(phoneNumber);
	}
}
