/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package uk.ac.cam.db538.cryptosms.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import android.app.Dialog;
import android.os.Bundle;

/*
 * Class that handles all dialogs in the system, 
 * saving/restoring their state when application is locked
 */
public class DialogManager {
	
	private static final String INSTANCE_PARAMS = "PARAMS";
	private static final String INSTANCE_STATE = "STATE";
	
	public static interface DialogBuilder {
		
		/**
		 * Called when dialog needs to be created.
		 * Should return Dialog object
		 *
		 * @param params the params
		 * @return the dialog
		 */
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
	
	/**
	 * Instantiates a new dialog manager.
	 */
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
	 * Shows the given dialog.
	 *
	 * @param id the id
	 * @param params the params
	 * @return the dialog
	 */
	public Dialog showDialog(String id, Bundle params) {
		for (DialogBuilder builder : mDialogBuilders)
			if (builder.getId().equals(id))
				return showDialog(builder, params);
		return null;
	}
	
	/**
	 * Dismisses the given dialog.
	 *
	 * @param id the id
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
	
	/**
	 * Gets the dialog object.
	 *
	 * @param id the id
	 * @return the dialog
	 */
	public Dialog getDialog(String id) {
		return mActiveDialogs.get(id).dialog;
	}
	
	/**
	 * Dismisses all given dialogs and saves their states into a bundle.
	 *
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
	 * Restores all previously active dialogs and lets them restore their own state.
	 *
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
	
	/**
	 * Adds a dialog builder.
	 *
	 * @param builder the builder
	 */
	public void addBuilder(DialogBuilder builder) {
		mDialogBuilders.add(builder);
	}
	
	/**
	 * Removes a dialog builder.
	 *
	 * @param builder the builder
	 */
	public void removeBuilder(DialogBuilder builder) {
		mDialogBuilders.remove(builder);
	}
}
