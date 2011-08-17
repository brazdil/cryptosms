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
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;
import uk.ac.cam.db538.cryptosms.utils.SimNumber;

public class ActivityExchangeViaText extends ActivityAppState {
	public static final String OPTION_CONTACT_ID = "CONTACT_ID";
	public static final String OPTION_CONTACT_KEY = "CONTACT_KEY";
	public static final String OPTION_PHONE_NUMBER = "PHONE_NUMBER";
	
	private static final String DIALOG_SENDING = "DIALOG_EXCHANGE_SENDING";
	private static final String DIALOG_SENDING_ERROR = "DIALOG_EXCHANGE_SENDING_ERROR";
	private static final String PARAM_SENDING_ERROR = "PARAM_EXCHANGE_SENDING_ERROR";
	
	private static boolean mCancelled = false;
	
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
		    	getDialogManager().showDialog(DIALOG_SENDING, null);
				
				// generate session keys
				final KeysMessage keysMessage = new KeysMessage(mContactId, mContactKey);

				// send the message
				try {
					mCancelled = false;
					keysMessage.sendSMS(mPhoneNumber, ActivityExchangeViaText.this, new MessageSendingListener() {
						@Override
						public void onMessageSent() {
							Log.d(MyApplication.APP_TAG, "Sent all!");
							getDialogManager().dismissDialog(DIALOG_SENDING);
							
							try {
								Conversation conv = Conversation.getConversation(mPhoneNumber);
								if (conv == null) {
									conv = Conversation.createConversation();
									conv.setPhoneNumber(mPhoneNumber);
									// no need to save, because it will get saved while
									// creating the session keys
								}
								SimNumber simNumber = SimCard.getSingleton().getNumber();
								conv.deleteSessionKeys(simNumber);
								SessionKeys keys = SessionKeys.createSessionKeys(conv);
								keys.setSessionKey_Out(keysMessage.getKeyOut());
								keys.setSessionKey_In(keysMessage.getKeyIn());
								keys.setSimNumber(simNumber);
								keys.setKeysSent(true);
								keys.setKeysConfirmed(false);
								keys.saveToFile();
							} catch (StorageFileException e) {
								State.fatalException(e);
								return;
							}
							// go back
							ActivityExchangeViaText.this.startActivity(new Intent(ActivityExchangeViaText.this, ActivityLists.class));
						}
						
						@Override
						public void onError(String message) {
							getDialogManager().dismissDialog(DIALOG_SENDING);
							
							Bundle params = new Bundle();
							params.putString(PARAM_SENDING_ERROR, message);
							getDialogManager().showDialog(DIALOG_SENDING_ERROR, params);
						}

						@Override
						public boolean onPartSent(int index) {
							if (!Pki.isLoggedIn()) {
								mCancelled = true;
								return false;
							}
							
							ProgressDialog pd = (ProgressDialog) getDialogManager().getDialog(DIALOG_SENDING);
							if (pd != null) 
								pd.setProgress(index + 1);
							return true;
						}
					});
				} catch (StorageFileException e) {
					State.fatalException(e);
					return;
				} catch (MessageException e) {
					State.fatalException(e);
					return;
				} catch (EncryptionException e) {
					State.fatalException(e);
					return;
				}
			}
		});
        
        // prepare dialogs
        getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = ActivityExchangeViaText.this.getResources();
				ProgressDialog pd = new ProgressDialog(ActivityExchangeViaText.this);
				pd.setCancelable(false);
				pd.setMessage(res.getString(R.string.sending));
				pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				try {
					pd.setMax(KeysMessage.getPartsCount());
				} catch (MessageException e) {
					// should not happen, ever...
				}
				return pd;
			}
			
			@Override
			public String getId() {
				return DIALOG_SENDING;
			}
		});
        
        getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				String error = params.getString(PARAM_SENDING_ERROR);
				Resources res = ActivityExchangeViaText.this.getResources();
				
				return new AlertDialog.Builder(ActivityExchangeViaText.this)
				       .setTitle(res.getString(R.string.error_sms_service))
				       .setMessage(res.getString(R.string.error_sms_service_details) + "\nError: " + error)
				       .setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
				       .create();
			}
			
			@Override
			public String getId() {
				return DIALOG_SENDING_ERROR;
			}
		});
	}

	@Override
	public void onPkiLogin() {
		super.onPkiLogin();
		if (mCancelled) {
			mCancelled = false;
			getDialogManager().dismissDialog(DIALOG_SENDING);
		}
	}
}
