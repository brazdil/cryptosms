package uk.ac.cam.db538.securesms;

import android.app.Activity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SMSComposeActivity extends Activity {

	public SMSComposeActivity() {
		super();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.smscompose);
		
		final Button buttonSend = (Button) this.findViewById(R.id.compose_buttonSend);
		final EditText editPhoneNumber = (EditText) this.findViewById(R.id.compose_editTo);
		final EditText editMessageBody = (EditText) this.findViewById(R.id.compose_editMessageBody);
		
		buttonSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SmsManager sms = SmsManager.getDefault();
				sms.sendTextMessage(editPhoneNumber.getText().toString(), null, editMessageBody.getText().toString(), null, null);
			}
		});
	}
}
