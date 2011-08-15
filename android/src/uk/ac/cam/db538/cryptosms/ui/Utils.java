package uk.ac.cam.db538.cryptosms.ui;

import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.Preferences;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.DummyOnClickListener;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;
import uk.ac.cam.db538.cryptosms.utils.SimNumber;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.telephony.PhoneNumberUtils;

public class Utils {
	private static boolean mImportShown = false;
	private static boolean mWarningSIMNotPresent = false;
	
	private static final String DIALOG_NO_SIM_AVAILABLE = "NO_SIM_AVAILABLE";
	private static final String DIALOG_IMPORT_KEYS = "IMPORT_KEYS";
	
	public static void prepareDialogs(DialogManager manager, final Context context) {
		manager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild() {
				Resources res = context.getResources();
				return new AlertDialog.Builder(context)
			       .setTitle(res.getString(R.string.error_no_sim_available))
			       .setMessage(res.getString(R.string.error_no_sim_available_details))
			       .setPositiveButton(res.getString(R.string.ok), new DummyOnClickListener())
			       .setCancelable(false)
			       .create();
			}

			@Override
			public String getId() {
				return DIALOG_NO_SIM_AVAILABLE;
			}
		});

		manager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild() {
				Resources res = context.getResources();
				
				// make list of phone numbers that have session keys
				final ArrayList<SimNumber> phoneNumbers = new ArrayList<SimNumber>();
				try {
					phoneNumbers.addAll(Conversation.filterOnlyPhoneNumbers(Conversation.getAllSimNumbersStored()));				
				} catch (StorageFileException ex) {
				}
				
				// are there any?
				if (phoneNumbers.size() > 0) {
					return new AlertDialog.Builder(context)
					       .setTitle(res.getString(R.string.error_no_sim_number))
						   .setMessage(res.getString(R.string.error_no_sim_number_details))
						   .setNegativeButton(res.getString(R.string.never), new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,	int which) {
									SharedPreferences.Editor editor = Preferences.getSingleton().getPreferences().edit();
									editor.putString(Preferences.PREFERENCES_IMPORT_NEVER, SimCard.getSingleton().getSimNumber().getNumber());
									editor.commit();
								}
						   })
						   .setNeutralButton(res.getString(R.string.later), new DummyOnClickListener())
						   .setPositiveButton(res.getString(R.string.yes), new OnClickListener() {
							   @Override
							   public void onClick(DialogInterface dialog, int which) {
								   Utils.moveSessionKeys(context, phoneNumbers);
							   }
						   })
						   .setCancelable(false)
						   .create();
				}
				return null;
			}

			@Override
			public String getId() {
				return DIALOG_IMPORT_KEYS;
			}
		});
	}
	
	public static void handleSimIssues(Context context, DialogManager dialogManager) {
		SimNumber simNumber = SimCard.getSingleton().getSimNumber();
		
		if (simNumber == null) {
			// no SIM present (possibly Airplane mode)
			if (!mWarningSIMNotPresent) {
				dialogManager.showDialog(DIALOG_NO_SIM_AVAILABLE);
				mWarningSIMNotPresent = true;
			}
		} else if (simNumber.isSerial()) {
			// offer import
			// check preferences
			String pref = Preferences.getSingleton().getPreferences().getString(Preferences.PREFERENCES_IMPORT_NEVER, null);
			SimNumber declinedSim = null;
			if (pref != null)
				declinedSim = new SimNumber(pref, true);
			if (!mImportShown && (declinedSim == null || simNumber.equals(declinedSim))) {
				mImportShown = true;
				dialogManager.showDialog(DIALOG_IMPORT_KEYS);
			}
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
		SimNumber simNumber = SimCard.getSingleton().getSimPhoneNumber();
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
	
	public static void moveSessionKeys(final Context context, final ArrayList<SimNumber> phoneNumbers) {
		Resources res = context.getResources();
		
		final SimNumber simNumber = SimCard.getSingleton().getSimPhoneNumber();
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
			    @Override
				public void onClick(DialogInterface dialog, int item) {
			    	if (item < phoneNumbers.size()) {
						try {
							if (simNumber.getNumber().length() == 0)
								Conversation.changeAllSessionKeys(phoneNumbers.get(item), SimCard.getSingleton().getSimSerialNumber());
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
