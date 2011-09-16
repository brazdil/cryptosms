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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.KeysMessage;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageSendingListener;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.ui.UtilsContactBadge;
import uk.ac.cam.db538.cryptosms.ui.UtilsSendMessage;

public class ActivityExchangeViaText extends ActivityAppState {
	public static final String OPTION_PHONE_NUMBER = "PHONE_NUMBER";
	
	private static boolean mCancelled = false;
	
	private Contact mContact;

	private String mPhoneNumber;
	
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
        if (mPhoneNumber == null)
        	this.finish();

        mContact = Contact.getContact(this, mPhoneNumber);
        if (!mContact.existsInDatabase())
        	this.finish();
        
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
				
		    	getDialogManager().showDialog(UtilsSendMessage.DIALOG_SENDING, null);

				new SendMessageTask().execute();
		    	
			}
		});
        
        // prepare dialogs
        UtilsSendMessage.prepareDialogs(getDialogManager(), this);
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.ui.activity.ActivityAppState#onPkiLogin()
	 */
	@Override
	public void onPkiLogin() {
		super.onPkiLogin();
		if (mCancelled) {
			mCancelled = false;
			getDialogManager().dismissDialog(UtilsSendMessage.DIALOG_SENDING_MULTIPART);
		}
	}

	private class SendMessageTask extends AsyncTask<Void, Void, Void> {
		
		private Exception mException = null;
		private KeysMessage mKeysMessage = null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			// generate session keys
			try {
				mKeysMessage = new KeysMessage();
			} catch (StorageFileException e) {
				mException = e;
				return null;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			if (mException != null) {
				State.fatalException(mException);
				return;
			}

	    	// send the message
			try {
				mCancelled = false;
				mKeysMessage.sendSMS(mPhoneNumber, ActivityExchangeViaText.this, new MessageSendingListener() {
					@Override
					public void onMessageSent() {
						getDialogManager().dismissDialog(UtilsSendMessage.DIALOG_SENDING);
						
						// go back
						ActivityExchangeViaText.this.setResult(Activity.RESULT_OK);
						ActivityExchangeViaText.this.finish();
					}
					
					@Override
					public void onError(Exception ex) {
						if (ex instanceof StorageFileException) {
							State.fatalException(ex);
							return;
						}
						
						getDialogManager().dismissDialog(UtilsSendMessage.DIALOG_SENDING);
						
						Bundle params = new Bundle();
						params.putString(UtilsSendMessage.PARAM_SENDING_ERROR, ex.getMessage());
						getDialogManager().showDialog(UtilsSendMessage.DIALOG_SENDING_ERROR, params);

						mSendButton.setEnabled(true);
						mBackButton.setEnabled(true);
					}
					
					@Override
					public void onPartSent(int index) {
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
	}
}
