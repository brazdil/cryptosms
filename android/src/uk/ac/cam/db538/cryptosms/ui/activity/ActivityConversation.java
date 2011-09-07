package uk.ac.cam.db538.cryptosms.ui.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import roboguice.inject.InjectView;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.data.TextMessage;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageSendingListener;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.ui.DialogManager;
import uk.ac.cam.db538.cryptosms.ui.DummyOnClickListener;
import uk.ac.cam.db538.cryptosms.ui.UtilsContactBadge;
import uk.ac.cam.db538.cryptosms.ui.UtilsSimIssues;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;
import uk.ac.cam.db538.cryptosms.ui.adapter.AdapterMessages;
import uk.ac.cam.db538.cryptosms.ui.list.ListViewMessage;
import uk.ac.cam.db538.cryptosms.utils.CompressedText;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.AsyncTask;
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
	@InjectView(R.id.history)
	private ListViewMessage mListMessageHistory;
	
	private AdapterMessages mAdapterMessageHistory;
    
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
	    
	    // check that we got arguments
	    if (phoneNumber == null)
	    	this.finish();
	    
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
				mBytesCounterView.setText(TextMessage.getRemainingBytes(msg.getDataLength()) + " (" + TextMessage.getMessagePartsCount(msg.getDataLength()) + ")");
				mSendButton.setEnabled(true);
				mBytesCounterView.setVisibility(View.VISIBLE);
			}
		});

        mSendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mConversation != null) {
					try {
						// start the progress bar
						// TODO: get rid of this!!!
						final ProgressDialog pd = new ProgressDialog(ActivityConversation.this);
						pd.setMessage(res.getString(R.string.sending));
						pd.show();
						
						// create message
						final TextMessage msg = new TextMessage(MessageData.createMessageData(mConversation));
						msg.setText(CompressedText.createFromString(mTextEditor.getText().toString()));
						// send it
						msg.sendSMS(mContact.getPhoneNumber(), ActivityConversation.this, new MessageSendingListener() {
							@Override
							public void onMessageSent() {
								pd.cancel();
								mTextEditor.setText("");
								updateMessageHistory();
							}
							
							@Override
							public void onError(Exception ex) {
								pd.cancel();
								// TODO: get rid of this!!!
								new AlertDialog.Builder(ActivityConversation.this)
								.setTitle(res.getString(R.string.error_sms_service))
								.setMessage(res.getString(R.string.error_sms_service_details) + "\nError: " + ex.getMessage())
								.setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
								.show();
								updateMessageHistory();
							}
							
							@Override
							public void onPartSent(int index) {
							}
						});
					} catch (StorageFileException ex) {
						State.fatalException(ex);
						return;
					} catch (MessageException ex) {
						State.fatalException(ex);
						return;
					} catch (EncryptionException ex) {
						State.fatalException(ex);
						return;
					}
				}
			}
        });
        
        // set appearance of list view
	    mListMessageHistory.setFastScrollEnabled(true);
    	// create the adapter
	    mAdapterMessageHistory = new AdapterMessages(getLayoutInflater(), mListMessageHistory);
		// prepare for context menus
		registerForContextMenu(mListMessageHistory);
	
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
	public void onPkiLogin() {
		super.onPkiLogin();
		updateMessageHistory();
		mListMessageHistory.setAdapter(mAdapterMessageHistory);
	}

	@Override
	public void onPkiLogout() {
		mListMessageHistory.setAdapter(null);
		modeEnabled(false);
		super.onPkiLogout();
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

	private void updateMessageHistory() {
		synchronized(mAdapterMessageHistory) {
			new MessageHistoryUpdateTask().execute();
		}
	}

	private class MessageHistoryUpdateTask extends AsyncTask<Void, Void, Void> {

		private ArrayList<TextMessage> mTextMessages = null;
		private Exception mException = null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
//			if (mAdapterMessageHistory.getList() == null || mAdapterMessageHistory.getList().size() == 0) {
//				mListNotificationsLoading.setVisibility(View.VISIBLE);
//				mListNotifications.setVisibility(View.GONE);
//			}
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				ArrayList<MessageData> allMessageData = mConversation.getMessages();
				Collections.sort(allMessageData, new Comparator<MessageData>() {
					@Override
					public int compare(MessageData arg0, MessageData arg1) {
						return arg0.getTimeStamp().compareTo(arg1.getTimeStamp());
					}
				});
				mTextMessages = new ArrayList<TextMessage>(allMessageData.size());
				for (MessageData storage : allMessageData)
					mTextMessages.add(new TextMessage(storage));
			} catch (StorageFileException ex) {
				mException = ex;
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

			mAdapterMessageHistory.setList(mTextMessages);
			mAdapterMessageHistory.notifyDataSetChanged();
//			mListNotificationsLoading.setVisibility(View.GONE);
//			mListNotifications.setVisibility(View.VISIBLE);
		}
	}
}