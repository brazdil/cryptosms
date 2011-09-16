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
package uk.ac.cam.db538.cryptosms.data;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.ui.activity.ActivityLists;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

/*
 * Class that handles received text messages
 */
public class DataSmsReceiver extends BroadcastReceiver {

	public static final String SMS_RECEIVED="android.intent.action.DATA_SMS_RECEIVED";
	
	/**
	 * Instantiates a new data sms receiver.
	 */
	public DataSmsReceiver() {
		super();
	}

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
	    Resources res = context.getResources();
		
		DbPendingAdapter database = new DbPendingAdapter(context);
		database.open();

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
						Log.d(MyApplication.APP_TAG, "Received SMS: " + LowLevel.toHex(data) + "(" + data.length + " bytes)");
						if (data.length != MessageData.LENGTH_MESSAGE) {
							// TODO: ERROR!!!
						} else {
							// get the sender
							String phoneNumber = msg.getOriginatingAddress();
							// put it in the database
							database.insertEntry(new Pending(phoneNumber, data));
							
							// show notification
							NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
							Notification notification = MyApplication.getSingleton().getNotification();
							
							String expandedTitle = res.getString(R.string.notification_title);
							String expandedText = res.getString(R.string.notification_text);
							Intent startActivityIntent = new Intent(context, ActivityLists.class);
							startActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							PendingIntent launchIntent = PendingIntent.getActivity(context, 0, startActivityIntent, 0);
							notification.setLatestEventInfo(context, expandedTitle, expandedText, launchIntent);
							notification.when = System.currentTimeMillis();
							
							notificationManager.notify(MyApplication.NOTIFICATION_ID, notification);
							State.notifyNewEvent();
						}
					}
				}
			}
		}
		
		database.close();
	}
}
