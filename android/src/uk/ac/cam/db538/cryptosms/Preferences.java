package uk.ac.cam.db538.cryptosms;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
	private static final String PREFERECES = "SECURESMS_PREFS";
	private static Preferences mSingleton;
	
	static void initSingleton(Context context) {
		mSingleton = new Preferences(context);
	}
	
	public static Preferences getSingleton() {
		return mSingleton;
	}
	
	private SharedPreferences mPreferences;
	
	private Preferences(Context context) {
		mPreferences = context.getSharedPreferences(PREFERECES, Activity.MODE_PRIVATE);
	}
	
	// GETTERS / SETTERS
	
	public SharedPreferences getPreferences() {
		return mPreferences;
	}
}
