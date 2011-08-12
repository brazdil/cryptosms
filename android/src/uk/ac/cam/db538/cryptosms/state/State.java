package uk.ac.cam.db538.cryptosms.state;

import java.util.ArrayList;

public class State {
	public interface StateChangeListener {
		public void onConnect();
		public void onLogin();
		public void onLogout();
		public void onDisconnect();
		public void onPkiMissing();
	}
	
	private static ArrayList<StateChangeListener> mListeners = new ArrayList<StateChangeListener>();
	
	public static void addListener(StateChangeListener listener) {
		mListeners.add(listener);
		
		if (Pki.isMissing())
			listener.onPkiMissing();
		else if (Pki.isConnected()) {
			listener.onConnect();
			if (Pki.isLoggedIn())
				listener.onLogin();
		}
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
	}
	
	public static void notifyLogout() {
		for (StateChangeListener listener : mListeners)
			listener.onLogout();
	}

	public static void notifyDisconnect() {
		for (StateChangeListener listener : mListeners)
			listener.onDisconnect();
	}
	
	public static void notifyPkiMissing() {
		for (StateChangeListener listener : mListeners)
			listener.onPkiMissing();
	}
}
