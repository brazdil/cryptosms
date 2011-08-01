package uk.ac.cam.db538.securesms;

import uk.ac.cam.db538.securesms.storage.Storage;
import android.app.Application;

public class MyApplication extends Application {
	private static MyApplication mSingleton;
	
	public static MyApplication getSingleton() {
		return mSingleton;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mSingleton = this;
		try {
			Storage.initSingleton(this);
			Preferences.initSingleton(this);
		} catch (Exception e) {
			// TODO: show error dialog
		}
	}
}
