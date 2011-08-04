package uk.ac.cam.db538.securesms.receivers;

import uk.ac.cam.db538.securesms.MyApplication;
import uk.ac.cam.db538.securesms.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class SMSReceiver extends BroadcastReceiver {

	public static final String SMS_RECEIVED="android.intent.action.DATA_SMS_RECEIVED";
	
	public SMSReceiver() {
		super();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(SMS_RECEIVED)) {
			// check the port number
			String[] uri = intent.getDataString().split(":");
			int port = Integer.parseInt(uri[uri.length - 1]);
			if (port == MyApplication.getSmsPort())
			{
				// for each message
				Bundle bundle = intent.getExtras();
				if (bundle != null) {
					Object[] pdus = (Object[]) bundle.get("pdus");
					SmsMessage[] messages = new SmsMessage[pdus.length];
					for (int i = 0; i < pdus.length; ++i)
						messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
					for (SmsMessage msg : messages) {
						// get the data in the message
						byte[] data = msg.getUserData();
						String messageBody = new String(data);
						String phoneNumber = msg.getOriginatingAddress();
						
						// show it on screen
						Toast.makeText(context, phoneNumber + ": " + messageBody, Toast.LENGTH_LONG).show();
					}
				}
			}
		}
	}
}
