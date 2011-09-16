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

import java.lang.Thread.UncaughtExceptionHandler;

import roboguice.application.RoboApplication;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionPki;
import uk.ac.cam.db538.cryptosms.data.PendingParser;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class MyApplication extends RoboApplication {
	private static short SMS_PORT; 
	public static final int NOTIFICATION_ID = 1;
	public static final String APP_TAG = "CRYPTOSMS";
	public static final String STORAGE_FILE_NAME = "storage.db";
	
	public static final String PKI_PACKAGE = "uk.ac.cam.dje38.pki";
	public static final String PKI_CONTACT_PICKER = "uk.ac.cam.dje38.pki.picker";
	public static final String PKI_KEY_PICKER = "uk.ac.cam.dje38.pki.keypicker";
	public static final String PKI_LOGIN = "uk.ac.cam.dje38.pki.login";
	
	public static final String NEWLINE = System.getProperty("line.separator");
	public static final String[] REPORT_EMAILS = new String[] { }; // TODO: create new email!
	
	private static MyApplication mSingleton;
	
	public static MyApplication getSingleton() {
		return mSingleton;
	}
	
	public static short getSmsPort() {
		return SMS_PORT;
	}

	private Notification mNotification = null;
	private Drawable mDefaultContactImage = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mSingleton = this;
		final Context context = this.getApplicationContext();
		Resources res = this.getResources();
		
		SMS_PORT = (short) res.getInteger(R.integer.presets_data_sms_port);
		
		int icon = R.drawable.icon_notification;
		String tickerText = res.getString(R.string.notification_ticker);
		long when = System.currentTimeMillis();
		mNotification = new Notification(icon, tickerText, when);
		
		mDefaultContactImage = getResources().getDrawable(R.drawable.ic_contact_picture);
		
		Preferences.initSingleton(context);
		if (Encryption.getEncryption() == null)
			Encryption.setEncryption(new EncryptionPki());
		
		final UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			private void logException(Throwable ex) {
				Log.e(APP_TAG, "Exception: " + ex.getClass().getName());
				Log.e(APP_TAG, "Message: " + ex.getMessage());
				Log.e(APP_TAG, "Stack: ");
				for (StackTraceElement element : ex.getStackTrace())
					Log.e(APP_TAG, element.toString());
				if (ex.getCause() != null) {
					Log.e(APP_TAG, "Cause: ");
					logException(ex.getCause());
				}
			}
			
			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				if ((ex instanceof WrongKeyDecryptionException) ||
					(ex instanceof RuntimeException && ex.getCause() instanceof WrongKeyDecryptionException)) {
					// TODO: Handle better
					logException(ex);
					State.fatalException((WrongKeyDecryptionException) ex);
				}
				else
					defaultHandler.uncaughtException(thread, ex);
			}
		});

		String storageFile = context.getFilesDir().getAbsolutePath() + "/" + MyApplication.STORAGE_FILE_NAME;

		Storage.setFilename(storageFile);
		
		Pki.init(this.getApplicationContext());
		SimCard.init(this.getApplicationContext());
		PendingParser.init(this.getApplicationContext());
	}
	
	public Notification getNotification() {
		return mNotification;
	}
	
	public Drawable getDefaultContactImage() {
		return mDefaultContactImage;
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		Pki.disconnect();
	}
}
