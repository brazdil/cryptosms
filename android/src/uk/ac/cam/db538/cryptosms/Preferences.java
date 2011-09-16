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
package uk.ac.cam.db538.cryptosms;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
	public static final String PREFERECES = "CRYPTOSMS_PREFS";
	public static final String PREFERENCES_IMPORT_NEVER = "IMPORT_NEVER";
	
	private static Preferences mSingleton;
	
	/**
	 * Inits the singleton.
	 *
	 * @param context the context
	 */
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
