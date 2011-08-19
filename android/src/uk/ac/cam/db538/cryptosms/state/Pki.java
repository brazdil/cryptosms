package uk.ac.cam.db538.cryptosms.state;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.state.State.StateChangeListener;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.BadInputException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.ConnectionListener;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.DeclinedException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.NotConnectedException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.PKIErrorException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.PKInotInstalledException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.TimeoutException;

public class Pki {
	private static final String INTENT_PKI_LOGIN = "uk.ac.cam.dje38.pki.login";
	private static final String INTENT_PKI_LOGOUT = "uk.ac.cam.dje38.pki.logout";
	
	private static final int TIMEOUT_DEFAULT = 60;
	private static final int TIMEOUT_LOGIN_CHECK = 4;

	private static PKIwrapper mPki = null;
	private static Context mContext = null;
	
	private static byte[] mMasterKey = null;
	private static final String KEY_STORAGE = "CRYPTOSMS_MASTER_KEY";

	private static boolean mMissing = false;
	private static boolean mLoggedIn = false;
	
	public static class PkiNotReadyException extends Exception {
		private static final long serialVersionUID = 5247215307724480706L;
		
		public PkiNotReadyException() {
			super("PKI is not available (either not connected or logged out)");
		}
	}

//	private static boolean dbInitialized = false;
	private static StateChangeListener mMainListener = new StateChangeListener() {
		@Override
		public void onLogin() {
//			// TODO Just for testing!!!
//			if (!dbInitialized) {
//				dbInitialized = true;
//				try {
//					Conversation conv1 = Conversation.createConversation();
//					conv1.setPhoneNumber("+447572306095");
//					SessionKeys keys1 = SessionKeys.createSessionKeys(conv1);
//					keys1.setSimNumber(new SimNumber("89441000301641313004", true));
//					keys1.setKeysSent(true);
//					keys1.setKeysConfirmed(false);
//					keys1.saveToFile();
//					SessionKeys keys4 = SessionKeys.createSessionKeys(conv1);
//					keys4.setSimNumber(new SimNumber("07879116797", false));
//					keys4.setKeysSent(true);
//					keys4.setKeysConfirmed(true);
//					keys4.saveToFile();
//					SessionKeys keys5 = SessionKeys.createSessionKeys(conv1);
//					keys5.setSimNumber(new SimNumber("07572306095", false));
//					keys5.setKeysSent(true);
//					keys5.setKeysConfirmed(true);
//					keys5.setSessionKey_In(keys5.getSessionKey_Out().clone());
//					keys5.saveToFile();
//					Conversation conv2 = Conversation.createConversation();
//					MessageData msg2 = MessageData.createMessageData(conv2);
//					TextMessage txtmsg2 = new TextMessage(msg2);
//					txtmsg2.setText(CompressedText.createFromString("You're a jerk!"));
//					msg2.setUnread(false);
//					msg2.saveToFile();
//					conv2.setPhoneNumber("+20104544366");
//					SessionKeys keys2 = SessionKeys.createSessionKeys(conv2);
//					keys2.setSimNumber(new SimNumber("89441000301641313002", true));
//					keys2.setKeysSent(false);
//					keys2.setKeysConfirmed(true);
//					keys2.saveToFile();
//					SessionKeys keys3 = SessionKeys.createSessionKeys(conv2);
//					keys3.setSimNumber(new SimNumber("07879116797", false));
//					keys3.setKeysSent(false);
//					keys3.setKeysConfirmed(false);
//					keys3.saveToFile();
//					Conversation conv3 = Conversation.createConversation();
//					conv3.setPhoneNumber("+447879116797");
//					SessionKeys keys6 = SessionKeys.createSessionKeys(conv3);
//					keys6.setSimNumber(new SimNumber("+447572306095", false));
//					keys6.setKeysSent(true);
//					keys6.setKeysConfirmed(true);
//					keys6.saveToFile();
//				} catch (Exception ex) {
//				}
//			}
		}
		
		@Override
		public void onConnect() {
		}

		@Override
		public void onDisconnect() {
		}
		
		@Override
		public void onPkiMissing() {
		}

		@Override
		public void onLogout() {
		}

		@Override
		public void onFatalException(Exception ex) {
		}

		@Override
		public void onSimState() {
		}

		@Override
		public void onNewEvent() {
		}
	};
	
	public static void init(Context context) {
		mContext = context;
		if (mPki != null) return;

		IntentFilter filterLogin = new IntentFilter();
		filterLogin.addAction(INTENT_PKI_LOGIN);
		filterLogin.addAction(INTENT_PKI_LOGOUT);
		context.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(MyApplication.APP_TAG, "Intent: " + intent.getAction());
				if (intent.getAction().equals(INTENT_PKI_LOGIN)) {
					mPki.setTimeout(TIMEOUT_LOGIN_CHECK);
					try {
						Pki.getMasterKey(true); // calls authorize
						Pki.setLoggedIn(true);
					} catch (PkiNotReadyException e) {
						// if this happens then setLoggedIn didn't
					}
					mPki.setTimeout(TIMEOUT_DEFAULT);
				} else if (intent.getAction().equals(INTENT_PKI_LOGOUT))
					Pki.setLoggedIn(false);
			}
		}, filterLogin);
		
		IntentFilter filterInstall = new IntentFilter();
		filterInstall.addAction(Intent.ACTION_PACKAGE_ADDED);
		filterInstall.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filterInstall.addCategory("android.intent.category.DEFAULT");
		filterInstall.addDataScheme("package");
		filterInstall.addDataPath("uk.ac.cam.dje38.pki", 0);
		context.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
					if (intent.getDataString().equals("package:" + MyApplication.PKI_PACKAGE))
						Pki.setMissing();
				} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
					if (intent.getDataString().equals("package:" + MyApplication.PKI_PACKAGE))
						Pki.connect();
				}
			}
		}, filterInstall);
		
		try {
			mPki = new PKIwrapper(mContext);
		} catch (InterruptedException e1) {
			// ignore
		}

		connect();
	}
	
	public static PKIwrapper getPkiWrapper() {
		return mPki;
	}
	
	public static void connect() {
		try {
			if (mPki != null) {
				mPki.setTimeout(TIMEOUT_DEFAULT);
				mPki.connect(new ConnectionListener() {
					
					@Override
					public void onConnectionDeclined() {
						// TODO Auto-generated method stub
						Log.d(MyApplication.APP_TAG, "onConnectionDeclined");
						
					}
					
					@Override
					public void onConnectionFailed() {
						// TODO Auto-generated method stub
						Log.d(MyApplication.APP_TAG, "onConnectionFailed");
					}
					
					@Override
					public void onConnectionTimeout() {
						// TODO Auto-generated method stub
						Log.d(MyApplication.APP_TAG, "onConnectionTimeout");
					}
					
					@Override
					public void onDisconnect() {
						Log.d(MyApplication.APP_TAG, "onDisconnect");
						if (!State.isInFatalState()) {
							setLoggedIn(false);
							State.notifyDisconnect();
							State.removeListener(mMainListener);
						}
					}
					
					@Override
					public void onConnect() {
						Log.d(MyApplication.APP_TAG, "onConnect");
						State.notifyConnect();
						setLoggedIn(false, true);
						login(false);
						State.addListener(mMainListener);
					}
				});
				mMissing = false;
			}
		} catch (PKInotInstalledException e) {
			mMissing = true;
			State.notifyPkiMissing();
		}
	}

	public static boolean isConnected() {
		return !State.isInFatalState() && mPki != null && mPki.isConnected();
	}
	
	public static void login(boolean force) {
		Log.d(MyApplication.APP_TAG, "Login: " + (isConnected() ? "connected" : "disconnected") + ", " + (isLoggedIn() ? "logged in" : "logged out") + ", " + (force ? "forced" : "unforced") );
		if (isConnected() && !isLoggedIn()) {
			if (force) {
				try {
					mPki.setTimeout(TIMEOUT_DEFAULT);
					setLoggedIn(mPki.authorise());
				} catch (TimeoutException e) {
				} catch (PKIErrorException e) {
				} catch (NotConnectedException e) {
				}
			} else {
				setLoggedIn(false);
				Intent intent = new Intent(MyApplication.PKI_LOGIN);
				mContext.startService(intent);
			}
		} else if (isLoggedIn())
			State.notifyLogin();
	}

	public static void disconnect() {
		if (mPki != null)
			try {
				mPki.disconnect();
			} catch (TimeoutException e) {
			} catch (PKIErrorException e) {
		}
	}
	
	static void setMissing() {
		Log.d(MyApplication.APP_TAG, "PKI missing");
		mMissing = true;
		State.notifyPkiMissing();
	}

	static boolean isMissing() {
		return mMissing;
	}

	public static boolean isLoggedIn() {
		return isConnected() && mLoggedIn;
	}
	
	static void setLoggedIn(boolean value) {
		setLoggedIn(value, false);
	}
	
	static void setLoggedIn(boolean value, boolean forceNotify) {
		if (mLoggedIn != value || forceNotify) {
			mLoggedIn = value;
			if (mLoggedIn) 
				State.notifyLogin();
			else {
				mMasterKey = null; // forget the master key
				State.notifyLogout();
			}
		}
	}
	
	public static byte[] getMasterKey(boolean forceLogIn) throws PkiNotReadyException {
		byte[] key = getMasterKey(forceLogIn, true);
		return key;
	}
	
	public static byte[] getMasterKey(boolean forceLogIn, boolean generateAllow) throws PkiNotReadyException {
		if (isLoggedIn()) {
			if (mMasterKey == null) {
				try {
					if (mPki.hasDataStore(KEY_STORAGE)) {
						mMasterKey = mPki.getDataStore(KEY_STORAGE);
						return mMasterKey;
					}
					else if (generateAllow) {
						mPki.setDataStore(KEY_STORAGE, Encryption.getEncryption().generateRandomData(Encryption.SYM_KEY_LENGTH));
						return getMasterKey(forceLogIn, false);
					} else
						throw new PkiNotReadyException();
				} catch (NotConnectedException e) {
					throw new PkiNotReadyException();
				} catch (TimeoutException e) {
					throw new PkiNotReadyException();
				} catch (DeclinedException e) {
					throw new PkiNotReadyException();
				} catch (PKIErrorException e) {
					throw new PkiNotReadyException();
				} catch (BadInputException e) {
					throw new PkiNotReadyException();
				}
			} else
				return mMasterKey;
		} else if (isConnected() && forceLogIn) {
			Pki.login(true);
			// no need to alter our own login information - broadcast will be sent by PKI
			return getMasterKey(false, generateAllow);
		} else
			throw new PkiNotReadyException();
	}
}
