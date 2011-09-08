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
	
	public static void removeListener(StateChangeListener listener) {
		mListeners.remove(listener);
	}
	
	public static void notifyConnect() {
		for (StateChangeListener listener : mListeners)
			listener.onConnect();
		Pki.login(false);
	}
	
	public static void notifyLogin() {
		for (StateChangeListener listener : mListeners)
			listener.onLogin();
		notifySimState();
		notifyNewEvent();
	}
	
	public static void notifyLogout() {
		for (StateChangeListener listener : mListeners)
			listener.onLogout();
	}

	public static void notifyDisconnect() {
		notifyLogout();
		for (StateChangeListener listener : mListeners)
			listener.onDisconnect();
	}
	
	public static void notifyPkiMissing() {
		notifyDisconnect();
		for (StateChangeListener listener : mListeners)
			listener.onPkiMissing();
	}
	
	public static void notifySimState() {
		if (Pki.isLoggedIn()) {
			for (StateChangeListener listener : mListeners)
				listener.onSimState();
		}
	}
	
	public static void notifyNewEvent() {
		if (Pki.isLoggedIn()) {
			for (StateChangeListener listener : mListeners)
				listener.onNewEvent();
		}
	}
	
	public static void notifyEventParsingStarted() {
		if (Pki.isLoggedIn()) {
			mCurrentlyParsing = true;
			for (StateChangeListener listener : mListeners)
				listener.onEventParsingStarted();
		}
	}

	public static void notifyEventParsingFinished() {
		if (Pki.isLoggedIn()) {
			mCurrentlyParsing = false;
			for (StateChangeListener listener : mListeners)
				listener.onEventParsingFinished();
		}
	}

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
