package uk.ac.cam.db538.cryptosms.ui;

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
import uk.ac.cam.db538.cryptosms.utils.CompressedText;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.QuickContactBadge;
import android.widget.TextView;

public class ConversationActivity extends Activity {
	public static final String OPTION_PHONE_NUMBER = "phoneNumber";
	public static final String OPTION_OFFER_KEYS_SETUP = "createKeys";
	
	static private Drawable sDefaultContactImage = null;

	private Contact mContact;
	private Conversation mConversation;
	private TextView mNameView;
	private TextView mPhoneNumberView;
    private QuickContactBadge mAvatarView;
    private Button mSendButton;
    private EditText mTextEditor;
    private TextView mRemainsView;
    
    private boolean errorNoKeysShow;
	private DialogManager mDialogManager = new DialogManager();
	private Context mContext = this;
    
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
		State.addListener(mStateChangeListener);
	}

	@Override
	protected void onStop() {
		State.removeListener(mStateChangeListener);
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Pki.login(false);
	}

	private StateChangeListener mStateChangeListener = new StateChangeListener() {

		@Override
		public void onConnect() {
		}

		@Override
		public void onLogin() {
		}

		@Override
		public void onLogout() {
		}

		@Override
		public void onDisconnect() {
		}

		@Override
		public void onPkiMissing() {
		}

		@Override
		public void onSimState() {
			Utils.handleSimIssues(mContext, mDialogManager);
			
			// check for SIM availability
		    try {
			    Resources res = getResources();
				if (SimCard.getSingleton().isNumberAvailable()) {
					// check keys availability
			    	if (!StorageUtils.hasKeysExchangedForSim(mConversation)) {
			    		if (errorNoKeysShow) {
							// secure connection has not been successfully established yet
							new AlertDialog.Builder(mContext)
								.setTitle(res.getString(R.string.conversation_no_keys))
								.setMessage(res.getString(R.string.conversation_no_keys_details))
								.setPositiveButton(res.getString(R.string.read_only), new DummyOnClickListener())
								.setNegativeButton(res.getString(R.string.setup), new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										// TODO: Setup
									}
								})
								.show();
							errorNoKeysShow = false;
			    		}
			    		
						// set to disabled mode
			    		modeEnabled(false);
					} else
						modeEnabled(true);
				} else
					modeEnabled(false);
			} catch (StorageFileException ex) {
				State.fatalException(ex);
				return;
			}
		}

		@Override
		public void onFatalException(Exception ex) {
			// TODO Auto-generated method stub
			
		}
		
	};
}