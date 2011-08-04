package uk.ac.cam.db538.securesms.receivers;

import java.util.ArrayList;

import uk.ac.cam.db538.securesms.MyApplication;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class DataSmsReceiver extends BroadcastReceiver {

	public static final String SMS_RECEIVED="android.intent.action.DATA_SMS_RECEIVED";
	
	public DataSmsReceiver() {
		super();
	}
	
	private void checkPending(String phoneNumber, Context context, DbPendingAdapter database) {
		ArrayList<Pending> pending =  database.getEntry(phoneNumber);

		// TODO: check whether we can put together any complete messages
		// from the user and potentially show notifications

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

					DbPendingAdapter database = new DbPendingAdapter(context);
					database.open();
					for (SmsMessage msg : messages) {
						// get the data in the message
						byte[] data = msg.getUserData();
						// get the sender
						String phoneNumber = msg.getOriginatingAddress();
						// put it in the database
						database.insertEntry(new Pending(phoneNumber, data));
						// check pending
						checkPending(phoneNumber, context, database);
					}
					database.close();
				}
			}
		}
	}
}
