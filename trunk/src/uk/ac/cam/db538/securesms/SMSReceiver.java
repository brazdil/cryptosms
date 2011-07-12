package uk.ac.cam.db538.securesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class SMSReceiver extends BroadcastReceiver {

	public static final String SMS_RECEIVED="android.provider.Telephony.SMS_RECEIVED";
	
	public SMSReceiver() {
		super();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(SMS_RECEIVED)) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				SmsMessage[] messages = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; ++i)
					messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				for (SmsMessage msg : messages) {
					byte[] data = msg.getUserData();
					Toast.makeText(context, msg.getOriginatingAddress() + ": " + msg.getMessageBody(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}
}
