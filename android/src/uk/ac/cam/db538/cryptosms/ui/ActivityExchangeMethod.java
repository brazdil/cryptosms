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

public class ActivityExchangeMethod extends ActivityAppState {
	public static final String OPTION_PHONE_NUMBER = "PHONE_NUMBER";
	
	private Contact mContact;
	
	@InjectView(R.id.exchange_method_radio_text_message)
	RadioButton mTextMessageRadioButton;
	@InjectView(R.id.exchange_method_back)
	Button mBackButton;
	@InjectView(R.id.exchange_method_next)
	Button mNextButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_exchange_method);
		
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // set up the contact badge
        mContact = Contact.getContact(this, getIntent().getExtras().getString(OPTION_PHONE_NUMBER));
        UtilsContactBadge.setBadge(mContact, getMainView());
        
        // disable the Next button
        mNextButton.setEnabled(false);
        OnClickListener radioButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				mNextButton.setEnabled(true);
			}
		};
        mTextMessageRadioButton.setOnClickListener(radioButtonListener);
        
        // back button
        mBackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ActivityExchangeMethod.this.onBackPressed();
			}
		});

        // next button
        mNextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mTextMessageRadioButton.isChecked())
					Toast.makeText(ActivityExchangeMethod.this, "Text message", Toast.LENGTH_LONG).show();
			}
		});
	}
	
}
