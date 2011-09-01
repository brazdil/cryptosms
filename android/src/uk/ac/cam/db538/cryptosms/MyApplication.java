package uk.ac.cam.db538.cryptosms;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;

import javax.crypto.KeyAgreement;

import roboguice.application.RoboApplication;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionPki;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
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
	public static final String[] REPORT_EMAILS = new String[] { "db538@cam.ac.uk" }; // TODO: create new email!
	
	static {
	    Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
	}
	
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

		//TODO: Just For Testing!!!
//		File file = new File(storageFile);
//		if (file.exists())
//			file.delete();
//		File file2 = new File(context.getFilesDir().getAbsolutePath() + "/../databases/pending.db");
//		if (file2.exists())
//			file2.delete();
		
		Storage.setFilename(storageFile);
		
		Pki.init(this.getApplicationContext());
		SimCard.init(this.getApplicationContext());

		KeyPairGenerator keyGen = null;
		try {
			keyGen = KeyPairGenerator.getInstance("ECDH", "SC");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    EllipticCurve curve = new EllipticCurve(new ECFieldFp(new BigInteger(
	        "fffffffffffffffffffffffffffffffeffffffffffffffff", 16)), new BigInteger(
	        "fffffffffffffffffffffffffffffffefffffffffffffffc", 16), new BigInteger(
	        "fffffffffffffffffffffffffffffffefffffffffffffffc", 16));

	    ECParameterSpec ecSpec = new ECParameterSpec(curve, new ECPoint(new BigInteger(
	        "fffffffffffffffffffffffffffffffefffffffffffffffc", 16), new BigInteger(
	        "fffffffffffffffffffffffffffffffefffffffffffffffc", 16)), new BigInteger(
	        "fffffffffffffffffffffffffffffffefffffffffffffffc", 16), 1);

	    try {
			keyGen.initialize(ecSpec, new SecureRandom());
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    KeyAgreement aKeyAgree = null;
		try {
			aKeyAgree = KeyAgreement.getInstance("ECDH", "SC");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    KeyPair aPair = keyGen.generateKeyPair();
	    KeyAgreement bKeyAgree = null;
		try {
			bKeyAgree = KeyAgreement.getInstance("ECDH", "SC");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    KeyPair bPair = keyGen.generateKeyPair();

	    try {
			aKeyAgree.init(aPair.getPrivate());
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
			bKeyAgree.init(bPair.getPrivate());
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    try {
			aKeyAgree.doPhase(bPair.getPublic(), true);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
			bKeyAgree.doPhase(aPair.getPublic(), true);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    MessageDigest hash = null;
		try {
			hash = MessageDigest.getInstance("SHA1", "SC");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    Log.d(APP_TAG, LowLevel.toHex(hash.digest(aKeyAgree.generateSecret())));
	    Log.d(APP_TAG, LowLevel.toHex(hash.digest(bKeyAgree.generateSecret())));
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
