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
package uk.ac.cam.db538.cryptosms.ui.activity;

import roboguice.activity.RoboActivity;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.state.State.StateChangeListener;
import uk.ac.cam.db538.cryptosms.ui.DialogManager;
import uk.ac.cam.db538.cryptosms.ui.ErrorOverlay;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

/*
 * Class that other activities should extend, because
 * it automatically handles PKI state for them
 */
public class ActivityAppState extends RoboActivity {
	private static final String BUNDLE_DIALOG_MANAGER = "DIALOG_MANAGER";

	private ErrorOverlay mErrorOverlay;
	private View mMainView;
	private DialogManager mDialogManager = new DialogManager();
	
	// CONTENT STUFF
	
	/* (non-Javadoc)
	 * @see roboguice.activity.RoboActivity#setContentView(android.view.View, android.view.ViewGroup.LayoutParams)
	 */
	@Override
	public void setContentView(View view, LayoutParams params) {
		mMainView = view;
		mErrorOverlay = new ErrorOverlay(this);
		mErrorOverlay.setMainView(mMainView);
		getErrorOverlay().hide();
		
		FrameLayout layoutRoot = new FrameLayout(this);
		layoutRoot.addView(mMainView, params);
		layoutRoot.addView(mErrorOverlay, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		super.setContentView(layoutRoot);
	}

	@Override
	public void setContentView(View view) {
		setContentView(view, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	@Override
	public void setContentView(int layoutResID) {
		setContentView(LayoutInflater.from(this).inflate(layoutResID, null));
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#findViewById(int)
	 */
	@Override
	public View findViewById(int id) {
		return mMainView.findViewById(id);
	}

	public ErrorOverlay getErrorOverlay() {
		return mErrorOverlay;
	}

	public View getMainView() {
		return mMainView;
	}
	
	public DialogManager getDialogManager() {
		return mDialogManager;
	}
	
	// STATE LISTENER

	private StateChangeListener mStateListener = new StateChangeListener() {
		@Override
		public void onSimState() {
			ActivityAppState.this.onSimState();
		}
		
		@Override
		public void onPkiMissing() {
			ActivityAppState.this.onPkiMissing();
		}
		
		@Override
		public void onLogout() {
			ActivityAppState.this.onPkiLogout();
		}
		
		@Override
		public void onLogin() {
			ActivityAppState.this.onPkiLogin();
		}
		
		@Override
		public void onFatalException(Exception ex) {
			ActivityAppState.this.onFatalException(ex);
		}
		
		@Override
		public void onDisconnect() {
			ActivityAppState.this.onPkiDisconnect();
		}
		
		@Override
		public void onConnect() {
			ActivityAppState.this.onPkiConnect();
		}

		@Override
		public void onNewEvent() {
			ActivityAppState.this.onNewEvent();
		}
		
		@Override
		public void onEventParsingFinished() {
			ActivityAppState.this.onEventParsingFinished();
		}

		@Override
		public void onEventParsingStarted() {
			ActivityAppState.this.onEventParsingStarted();
		}
	};
	
	/**
	 * When SIM state changes
	 */
	public void onSimState() {
	}
	
	/**
	 * When PKIwrapper connects to PKI
	 */
	public void onPkiConnect() {
	}
	
	/**
	 * When disconnected from PKI
	 */
	public void onPkiDisconnect() {
		getErrorOverlay().modeDisconnected();
		getErrorOverlay().show();
		this.closeContextMenu();
		this.closeOptionsMenu();
	}
	
	/**
	 * When user logs into PKI
	 */
	public void onPkiLogin() {
		getErrorOverlay().hide();
        mDialogManager.restoreState();
	}
	
	/**
	 * When user logs out of PKI
	 */
	public void onPkiLogout() {
		getErrorOverlay().modeLogin();
		getErrorOverlay().show();
        mDialogManager.saveState();
		this.closeContextMenu();
		this.closeOptionsMenu();
	}
	
	/**
	 * When PKI is not installed
	 */
	public void onPkiMissing() {
		getErrorOverlay().modePkiMissing();
		getErrorOverlay().show();
		this.closeContextMenu();
		this.closeOptionsMenu();
	}

	/**
	 * When new message arrives on phone
	 */
	public void onNewEvent() {
	}
	
	/**
	 * When parsing of messages starts
	 */
	public void onEventParsingStarted() {
	}

	/**
	 * When parsing of messages is finished
	 */
	public void onEventParsingFinished() {
	}

	/**
	 * When a fatal exception occurs
	 *
	 * @param ex exception
	 */
	public void onFatalException(Exception ex) {
		if (ex instanceof WrongKeyDecryptionException)
			getErrorOverlay().modeCorruptedFile();
		else
			getErrorOverlay().modeFatalException(ex);
		getErrorOverlay().show();
		this.closeContextMenu();
		this.closeOptionsMenu();
	}

	// OVERRIDES
	
	@Override
	protected void onStart() {
		super.onStart();
		Pki.login(false);
		State.addListener(mStateListener);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		State.removeListener(mStateListener);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		
		mDialogManager.setSavedState(state.getBundle(BUNDLE_DIALOG_MANAGER));
		mDialogManager.restoreState();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		mDialogManager.saveState();
		outState.putBundle(BUNDLE_DIALOG_MANAGER, mDialogManager.getSavedState());
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return Pki.isLoggedIn();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		if (Pki.isLoggedIn())
			super.onBackPressed();
	}
}
