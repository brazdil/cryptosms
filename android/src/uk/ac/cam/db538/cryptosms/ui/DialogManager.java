package uk.ac.cam.db538.cryptosms.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import uk.ac.cam.db538.cryptosms.Preferences;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.DummyOnClickListener;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.utils.SimNumber;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;

public class DialogManager {
	
	public static interface DialogBuilder {
		public Dialog onBuild();
		public String getId();
	}
	
	private Context mContext;
	private HashMap<String, Dialog> mActiveDialogs = new HashMap<String, Dialog>();
	private ArrayList<DialogBuilder> mDialogBuilders = new ArrayList<DialogBuilder>();
	private Bundle mSavedState;
	
	public DialogManager(Context context) {
		mContext = context;
	}
	
	private Dialog showDialog(DialogBuilder builder) {
		Dialog dialog = builder.onBuild();
		if (dialog != null) {
			mActiveDialogs.put(builder.getId(), dialog);
			dialog.show();
		}
		return dialog;
	}
	
	/**
	 * Shows a given dialog
	 * @param id
	 * @return
	 */
	public Dialog showDialog(String id) {
		for (DialogBuilder builder : mDialogBuilders)
			if (builder.getId().equals(id))
				return showDialog(builder);
		return null;
	}
	
	/**
	 * Dismisses a given dialog
	 * @param id
	 */
	public void dismissDialog(String id) {
		Dialog dialog = mActiveDialogs.get(id);
		mActiveDialogs.remove(id);
		if (dialog != null)
			dialog.dismiss();
	}
	
	/**
	 * Dismisses all given dialogs and saves their states into a bundle
	 * @param savedState
	 */
	public void saveState() {
		if (mSavedState == null)
			mSavedState = new Bundle();
		for (Entry<String, Dialog> entry : mActiveDialogs.entrySet()) {
			entry.getValue().dismiss();
			mSavedState.putBundle(entry.getKey(), entry.getValue().onSaveInstanceState());
		}
		mActiveDialogs.clear();
	}
	
	/**
	 * Restores all previously active dialogs and lets them restore their own state
	 * @param savedState
	 */
	public void restoreState() {
		if (mSavedState == null)
			return;
		
		for (DialogBuilder builder : mDialogBuilders) {
			if (mSavedState.containsKey(builder.getId())) {
				Dialog dialog = showDialog(builder);
				Bundle dialogState = mSavedState.getBundle(builder.getId());
				if (dialogState != null)
					dialog.onRestoreInstanceState(dialogState);
			}
		}
		
		mSavedState = null;
	}
	
	/**
	 * Returns the saved state. Call saveState() first, unless you really don't want to!
	 * @return
	 */
	public Bundle getSavedState() {
		return mSavedState;
	}
	
	
	/**
	 * Sets the internal saved state. This call should usually be followed by calling the restoreState() method. 
	 * @param savedState
	 */
	public void setSavedState(Bundle savedState) {
		mSavedState = savedState;
	}
	
	public void addBuilder(DialogBuilder builder) {
		mDialogBuilders.add(builder);
	}
	
	public void removeBuilder(DialogBuilder builder) {
		mDialogBuilders.remove(builder);
	}
}
