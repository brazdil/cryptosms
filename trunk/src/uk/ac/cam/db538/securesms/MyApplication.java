package uk.ac.cam.db538.securesms;

import java.io.File;

import uk.ac.cam.db538.securesms.storage.Conversation;
import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.SessionKeys;
import uk.ac.cam.db538.securesms.storage.Storage;
import uk.ac.cam.db538.securesms.storage.SessionKeys.SimNumber;
import android.app.Application;

public class MyApplication extends Application {
	private static short SMS_PORT; 
	private static MyApplication mSingleton;
	
	public static MyApplication getSingleton() {
		return mSingleton;
	}
	
	public static short getSmsPort() {
		return SMS_PORT;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mSingleton = this;
		SMS_PORT = (short) this.getResources().getInteger(R.integer.presets_data_sms_port);
		
		//TODO: Just For Testing!!!
		File file = new File("/data/data/uk.ac.cam.db538.securesms/files/storage.db");
		if (file.exists())
			file.delete();

		try {
			Storage.initSingleton(this);
			Preferences.initSingleton(this);
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
			keys5.saveToFile();
			Conversation conv2 = Conversation.createConversation();
			MessageData msg2 = MessageData.createMessageData(conv2);
			//msg2.setMessageBody("You're a jerk!");
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
}
