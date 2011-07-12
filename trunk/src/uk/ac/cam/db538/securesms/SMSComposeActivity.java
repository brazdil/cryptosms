package uk.ac.cam.db538.securesms;

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
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SMSComposeActivity extends Activity {

	private final String SENT_SMS_ACTION="SENT_SMS_ACTION";
	private final String DELIVERED_SMS_ACTION="DELIVERED_SMS_ACTION";
	
	public SMSComposeActivity() {
		super();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.smscompose);
		
		final Resources r = this.getResources();
		
		final short portDataSMS = (short) r.getInteger(R.integer.presets_data_sms_port);
		
		final String stringSending = r.getString(R.string.compose_sending);
		final String stringSendingErrorGeneral = r.getString(R.string.compose_sending_error_general);
		final String stringSendingErrorOK = r.getString(R.string.compose_sending_error_ok);
		
		final Button buttonSend = (Button) this.findViewById(R.id.compose_buttonSend);
		final EditText editPhoneNumber = (EditText) this.findViewById(R.id.compose_editTo);
		final EditText editMessageBody = (EditText) this.findViewById(R.id.compose_editMessageBody);

		final AlertDialog.Builder dialogbuilderSendingError = new AlertDialog.Builder(this);
		dialogbuilderSendingError.setMessage(stringSendingErrorGeneral)
		       .setCancelable(false)
		       .setNeutralButton(stringSendingErrorOK, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		
		buttonSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog dialogSending = ProgressDialog.show(v.getContext(), "", stringSending);

				SmsManager sms = SmsManager.getDefault();
				
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
				Intent deliveredIntent = new Intent(DELIVERED_SMS_ACTION);
				PendingIntent deliveredPI = PendingIntent.getBroadcast(getApplicationContext(), 0, deliveredIntent, 0);
				registerReceiver(new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							// TODO: Handle deliveries
						}
					},
					new IntentFilter(DELIVERED_SMS_ACTION)
				);
				
				sms.sendDataMessage(editPhoneNumber.getText().toString(), null, portDataSMS, editMessageBody.getText().toString().getBytes(), sentPI, deliveredPI);
			}
		});
	}
}
