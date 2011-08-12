package uk.ac.cam.db538.cryptosms;

import java.io.File;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionPki;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;

public class MyApplication extends Application {
	private static short SMS_PORT; 
	public static final int NOTIFICATION_ID = 1;
	public static final String APP_TAG = "CRYPTOSMS";
	public static final String STORAGE_FILE_NAME = "storage.db";
	public static final String PKI_PACKAGE = "uk.ac.cam.dje38.pki";
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
		
		Preferences.initSingleton(context);
		if (Encryption.getEncryption() == null)
			Encryption.setEncryption(new EncryptionPki());
		
		String storageFile = context.getFilesDir().getAbsolutePath() + "/" + MyApplication.STORAGE_FILE_NAME;

		//TODO: Just For Testing!!!
		File file = new File(storageFile);
		if (file.exists())
			file.delete();
		file = new File(context.getFilesDir().getAbsolutePath() + "/../databases/pending.db");
		if (file.exists())
			file.delete();
		
		Storage.setFilename(storageFile);
		
		Pki.init(this.getApplicationContext());
	}
	
	public Notification getNotification() {
		return mNotification;
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		Pki.disconnect();
	}
}
