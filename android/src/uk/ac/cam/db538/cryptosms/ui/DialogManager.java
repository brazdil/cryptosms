package uk.ac.cam.db538.cryptosms.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import android.app.Dialog;
import android.os.Bundle;

public class DialogManager {
	
	private static final String INSTANCE_PARAMS = "PARAMS";
	private static final String INSTANCE_STATE = "STATE";
	
	public static interface DialogBuilder {
		public Dialog onBuild(Bundle params);
		public String getId();
	}
	
	private static class DialogInstance {
		public Dialog dialog;
		public Bundle params;
		
		public DialogInstance(Dialog dialog, Bundle params) {
			this.dialog = dialog;
			this.params = params;
		}
	}
	
	private HashMap<String, DialogInstance> mActiveDialogs = new HashMap<String, DialogInstance>();
	private ArrayList<DialogBuilder> mDialogBuilders = new ArrayList<DialogBuilder>();
	private Bundle mSavedState;
	
	public DialogManager() {
	}
	
	private Dialog showDialog(DialogBuilder builder, Bundle params) {
		Dialog dialog = builder.onBuild(params);
		if (dialog != null) {
			mActiveDialogs.put(builder.getId(), new DialogInstance(dialog, params));
			dialog.show();
		}
		return dialog;
	}
	
	/**
	 * Shows a given dialog
	 * @param id
	 * @return
	 */
	public Dialog showDialog(String id, Bundle params) {
		for (DialogBuilder builder : mDialogBuilders)
			if (builder.getId().equals(id))
				return showDialog(builder, params);
		return null;
	}
	
	/**
	 * Dismisses a given dialog
	 * @param id
	 */
	public void dismissDialog(String id) {
		Dialog dialog = getDialog(id);
		if (dialog != null) {
			mActiveDialogs.remove(id);
			dialog.dismiss();
		} else if (mSavedState != null) {
			// try removing from saved state
			mSavedState.remove(id);
		}
	}
	
	public Dialog getDialog(String id) {
		return mActiveDialogs.get(id).dialog;
	}
	
	/**
	 * Dismisses all given dialogs and saves their states into a bundle
	 * @param savedState
	 */
	public void saveState() {
		if (mSavedState == null)
			mSavedState = new Bundle();
		for (Entry<String, DialogInstance> entry : mActiveDialogs.entrySet()) {
			Dialog dialog = entry.getValue().dialog;
			if (dialog.isShowing()) {
				dialog.dismiss();
				Bundle instance = new Bundle();
				instance.putBundle(INSTANCE_PARAMS, entry.getValue().params);
				instance.putBundle(INSTANCE_STATE, dialog.onSaveInstanceState());
				mSavedState.putBundle(entry.getKey(), instance);
			}
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
				Bundle instance = mSavedState.getBundle(builder.getId());
				Dialog dialog = showDialog(builder, instance.getBundle(INSTANCE_PARAMS));
				Bundle dialogState = instance.getBundle(INSTANCE_STATE);
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
