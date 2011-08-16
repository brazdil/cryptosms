package uk.ac.cam.db538.cryptosms.ui;

import roboguice.inject.InjectView;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;

public class ActivityExchangeViaText extends ActivityAppState {
	public static final String OPTION_PHONE_NUMBER = "PHONE_NUMBER";
	
	private Contact mContact;
	
	@InjectView(R.id.back)
	Button mBackButton;
	@InjectView(R.id.send)
	Button mSendButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_exchange_text_message);
		
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // set up the contact badge
        mContact = Contact.getContact(this, getIntent().getExtras().getString(OPTION_PHONE_NUMBER));
        UtilsContactBadge.setBadge(mContact, getMainView());
        
        // back button
        mBackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ActivityExchangeViaText.this.onBackPressed();
			}
		});

        // send button
        mSendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
	}
	
}
