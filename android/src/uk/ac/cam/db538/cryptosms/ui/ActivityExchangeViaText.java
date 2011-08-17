package uk.ac.cam.db538.cryptosms.ui;

import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.KeysMessage;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageSendingListener;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;

public class ActivityExchangeViaText extends ActivityAppState {
	public static final String OPTION_CONTACT_ID = "CONTACT_ID";
	public static final String OPTION_CONTACT_KEY = "CONTACT_KEY";
	public static final String OPTION_PHONE_NUMBER = "PHONE_NUMBER";
	
	private Contact mContact;

	private long mContactId;
	private String mPhoneNumber;
	private String mContactKey;
	
	@InjectView(R.id.sms_count)
	TextView mSmsCountView;
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

        // set up the recipient
        mContactId = getIntent().getExtras().getLong(OPTION_CONTACT_ID, -1L);
        mPhoneNumber = getIntent().getExtras().getString(OPTION_PHONE_NUMBER);
        mContactKey = getIntent().getExtras().getString(OPTION_CONTACT_KEY);
        if (mContactId == -1L || mPhoneNumber == null || mContactKey == null)
        	this.finish();

        mContact = Contact.getContact(this, mPhoneNumber, mContactId);
        if (mContact == null || !mContact.existsInDatabase())
        	this.finish();
        
        // number of texts
        try {
			mSmsCountView.setText(Integer.toString(KeysMessage.getPartsCount()));
		} catch (MessageException e) {
			State.fatalException(e);
			return;
		}
        
        // set up the contact badge
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
				mSendButton.setEnabled(false);
				mBackButton.setEnabled(false);
				UtilsKeyExchange.sendKeys(mPhoneNumber, mContactId, mContactKey, ActivityExchangeViaText.this, getDialogManager(), new MessageSendingListener() {
					@Override
					public boolean onPartSent(int index) {
						return true;
					}
					
					@Override
					public void onMessageSent() {
						// go back
						ActivityExchangeViaText.this.startActivity(new Intent(ActivityExchangeViaText.this, ActivityLists.class));
					}
					
					@Override
					public void onError(String message) {
						mSendButton.setEnabled(true);
						mBackButton.setEnabled(true);
					}
				});
			}
		});
        
        // prepare dialogs
        UtilsKeyExchange.prepareDialogs(getDialogManager(), ActivityExchangeViaText.this);
	}
}
