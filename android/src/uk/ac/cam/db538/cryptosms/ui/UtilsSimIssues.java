package uk.ac.cam.db538.cryptosms.ui;

import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.Preferences;
import uk.ac.cam.db538.cryptosms.R;
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
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public class UtilsSimIssues {
	private static boolean mImportShown = false;
	private static boolean mWarningSIMNotPresent = false;
	
	private static final String DIALOG_NO_SIM_AVAILABLE = "DIALOG_NO_SIM_AVAILABLE";
	private static final String DIALOG_IMPORT_KEYS = "DIALOG_IMPORT_KEYS";
	private static final String DIALOG_NO_SESSION_KEYS = "DIALOG_NO_SESSION_KEYS";
	private static final String DIALOG_EXISTING_PHONE_NUMBERS = "DIALOG_EXISTING_PHONE_NUMBERS";
	
	public static void prepareDialogs(final DialogManager manager, final Context context) {
		manager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
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
			public Dialog onBuild(Bundle params) {
				Resources res = context.getResources();
				return new AlertDialog.Builder(context)
					   .setTitle(res.getString(R.string.menu_move_sessions_none))
					   .setMessage(res.getString(R.string.menu_move_sessions_none_details))
					   .setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
					   .create();
			}

			@Override
			public String getId() {
				return DIALOG_NO_SESSION_KEYS;
			}
		});

		manager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
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
									editor.putString(Preferences.PREFERENCES_IMPORT_NEVER, SimCard.getSingleton().getNumber().getNumber());
									editor.commit();
								}
						   })
						   .setNeutralButton(res.getString(R.string.later), new DummyOnClickListener())
						   .setPositiveButton(res.getString(R.string.yes), new OnClickListener() {
							   @Override
							   public void onClick(DialogInterface dialog, int which) {
								   UtilsSimIssues.moveSessionKeys(manager);
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

		manager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = context.getResources();
				
				final SimNumber simNumber = SimCard.getSingleton().getPhoneNumber();
				final ArrayList<SimNumber> phoneNumbers = new ArrayList<SimNumber>();
				try {
					phoneNumbers.addAll(Conversation.filterOutNumber(
						Conversation.filterOnlyPhoneNumbers(
                            	Conversation.getAllSimNumbersStored()
                            ),
                            simNumber
                        )
                    );
				} catch (StorageFileException ex) {
				}
				
				String[] numbers = new String[phoneNumbers.size() + 1];
				int i = 0;
				for (SimNumber n : phoneNumbers)
					numbers[i++] = n.getNumber();
				numbers[i] = res.getString(R.string.error_no_sim_number_import_none);
				
				// display them in a dialog
				return new AlertDialog.Builder(context)
					   .setTitle(res.getString(R.string.error_no_sim_number_import))
					   .setItems(numbers, new DialogInterface.OnClickListener() {
						   @Override
						   public void onClick(DialogInterface dialog, int item) {
							   if (item < phoneNumbers.size()) {
								   try {
									   if (simNumber.getNumber().length() == 0)
										   Conversation.changeAllSessionKeys(phoneNumbers.get(item), SimCard.getSingleton().getSerialNumber());
									   else
										   Conversation.changeAllSessionKeys(phoneNumbers.get(item), simNumber);
								   } catch (StorageFileException ex) {
									   State.fatalException(ex);
									   return;
								   }
							   }
						   }
					   })
					   .create();
			}

			@Override
			public String getId() {
				return DIALOG_EXISTING_PHONE_NUMBERS;
			}
		});
	}
	
	public static void handleSimIssues(Context context, DialogManager dialogManager) {
		SimNumber simNumber = SimCard.getSingleton().getNumber();
		
		if (simNumber == null) {
			// no SIM present (possibly Airplane mode)
			if (!mWarningSIMNotPresent) {
				dialogManager.showDialog(DIALOG_NO_SIM_AVAILABLE, null);
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
				dialogManager.showDialog(DIALOG_IMPORT_KEYS, null);
			}
		}
	}
	
	/**
	 * Lets the user transfer some of the existing keys to different SIM / phone number
	 * @param context
	 * @throws StorageFileException 
	 */
	public static void moveSessionKeys(Context context, DialogManager dialogManager) throws StorageFileException {
		//TODO: Nicer import!!!
		// Let the user choose which contacts, etc...
		SimNumber simNumber = SimCard.getSingleton().getPhoneNumber();
		if (simNumber == null) // no SIM or Airplane mode
			return;
		
		ArrayList<SimNumber> phoneNumbers = Conversation.filterOutNumber(
												Conversation.filterOnlyPhoneNumbers(
		                                        	Conversation.getAllSimNumbersStored()
		                                        ),
		                                        simNumber
		                                    );
		if (phoneNumbers.size() > 0)
			moveSessionKeys(dialogManager);
		else
			dialogManager.showDialog(DIALOG_NO_SESSION_KEYS, null);
	}
	
	public static void moveSessionKeys(DialogManager dialogManager) {
		if (!SimCard.getSingleton().isNumberAvailable())
			return;
		
		dialogManager.showDialog(DIALOG_EXISTING_PHONE_NUMBERS, null);
	}
	
	public static String formatPhoneNumber(String phoneNumber) {
		return PhoneNumberUtils.stripSeparators(phoneNumber);
	}
}
