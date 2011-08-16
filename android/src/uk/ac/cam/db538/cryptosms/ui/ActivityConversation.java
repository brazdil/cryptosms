package uk.ac.cam.db538.cryptosms.ui;

import roboguice.inject.InjectView;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.data.TextMessage;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageSentListener;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;
import uk.ac.cam.db538.cryptosms.utils.CompressedText;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ActivityConversation extends ActivityAppState {
	public static final String OPTION_PHONE_NUMBER = "PHONE_NUMBER";
	public static final String OPTION_OFFER_KEYS_SETUP = "KEYS_SETUP";
	
	private static final String DIALOG_NO_SESSION_KEYS = "DIALOG_NO_SESSION_KEYS";
	
	private Contact mContact;
	private Conversation mConversation;
	
	@InjectView(R.id.send)
    private Button mSendButton;
	@InjectView(R.id.text_editor)
    private EditText mTextEditor;
	@InjectView(R.id.bytes_counter)
    private TextView mBytesCounterView;
    
	private Context mContext = this;
    private boolean mErrorNoKeysShow;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.screen_conversation);
	    
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

	    final Resources res = getResources();
	    
	    Bundle bundle = getIntent().getExtras();
	    String phoneNumber = bundle.getString(OPTION_PHONE_NUMBER);
	    mErrorNoKeysShow = bundle.getBoolean(OPTION_OFFER_KEYS_SETUP, true);
	    
	    mContact = Contact.getContact(this, phoneNumber);
		try {
			mConversation = Conversation.getConversation(mContact.getPhoneNumber());
		} catch (StorageFileException ex) {
			State.fatalException(ex);
			return;
		}

	    UtilsContactBadge.setBadge(mContact, getMainView());
        
        mTextEditor.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				String text = s.toString();
				CompressedText msg = CompressedText.createFromString(text);
				try {
					mBytesCounterView.setText(TextMessage.remainingBytesInLastMessagePart(msg) + " (" + TextMessage.computeNumberOfMessageParts(msg) + ")");
					mSendButton.setEnabled(true);
				} catch (MessageException e) {
					mSendButton.setEnabled(false);
				}
				mBytesCounterView.setVisibility(View.VISIBLE);
			}
		});

        mSendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mConversation != null) {
					try {
						// start the progress bar
						final ProgressDialog pd = new ProgressDialog(ActivityConversation.this);
						pd.setMessage(res.getString(R.string.conversation_sending));
						pd.show();
						
						// create message
						TextMessage msg = new TextMessage(MessageData.createMessageData(mConversation));
						msg.setText(CompressedText.createFromString(mTextEditor.getText().toString()));
						// send it
						msg.sendSMS(mContact.getPhoneNumber(), ActivityConversation.this, new MessageSentListener() {
							@Override
							public void onMessageSent() {
								pd.cancel();
								mTextEditor.setText("");
							}
							
							@Override
							public void onError(String message) {
								pd.cancel();
								// TODO: get rid of this!!!
								new AlertDialog.Builder(ActivityConversation.this)
								.setTitle(res.getString(R.string.error_sms_service))
								.setMessage(res.getString(R.string.error_sms_service_details) + "\nError: " + message)
								.setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
								.show();
							}
						});
					} catch (StorageFileException ex) {
						State.fatalException(ex);
						return;
					} catch (MessageException ex) {
						State.fatalException(ex);
						return;
					}
				}
			}
        });
	
        // prepare dialogs
        getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				return new AlertDialog.Builder(mContext)
				       .setTitle(res.getString(R.string.conversation_no_keys))
				       .setMessage(res.getString(R.string.conversation_no_keys_details))
				       .setPositiveButton(res.getString(R.string.read_only), new DummyOnClickListener())
				       .setNegativeButton(res.getString(R.string.setup), new OnClickListener() {
				    	   @Override
				    	   public void onClick(DialogInterface dialog, int which) {
				    			Intent intent = new Intent(ActivityConversation.this, ActivityExchangeMethod.class);
				    			intent.putExtra(ActivityExchangeMethod.OPTION_PHONE_NUMBER, mConversation.getPhoneNumber());
				    			startActivity(intent);
				    	   }
				       })
				       .create();
			}
			
			@Override
			public String getId() {
				return DIALOG_NO_SESSION_KEYS;
			}
		});
	}
	
	private void modeEnabled(boolean value) {
		Resources res = getResources();
		mSendButton.setEnabled(value);
		mTextEditor.setEnabled(value);
		mTextEditor.setHint((value) ? res.getString(R.string.conversation_type_to_compose) : null);
		mTextEditor.setFocusable(value);
		mTextEditor.setFocusableInTouchMode(value);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		modeEnabled(false);
		Pki.login(false);
	}

	@Override
	public void onPkiLogout() {
		super.onPkiLogout();
		modeEnabled(false);
	}

	@Override
	public void onSimState() {
		super.onSimState();
		
		modeEnabled(false);
		UtilsSimIssues.handleSimIssues(mContext, getDialogManager());
		
		// check for SIM availability
	    try {
			if (SimCard.getSingleton().isNumberAvailable()) {
				// check keys availability
		    	if (StorageUtils.hasKeysExchangedForSim(mConversation)) 
		    		modeEnabled(true);
		    	else {
		    		if (mErrorNoKeysShow) {
						// secure connection has not been successfully established yet
						mErrorNoKeysShow = false;
						getDialogManager().showDialog(DIALOG_NO_SESSION_KEYS, null);
		    		}
				}
			}
		} catch (StorageFileException ex) {
			State.fatalException(ex);
			return;
		}
	}
}