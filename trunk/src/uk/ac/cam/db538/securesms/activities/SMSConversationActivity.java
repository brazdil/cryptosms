package uk.ac.cam.db538.securesms.activities;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.R.id;
import uk.ac.cam.db538.securesms.R.integer;
import uk.ac.cam.db538.securesms.R.layout;
import uk.ac.cam.db538.securesms.R.string;
import uk.ac.cam.db538.securesms.database.DatabaseException;
import uk.ac.cam.db538.securesms.database.SMSHistoryAdapter;
import uk.ac.cam.db538.securesms.database.SMSHistoryEntry;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

public class SMSConversationActivity extends Activity {

	private static final String SENT_SMS_ACTION="SENT_SMS_ACTION";
	private static final String DELIVERED_SMS_ACTION="DELIVERED_SMS_ACTION";

	private Button buttonSend;
	private AutoCompleteTextView editContact;
	private EditText editMessageBody;
	
	private SMSHistoryAdapter adapterHistory;

	public SMSConversationActivity() {
		super();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.smsconversation);
		
		final Resources r = this.getResources();
		
		// Open connection to history database
		adapterHistory = new SMSHistoryAdapter(getApplicationContext());
		adapterHistory.open();
		
		// Values
		final short portDataSMS = (short) r.getInteger(R.integer.presets_data_sms_port);
		
		// Strings
		final String stringSending = r.getString(R.string.compose_sending);
		final String stringSendingErrorGeneral = r.getString(R.string.compose_sending_error_general);
		final String stringSendingErrorSave = r.getString(R.string.compose_sending_error_save);
		final String stringSendingErrorOK = r.getString(R.string.compose_sending_error_ok);
		
		// Layout views
		buttonSend = (Button) this.findViewById(R.id.compose_buttonSend);
		editContact = (AutoCompleteTextView) this.findViewById(R.id.compose_editContact);
		editMessageBody = (EditText) this.findViewById(R.id.compose_editMessageBody);

		// GSM Error dialog
		final AlertDialog.Builder dialogbuilderSendingError = new AlertDialog.Builder(this);
		dialogbuilderSendingError.setMessage(stringSendingErrorGeneral)
		                         .setCancelable(false)
		                         .setNeutralButton(stringSendingErrorOK, new DialogInterface.OnClickListener() {
		                             public void onClick(DialogInterface dialog, int id) {
		                                dialog.cancel();
		                             }
		                         });
	
		// Database Error dialog
		final AlertDialog.Builder dialogbuilderSavingError = new AlertDialog.Builder(this);
		dialogbuilderSavingError.setMessage(stringSendingErrorGeneral)
		                        .setCancelable(false)
		                        .setNeutralButton(stringSendingErrorOK, new DialogInterface.OnClickListener() {
		                            public void onClick(DialogInterface dialog, int id) {
		                                dialog.cancel();
		                            }
		                        });

		// Send button
		buttonSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog dialogSending = ProgressDialog.show(v.getContext(), "", stringSending);
				
				SmsManager sms = SmsManager.getDefault();
				final String phoneNumber = editContact.getText().toString();
				final String messageBody = editMessageBody.getText().toString();
				
				// Handler of sending status
				Intent sentIntent = new Intent(SENT_SMS_ACTION);
				PendingIntent sentPI = PendingIntent.getBroadcast(getApplicationContext(), 0, sentIntent, 0);
				registerReceiver(new BroadcastReceiver() {
						private boolean errorDialogShown = false;
						
						@Override
						public void onReceive(Context context, Intent intent) {
							dialogSending.dismiss();
							// TODO: react to SMS being delivered / error occurring 
							switch (getResultCode()) {
							case Activity.RESULT_OK:
								// Successfully sent
								// Save to history database
								try {
									Time time = new Time();
									time.setToNow();
									adapterHistory.insertEntry(new SMSHistoryEntry(0, 
									                                               phoneNumber, 
										                                           messageBody, 
										                                           time
										                                           ));
								}
								catch (DatabaseException ex) {
									errorDialogShown = true;
									AlertDialog dialog = dialogbuilderSavingError.create();
									dialog.show();
								}
								break;
							default:
								// ERROR!
								if (!errorDialogShown) {
									errorDialogShown = true;
									AlertDialog dialog = dialogbuilderSendingError.create();
									dialog.show();
								}
								break;
							}
						}
					},
					new IntentFilter(SENT_SMS_ACTION)
				);
				
				// Handler of delivery status
				/*Intent deliveredIntent = new Intent(DELIVERED_SMS_ACTION);
				PendingIntent deliveredPI = PendingIntent.getBroadcast(getApplicationContext(), 0, deliveredIntent, 0);
				registerReceiver(new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							// TODO: Handle deliveries
						}
					},
					new IntentFilter(DELIVERED_SMS_ACTION)
				);*/
				
				sms.sendDataMessage(phoneNumber, null, portDataSMS, messageBody.getBytes(), sentPI, null /*deliveredPI*/);
			}
		});
	}
}