package uk.ac.cam.db538.securesms.ui;

import java.io.IOException;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.data.Contact;
import uk.ac.cam.db538.securesms.data.DummyOnClickListener;
import uk.ac.cam.db538.securesms.data.SimCard;
import uk.ac.cam.db538.securesms.data.SimCard.OnSimStateListener;
import uk.ac.cam.db538.securesms.data.Utils;
import uk.ac.cam.db538.securesms.storage.Conversation;
import uk.ac.cam.db538.securesms.storage.StorageFileException;
import uk.ac.cam.db538.securesms.data.MessageData;
import android.app.Activity;
import android.app.AlertDialog;
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
    
    private boolean errorNoKeysShow;

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.screen_conversation);
	    
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        Context context = this;
	    Resources res = getResources();
	    
        if (sDefaultContactImage == null) {
            sDefaultContactImage = res.getDrawable(R.drawable.ic_contact_picture);
        }

        Intent intent = getIntent();
	    Bundle bundle = intent.getExtras();
	    String phoneNumber = bundle.getString(OPTION_PHONE_NUMBER);
	    errorNoKeysShow = bundle.getBoolean(OPTION_OFFER_KEYS_SETUP, true);
	    mContact = Contact.getContact(context, phoneNumber);
	    mNameView = (TextView) findViewById(R.id.conversation_name);
	    mPhoneNumberView = (TextView) findViewById(R.id.conversation_phone_number);
	    mAvatarView = (QuickContactBadge) findViewById(R.id.conversation_avatar);
	    mSendButton = (Button) findViewById(R.id.conversation_send_button);
	    mTextEditor = (EditText) findViewById(R.id.conversation_embedded_text_editor);
	    
	    mNameView.setText(mContact.getName());
	    mPhoneNumberView.setText(mContact.getPhoneNumber());
	    Drawable avatarDrawable = mContact.getAvatar(context, sDefaultContactImage);
        if (mContact.existsInDatabase()) {
            mAvatarView.assignContactUri(mContact.getUri());
        } else {
            mAvatarView.assignContactFromPhone(mContact.getPhoneNumber(), true);
        }
        mAvatarView.setImageDrawable(avatarDrawable);
        mAvatarView.setVisibility(View.VISIBLE);
        
        // register for changes in SIM state
        SimCard.getSingleton().registerSimStateListener(this, new OnSimStateListener() {
			public void onChange() {
				checkResources();
			}
		});
        
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
				MessageData msg = new MessageData(text);
				mSendButton.setText(new Integer(msg.getLength()).toString() + " x " + text.length());
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
	
	private void checkResources() {
		// check for SIM availability
	    try {
			if (Utils.checkSimPhoneNumberAvailable(this)) {
			    Resources res = getResources();
				mConversation = Conversation.getConversation(mContact.getPhoneNumber());

				// check keys availability
		    	if (!Utils.hasKeysExchangedForSIM(this, mConversation)) {
		    		if (errorNoKeysShow) {
						// secure connection has not been successfully established yet
						new AlertDialog.Builder(this)
							.setTitle(res.getString(R.string.conversation_no_keys))
							.setMessage(res.getString(R.string.conversation_no_keys_details))
							.setPositiveButton(res.getString(R.string.read_only), new DummyOnClickListener())
							.setNegativeButton(res.getString(R.string.setup), new OnClickListener() {
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
			Utils.dialogDatabaseError(this, ex);
			this.finish();
		} catch (IOException ex) {
			Utils.dialogIOError(this, ex);
			this.finish();
		}
	}
	
	public void onResume() {
		super.onResume();
		checkResources();
	}
}
