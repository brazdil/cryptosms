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
package uk.ac.cam.db538.cryptosms.ui.activity;

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
import uk.ac.cam.db538.cryptosms.ui.UtilsContactBadge;

public class ActivityExchangeMethod extends ActivityAppState {
	public static final String OPTION_PHONE_NUMBER = "PHONE_NUMBER";
	
	private static final int ACTIVITY_TEXT_MESSAGE = 1;
	
	private Contact mContact;
	
	private String mPhoneNumber;
	
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
        mPhoneNumber = getIntent().getExtras().getString(OPTION_PHONE_NUMBER);
        if (mPhoneNumber == null)
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
