package uk.ac.cam.db538.cryptosms.ui;

import roboguice.inject.InjectView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.state.Pki;

public class ActivityExchangeMethod extends ActivityAppState {
	public static final String OPTION_CONTACT_ID = "CONTACT_ID";
	public static final String OPTION_CONTACT_KEY = "CONTACT_KEY";
	public static final String OPTION_PHONE_NUMBER = "PHONE_NUMBER";
	
	private static final int ACTIVITY_TEXT_MESSAGE = 1;
	
	private Contact mContact;
	
	private long mContactId;
	private String mPhoneNumber;
	private String mContactKey;
	
	@InjectView(R.id.via_text)
	RadioButton mTextMessageRadioButton;
	@InjectView(R.id.back)
	Button mBackButton;
	@InjectView(R.id.next)
	Button mNextButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_exchange_method);
		
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // set up the recipient
        mContactId = getIntent().getExtras().getLong(OPTION_CONTACT_ID, -1L);
        mPhoneNumber = getIntent().getExtras().getString(OPTION_PHONE_NUMBER);
        mContactKey = getIntent().getExtras().getString(OPTION_CONTACT_KEY);
        if (mContactId == -1L || mPhoneNumber == null || mContactKey == null)
        	this.finish();
        
        mContact = Contact.getContact(this, mPhoneNumber);
        if (mContact == null || !mContact.existsInDatabase())
        	this.finish();
        
        // set up the contact badge
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
				if (mTextMessageRadioButton.isChecked()) {
	    			Intent intent = new Intent(ActivityExchangeMethod.this, ActivityExchangeViaText.class);
	    			intent.putExtra(ActivityExchangeViaText.OPTION_PHONE_NUMBER, mPhoneNumber);
	    			intent.putExtra(ActivityExchangeViaText.OPTION_CONTACT_ID, mContactId);
	    			intent.putExtra(ActivityExchangeViaText.OPTION_CONTACT_KEY, mContactKey);
	    			startActivityForResult(intent, ACTIVITY_TEXT_MESSAGE);
				}
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
    	if (!Pki.isLoggedIn())
    		return;
	
    	switch (requestCode) {
    	case ACTIVITY_TEXT_MESSAGE:
    		if (resultCode == Activity.RESULT_OK) 
    			this.finish();
    		break;
    	}
	}
	
}
