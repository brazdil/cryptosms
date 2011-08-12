package uk.ac.cam.db538.cryptosms;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PkiStateReceiver extends BroadcastReceiver {
	
	private static final String INTENT_PKI_LOGIN = "uk.ac.cam.dje38.pki.login";
	private static final String INTENT_PKI_LOGOUT = "uk.ac.cam.dje38.pki.logout";

	public interface PkiStateListener {
		public void onConnect();
		public void onLogin();
		public void onLogout();
		public void onDisconnect();
	}
	
	private static ArrayList<PkiStateListener> mListeners = new ArrayList<PkiStateListener>();
	
	public static void addListener(PkiStateListener listener) {
		mListeners.add(listener);
		
		if (MyApplication.getSingleton().getPki().isConnected()) {
			listener.onConnect();
			if (isLoggedIn())
				listener.onLogin();
		}
	}
	
	public static void removeListener(PkiStateListener listener) {
		mListeners.remove(listener);
	}
	
	public static void notifyConnect() {
		mLoggedIn = false;
		for (PkiStateListener listener : mListeners)
			listener.onConnect();
		MyApplication.getSingleton().loginPki();
	}
	
	public static void notifyLogin() {
		for (PkiStateListener listener : mListeners)
			listener.onLogin();
	}
	
	public static void notifyLogout() {
		for (PkiStateListener listener : mListeners)
			listener.onLogout();
	}

	public static void notifyDisconnect() {
		mLoggedIn = false;
		for (PkiStateListener listener : mListeners)
			listener.onDisconnect();
	}
	
	private static boolean mLoggedIn = false;
	
	public static boolean isLoggedIn() {
		return mLoggedIn;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(INTENT_PKI_LOGIN)) {
			mLoggedIn = true;
			notifyLogin();
		}
		else if (intent.getAction().equals(INTENT_PKI_LOGOUT)) {
			mLoggedIn = false;
			notifyLogout();
		}
	}

}
