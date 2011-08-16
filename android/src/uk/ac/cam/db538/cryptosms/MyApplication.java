package uk.ac.cam.db538.cryptosms;

import java.io.File;
import java.util.List;

import com.google.inject.Module;

import roboguice.application.RoboApplication;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionPki;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class MyApplication extends RoboApplication {
	private static short SMS_PORT; 
	public static final int NOTIFICATION_ID = 1;
	public static final String APP_TAG = "CRYPTOSMS";
	public static final String STORAGE_FILE_NAME = "storage.db";
	public static final String PKI_PACKAGE = "uk.ac.cam.dje38.pki";
	public static final String PKI_CONTACT_PICKER = "uk.ac.cam.dje38.pki.picker";
	public static final String NEWLINE = System.getProperty("line.separator");
	public static final String[] REPORT_EMAILS = new String[] { "db538@cam.ac.uk" }; // TODO: create new email!
	
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
		
		String storageFile = context.getFilesDir().getAbsolutePath() + "/" + MyApplication.STORAGE_FILE_NAME;

		//TODO: Just For Testing!!!
//		File file = new File(storageFile);
//		if (file.exists())
//			file.delete();
		File file2 = new File(context.getFilesDir().getAbsolutePath() + "/../databases/pending.db");
		if (file2.exists())
			file2.delete();
		
		Storage.setFilename(storageFile);
		
		Pki.init(this.getApplicationContext());
		SimCard.init(this.getApplicationContext());
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
