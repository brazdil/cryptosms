package uk.ac.cam.db538.cryptosms.ui;

import roboguice.inject.InjectView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import uk.ac.cam.db538.cryptosms.utils.SimNumber;

public class ActivityExchangeViaText extends ActivityAppState {
	public static final String OPTION_PHONE_NUMBER = "PHONE_NUMBER";
	
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
        mPhoneNumber = getIntent().getExtras().getString(OPTION_PHONE_NUMBER);
        if (mContactId == -1L || mPhoneNumber == null)
        	this.finish();

        mContact = Contact.getContact(this, mPhoneNumber, mContactId);
        if (mContact == null || !mContact.existsInDatabase())
        	this.finish();
        
        // number of texts
		mSmsCountView.setText(Integer.toString(KeysMessage.getPartsCount()));
        
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
				
				// generate session keys
				final KeysMessage keysMessage;
				try {
					keysMessage = new KeysMessage();
				} catch (StorageFileException e) {
					State.fatalException(e);
					return;
				}

				Bundle params = new Bundle();
				params.putInt(UtilsSendMessage.PARAM_SENDING_MULTIPART_MAX, KeysMessage.getPartsCount());
		    	getDialogManager().showDialog(UtilsSendMessage.DIALOG_SENDING_MULTIPART, params);

		    	// send the message
				try {
					mCancelled = false;
					keysMessage.sendSMS(mPhoneNumber, ActivityExchangeViaText.this, new MessageSendingListener() {
						@Override
						public void onMessageSent() {
							Log.d(MyApplication.APP_TAG, "Sent all!");
							getDialogManager().dismissDialog(UtilsSendMessage.DIALOG_SENDING_MULTIPART);
							
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
								keys.setPrivateKey(keysMessage.getPrivateKey());
								keys.setKeysId(keysMessage.getId());
								Log.d(MyApplication.APP_TAG, "KeyId: " + keysMessage.getId());
								keys.setSimNumber(simNumber);
								keys.setKeysSent(true);
								keys.setKeysConfirmed(false);
								keys.saveToFile();
							} catch (StorageFileException e) {
								State.fatalException(e);
								return;
							}
							// go back
							Log.d(MyApplication.APP_TAG, "finishing");
							ActivityExchangeViaText.this.setResult(Activity.RESULT_OK);
							ActivityExchangeViaText.this.finish();
						}
						
						@Override
						public void onError(String message) {
							getDialogManager().dismissDialog(UtilsSendMessage.DIALOG_SENDING_MULTIPART);
							
							Bundle params = new Bundle();
							params.putString(UtilsSendMessage.PARAM_SENDING_ERROR, message);
							getDialogManager().showDialog(UtilsSendMessage.DIALOG_SENDING_ERROR, params);

							mSendButton.setEnabled(true);
							mBackButton.setEnabled(true);
						}

						private int mCounterFinished = 0;
						
						@Override
						public void onPartSent(int index) {
							ProgressDialog pd = (ProgressDialog) getDialogManager().getDialog(UtilsSendMessage.DIALOG_SENDING_MULTIPART);
							if (pd != null)
								pd.setProgress(++mCounterFinished);
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
        UtilsSendMessage.prepareDialogs(getDialogManager(), this);
	}

	@Override
	public void onPkiLogin() {
		super.onPkiLogin();
		if (mCancelled) {
			mCancelled = false;
			getDialogManager().dismissDialog(UtilsSendMessage.DIALOG_SENDING_MULTIPART);
		}
	}
}
