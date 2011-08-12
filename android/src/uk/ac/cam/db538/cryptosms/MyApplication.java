package uk.ac.cam.db538.cryptosms;

import java.io.File;
import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionNone;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionPki;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.data.TextMessage;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys.SimNumber;
import uk.ac.cam.db538.cryptosms.ui.PkiInstallActivity;
import uk.ac.cam.db538.cryptosms.utils.CompressedText;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.ConnectionListener;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.NotConnectedException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.PKIErrorException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.PKInotInstalledException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.TimeoutException;
import android.app.Application;
import android.app.Notification;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.util.Log;

public class MyApplication extends Application {
	private static short SMS_PORT; 
	public static final int NOTIFICATION_ID = 1;
	public static final String APP_TAG = "CRYPTOSMS";
	private static final String STORAGE_FILE_NAME = "storage.db";
	
	private static MyApplication mSingleton;
	
	public static MyApplication getSingleton() {
		return mSingleton;
	}
	
	public static short getSmsPort() {
		return SMS_PORT;
	}

	private Notification mNotification = null;
	private PKIwrapper mPki = null;
	
	//private final Context mContext = this.getApplicationContext();
	private ConnectionListener onPkiConnect;

	private void initPki() {
		if (mPki != null) return;
		
		try {
			mPki = new PKIwrapper(this.getApplicationContext());
		} catch (InterruptedException e1) {
			// ignore
		}
		try {
			if (mPki != null) {
				mPki.setTimeout(60);
				mPki.connect(onPkiConnect);
			}
		} catch (PKInotInstalledException e) {
			mPki = null;
		}
	}
	
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
	
		onPkiConnect = new ConnectionListener() {
				@Override
				public void onConnect() {
					Log.d(APP_TAG, "onConnect");
					PkiStateReceiver.notifyConnect();
					
					String storageFile = context.getFilesDir().getAbsolutePath() + "/" + STORAGE_FILE_NAME;
					
					//TODO: Just For Testing!!!
					File file = new File(storageFile);
					if (file.exists())
						file.delete();
					file = new File(context.getFilesDir().getAbsolutePath() + "/../databases/pending.db");
					if (file.exists())
						file.delete();

					try {
						Storage.initSingleton(storageFile);
					} catch (Exception e) {
						// TODO: show error dialog
					}
					
					try {
						Conversation conv1 = Conversation.createConversation();
						conv1.setPhoneNumber("+447572306095");
						SessionKeys keys1 = SessionKeys.createSessionKeys(conv1);
						keys1.setSimNumber(new SimNumber("89441000301641313004", true));
						keys1.setKeysSent(true);
						keys1.setKeysConfirmed(false);
						keys1.saveToFile();
						SessionKeys keys4 = SessionKeys.createSessionKeys(conv1);
						keys4.setSimNumber(new SimNumber("07879116797", false));
						keys4.setKeysSent(true);
						keys4.setKeysConfirmed(true);
						keys4.saveToFile();
						SessionKeys keys5 = SessionKeys.createSessionKeys(conv1);
						keys5.setSimNumber(new SimNumber("07572306095", false));
						keys5.setKeysSent(true);
						keys5.setKeysConfirmed(true);
						keys5.setSessionKey_In(keys5.getSessionKey_Out().clone());
						keys5.saveToFile();
						Conversation conv2 = Conversation.createConversation();
						MessageData msg2 = MessageData.createMessageData(conv2);
						TextMessage txtmsg2 = new TextMessage(msg2);
						txtmsg2.setText(CompressedText.createFromString("You're a jerk!"));
						msg2.setUnread(false);
						msg2.saveToFile();
						conv2.setPhoneNumber("+20104544366");
						SessionKeys keys2 = SessionKeys.createSessionKeys(conv2);
						keys2.setSimNumber(new SimNumber("89441000301641313002", true));
						keys2.setKeysSent(false);
						keys2.setKeysConfirmed(true);
						keys2.saveToFile();
						SessionKeys keys3 = SessionKeys.createSessionKeys(conv2);
						keys3.setSimNumber(new SimNumber("07879116797", false));
						keys3.setKeysSent(false);
						keys3.setKeysConfirmed(false);
						keys3.saveToFile();
						Conversation conv3 = Conversation.createConversation();
						conv3.setPhoneNumber("+447879116797");
						SessionKeys keys6 = SessionKeys.createSessionKeys(conv3);
						keys6.setSimNumber(new SimNumber("+447572306095", false));
						keys6.setKeysSent(true);
						keys6.setKeysConfirmed(true);
						keys6.saveToFile();
					} catch (Exception ex) {
					}
				}		

				@Override
				public void onConnectionDeclined() {
					// TODO Auto-generated method stub
					Log.d(APP_TAG, "onConnectionDeclined");
					
				}
				@Override
				public void onConnectionFailed() {
					// TODO Auto-generated method stub
					Log.d(APP_TAG, "onConnectionFailed");
				}
				@Override
				public void onConnectionTimeout() {
					// TODO Auto-generated method stub
					Log.d(APP_TAG, "onConnectionTimeout");
				}
				@Override
				public void onDisconnect() {
					Log.d(APP_TAG, "onDisconnect");
					mPki = null; 
					PkiStateReceiver.notifyDisconnect();
				}
		};
		initPki();
	}
	
	public Notification getNotification() {
		return mNotification;
	}
	
	public PKIwrapper getPki() {
		return mPki;
	}
	
	public void loginPki() {
		if (mPki != null && mPki.isConnected() && !PkiStateReceiver.isLoggedIn()) {
			Log.d(MyApplication.APP_TAG, "Logging in");
			Intent intent = new Intent("uk.ac.cam.dje38.pki.login");
			this.getApplicationContext().startService(intent);
		}
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		if (mPki != null)
			try {
				mPki.disconnect();
			} catch (TimeoutException e) {
			} catch (PKIErrorException e) {
			}
	}
}
