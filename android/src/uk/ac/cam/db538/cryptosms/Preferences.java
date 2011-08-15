package uk.ac.cam.db538.cryptosms;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
	public static final String PREFERECES = "CRYPTOSMS_PREFS";
	public static final String PREFERENCES_IMPORT_NEVER = "IMPORT_NEVER";
	
	private static Preferences mSingleton;
	
	static void initSingleton(Context context) {
		mSingleton = new Preferences(context);
	}
	
	public static Preferences getSingleton() {
		return mSingleton;
	}
	
	private SharedPreferences mPreferences;
	
	private Preferences(Context context) {
		mPreferences = context.getSharedPreferences(PREFERECES, Context.MODE_PRIVATE);
	}
	
	// GETTERS / SETTERS
	
	public SharedPreferences getPreferences() {
		return mPreferences;
	}
}
