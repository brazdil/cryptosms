package uk.ac.cam.db538.cryptosms.ui;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.data.TextMessage;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageSentListener;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.state.State.StateChangeListener;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;
import uk.ac.cam.db538.cryptosms.utils.CompressedText;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.QuickContactBadge;
import android.widget.TextView;

public class ConversationActivity extends Activity {
	public static final String OPTION_PHONE_NUMBER = "phoneNumber";
	public static final String OPTION_OFFER_KEYS_SETUP = "createKeys";
	
	private static final String DIALOG_NO_SESSION_KEYS = "DIALOG_NO_SESSION_KEYS";
	
	static private Drawable sDefaultContactImage = null;

	private Contact mContact;
	private Conversation mConversation;
	private TextView mNameView;
	private TextView mPhoneNumberView;
    private QuickContactBadge mAvatarView;
    private Button mSendButton;
    private EditText mTextEditor;
    private TextView mRemainsView;
    
	private ErrorOverlay mErrorOverlay;
	private View mMainLayout;
    
    private boolean errorNoKeysShow;
	private DialogManager mDialogManager = new DialogManager();
	private Context mContext = this;

	private StateChangeListener mStateListener = new StateChangeListener(){
		@Override
		public void onConnect() {
		}

		@Override
		public void onDisconnect() {
			Log.d(MyApplication.APP_TAG, "Disconnect error overlay");
			mErrorOverlay.modeDisconnected();
			mErrorOverlay.setVisibility(View.VISIBLE);
	        mMainLayout.setVisibility(View.INVISIBLE);
		}

		@Override
		public void onFatalException(Exception ex) {
			Log.d(MyApplication.APP_TAG, "Fatal exception error overlay");
			mErrorOverlay.modeFatalException(ex);
			mErrorOverlay.setVisibility(View.VISIBLE);
	        mMainLayout.setVisibility(View.INVISIBLE);
		}

		@Override
		public void onLogin() {
			mErrorOverlay.setVisibility(View.INVISIBLE);
	        mMainLayout.setVisibility(View.VISIBLE);
	        
	        mDialogManager.restoreState();
		}

		@Override
		public void onLogout() {
			Log.d(MyApplication.APP_TAG, "Logout error overlay");
			modeEnabled(false);
			mErrorOverlay.modeLogin();
			mErrorOverlay.setVisibility(View.VISIBLE);
	        mMainLayout.setVisibility(View.INVISIBLE);
	        
	        mDialogManager.saveState();
		}

		@Override
		public void onPkiMissing() {
			Log.d(MyApplication.APP_TAG, "PkiMissing error overlay");
			mErrorOverlay.modePkiMissing();
			mErrorOverlay.setVisibility(View.VISIBLE);
	        mMainLayout.setVisibility(View.INVISIBLE);
		}

		@Override
		public void onSimState() {
    		modeEnabled(false);
    		Utils.handleSimIssues(mContext, mDialogManager);
    		
			// check for SIM availability
		    try {
				if (SimCard.getSingleton().isNumberAvailable()) {
					// check keys availability
			    	if (StorageUtils.hasKeysExchangedForSim(mConversation)) 
			    		modeEnabled(true);
			    	else {
			    		if (errorNoKeysShow) {
							// secure connection has not been successfully established yet
							errorNoKeysShow = false;
							mDialogManager.showDialog(DIALOG_NO_SESSION_KEYS, null);
			    		}
					}
				}
			} catch (StorageFileException ex) {
				State.fatalException(ex);
				return;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.screen_conversation);
	    
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        final Context context = this;
	    final Resources res = getResources();
	    
        if (sDefaultContactImage == null) {
            sDefaultContactImage = res.getDrawable(R.drawable.ic_contact_picture);
        }

        Intent intent = getIntent();
	    Bundle bundle = intent.getExtras();
	    String phoneNumber = bundle.getString(OPTION_PHONE_NUMBER);
	    errorNoKeysShow = bundle.getBoolean(OPTION_OFFER_KEYS_SETUP, true);
	    mContact = Contact.getContact(context, phoneNumber);
		try {
			mConversation = Conversation.getConversation(mContact.getPhoneNumber());
		} catch (StorageFileException ex) {
			State.fatalException(ex);
			return;
		}
	    
	    mNameView = (TextView) findViewById(R.id.conversation_name);
	    mPhoneNumberView = (TextView) findViewById(R.id.conversation_phone_number);
	    mAvatarView = (QuickContactBadge) findViewById(R.id.conversation_avatar);
	    mSendButton = (Button) findViewById(R.id.conversation_send_button);
	    mTextEditor = (EditText) findViewById(R.id.conversation_embedded_text_editor);
	    mRemainsView = (TextView) findViewById(R.id.conversation_text_counter);
	    
	    if (mContact.getName().length() > 0) {
	    	mNameView.setText(mContact.getName());
	    	mPhoneNumberView.setText(mContact.getPhoneNumber());
	    }
	    else {
	    	mNameView.setText(mContact.getPhoneNumber());
	    	mPhoneNumberView.setText(new String());
	    }
	    
	    Drawable avatarDrawable = mContact.getAvatar(context, sDefaultContactImage);
        if (mContact.existsInDatabase()) {
            mAvatarView.assignContactUri(mContact.getUri());
        } else {
            mAvatarView.assignContactFromPhone(mContact.getPhoneNumber(), true);
        }
        mAvatarView.setImageDrawable(avatarDrawable);
        mAvatarView.setVisibility(View.VISIBLE);
        
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
					mRemainsView.setText(TextMessage.remainingBytesInLastMessagePart(msg) + " (" + TextMessage.computeNumberOfMessageParts(msg) + ")");
					mSendButton.setEnabled(true);
				} catch (MessageException e) {
					mSendButton.setEnabled(false);
				}
				mRemainsView.setVisibility(View.VISIBLE);
			}
		});

        mSendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mConversation != null) {
					try {
						// start the progress bar
						final ProgressDialog pd = new ProgressDialog(context);
						pd.setMessage(res.getString(R.string.conversation_sending));
						pd.show();
						
						// create message
						TextMessage msg = new TextMessage(MessageData.createMessageData(mConversation));
						msg.setText(CompressedText.createFromString(mTextEditor.getText().toString()));
						// send it
						msg.sendSMS(mContact.getPhoneNumber(), context, new MessageSentListener() {
							@Override
							public void onMessageSent() {
								pd.cancel();
								mTextEditor.setText("");
							}
							
							@Override
							public void onError(String message) {
								pd.cancel();
								// TODO: get rid of this!!!
								new AlertDialog.Builder(context)
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
	
	    // error overlay
	    mMainLayout = findViewById(R.id.screen_conversation);
	    mErrorOverlay = (ErrorOverlay) findViewById(R.id.screen_conversation_error);
        mStateListener.onLogout();
        
        // prepare dialogs
        mDialogManager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				return new AlertDialog.Builder(mContext)
				       .setTitle(res.getString(R.string.conversation_no_keys))
				       .setMessage(res.getString(R.string.conversation_no_keys_details))
				       .setPositiveButton(res.getString(R.string.read_only), new DummyOnClickListener())
				       .setNegativeButton(res.getString(R.string.setup), new OnClickListener() {
				    	   @Override
				    	   public void onClick(DialogInterface dialog,
				    			   int which) {
				    		   // TODO: setup
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
		State.addListener(mStateListener);
	}

	@Override
	protected void onStop() {
		State.removeListener(mStateListener);
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Pki.login(false);
	}

}