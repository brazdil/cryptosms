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
package uk.ac.cam.db538.cryptosms.state;

import java.util.ArrayList;

import android.util.Log;

import uk.ac.cam.db538.cryptosms.MyApplication;

public class State {
	public static abstract class StateChangeListener {
		
		public void onConnect() {
		}

		public void onLogin() {
		}
		
		public void onLogout() {
		}
		
		public void onDisconnect() {
		}
		
		public void onPkiMissing() {
		}
		
		public void onSimState() {
		}
		
		public void onNewEvent() {
		}
		
		public void onEventParsingFinished() {
		}
		
		public void onEventParsingStarted() {
		}

		public void onFatalException(Exception ex) {
		}
	}
	
	private static ArrayList<StateChangeListener> mListeners = new ArrayList<StateChangeListener>();
	private static Exception mFatalException = null;
	
	private static boolean mCurrentlyParsing = false;
	
	/**
	 * Adds the listener.
	 *
	 * @param listener the listener
	 */
	public static void addListener(StateChangeListener listener) {
		mListeners.add(listener);
		
		if (isInFatalState())
			listener.onFatalException(mFatalException);
		else if (Pki.isMissing())
			listener.onPkiMissing();
		else if (Pki.isConnected()) {
			listener.onConnect();
			if (Pki.isLoggedIn()) {
				listener.onLogin();
				listener.onSimState();
				listener.onNewEvent();
				if (mCurrentlyParsing)
					listener.onEventParsingStarted();
			} else
				listener.onLogout();
		} else
			listener.onDisconnect();
	}
	
	/**
	 * Removes the listener.
	 *
	 * @param listener the listener
	 */
	public static void removeListener(StateChangeListener listener) {
		mListeners.remove(listener);
	}
	
	/**
	 * Notify connect.
	 */
	public static void notifyConnect() {
		for (StateChangeListener listener : mListeners)
			listener.onConnect();
		Pki.login(false);
	}
	
	/**
	 * Notify login.
	 */
	public static void notifyLogin() {
		for (StateChangeListener listener : mListeners)
			listener.onLogin();
		notifySimState();
		notifyNewEvent();
	}
	
	/**
	 * Notify logout.
	 */
	public static void notifyLogout() {
		for (StateChangeListener listener : mListeners)
			listener.onLogout();
	}

	/**
	 * Notify disconnect.
	 */
	public static void notifyDisconnect() {
		notifyLogout();
		for (StateChangeListener listener : mListeners)
			listener.onDisconnect();
	}
	
	/**
	 * Notify pki missing.
	 */
	public static void notifyPkiMissing() {
		notifyDisconnect();
		for (StateChangeListener listener : mListeners)
			listener.onPkiMissing();
	}
	
	/**
	 * Notify sim state.
	 */
	public static void notifySimState() {
		if (Pki.isLoggedIn()) {
			for (StateChangeListener listener : mListeners)
				listener.onSimState();
		}
	}
	
	/**
	 * Notify new event.
	 */
	public static void notifyNewEvent() {
		if (Pki.isLoggedIn()) {
			for (StateChangeListener listener : mListeners)
				listener.onNewEvent();
		}
	}
	
	/**
	 * Notify event parsing started.
	 */
	public static void notifyEventParsingStarted() {
		if (Pki.isLoggedIn()) {
			mCurrentlyParsing = true;
			for (StateChangeListener listener : mListeners)
				listener.onEventParsingStarted();
		}
	}

	/**
	 * Notify event parsing finished.
	 */
	public static void notifyEventParsingFinished() {
		if (Pki.isLoggedIn()) {
			mCurrentlyParsing = false;
			for (StateChangeListener listener : mListeners)
				listener.onEventParsingFinished();
		}
	}

	/**
	 * Fatal exception.
	 *
	 * @param ex the ex
	 */
	public static void fatalException(Exception ex) {
		notifyDisconnect();
		mFatalException = ex;
		Log.e(MyApplication.APP_TAG, "Fatal exception: " + ex.getClass().getName());
		for (StateChangeListener listener : mListeners)
			listener.onFatalException(ex);
	}
	
	public static boolean isInFatalState() {
		return mFatalException != null;
	}
}
