package uk.ac.cam.db538.securesms.ui;

import java.io.IOException;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.database.Conversation;
import uk.ac.cam.db538.securesms.database.DatabaseFileException;
import uk.ac.cam.db538.securesms.utils.Common;
import uk.ac.cam.db538.securesms.utils.Contact;
import uk.ac.cam.db538.securesms.utils.DummyOnClickListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

public class ConversationActivity extends Activity {
	static private Drawable sDefaultContactImage = null;

	private Contact mContact;
	private Conversation mConversation;
	private TextView mNameView;
	private TextView mPhoneNumberView;
    private QuickContactBadge mAvatarView;
    private Button mSendButton;
    private EditText mTextEditor;
    private View mTopPanel;
    private View mBottomPanel;
    
    private boolean errorNoKeysShown = false;

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.screen_conversation);
	    
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        Context context = getApplicationContext();
	    Resources res = getResources();
	    
        if (sDefaultContactImage == null) {
            sDefaultContactImage = res.getDrawable(R.drawable.ic_contact_picture);
        }

        Intent intent = getIntent();
	    Bundle bundle = intent.getExtras();
	    String phoneNumber = bundle.getString("phoneNumber");
	    mContact = Contact.getContact(context, phoneNumber);
	    mNameView = (TextView) findViewById(R.id.conversation_name);
	    mPhoneNumberView = (TextView) findViewById(R.id.conversation_phone_number);
	    mAvatarView = (QuickContactBadge) findViewById(R.id.conversation_avatar);
	    mSendButton = (Button) findViewById(R.id.conversation_send_button);
	    mTextEditor = (EditText) findViewById(R.id.conversation_embedded_text_editor);
	    
	    mTopPanel = findViewById(R.id.conversation_top_panel);
	    mBottomPanel = findViewById(R.id.conversation_bottom_panel);
	    
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
	}
	
	public void onStart() {
		super.onStart();
	    Context context = this;
	    Resources res = getResources();

	    try {
			mConversation = Conversation.getConversation(mContact.getPhoneNumber());
			
	    	if (!mConversation.hasKeysExchanged(Common.getSimNumber(context))) {
	    		if (!errorNoKeysShown) {
					// secure connection has not been successfully established yet
					new AlertDialog.Builder(context)
						.setTitle(res.getString(R.string.conversation_no_keys))
						.setMessage(res.getString(R.string.conversation_no_keys_details))
						.setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
						.show();
					errorNoKeysShown = true;
	    		}
	    		
				// disable the send button and text view
				mSendButton.setEnabled(false);
				mTextEditor.setEnabled(false);
			}
		} catch (DatabaseFileException ex) {
			Common.dialogDatabaseError(context, ex);
			this.finish();
		} catch (IOException ex) {
			Common.dialogIOError(context, ex);
			this.finish();
		}
	}
}
