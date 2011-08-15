package uk.ac.cam.db538.cryptosms.ui;

import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.state.State.StateChangeListener;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.Header;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.storage.Conversation.ConversationsChangeListener;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class TabContacts extends ListActivity {
	private static final int ACTIVITY_NEW_CONTACT = 1;
	
	private static final String BUNDLE_DIALOG_MANAGER = "DIALOG_MANAGER";
	private static final String DIALOG_PHONE_NUMBER_PICKER = "DIALOG_PHONE_NUMBER_PICKER";
	private static final String PARAMS_PHONE_NUMBER_PICKER_ID = "PARAMS_PHONE_NUMBER_PICKER_ID";
	
	private Context mContext = this;
    private ListView mListView;
    private LayoutInflater mInflater;
    
	private ArrayList<Conversation> mContacts = new ArrayList<Conversation>();;
	private TabContactsItem mNewContactView;
	private ArrayAdapter<Conversation> mAdapterContacts;
	private DialogManager mDialogManager = new DialogManager();
	
	private void startConversation(Conversation conv) {
		Intent intent = new Intent(TabContacts.this, ConversationActivity.class);
		intent.putExtra(ConversationActivity.OPTION_PHONE_NUMBER, conv.getPhoneNumber());
		startActivity(intent);
	}	
	
	private void startConversation(String phoneNumber) {
    	// get the appropriate conversation
    	Conversation conv;
		try {
			conv = Conversation.getConversation(phoneNumber);
	    	if (conv == null) {
	    		conv = Conversation.createConversation();
	    		conv.setPhoneNumber(phoneNumber);
	    		conv.saveToFile();
	    	}
			
			// if we managed to get it, start the activity
			startConversation(conv);
		} catch (StorageFileException ex) {
			State.fatalException(ex);
			return;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_main_listtab);
        mListView = this.getListView();
        mInflater = LayoutInflater.from(this);
        
        // set appearance of list view
        mListView.setFastScrollEnabled(true);
		
        // the New contact header
		mNewContactView = (TabContactsItem) mInflater.inflate(R.layout.item_main_contacts, mListView, false);
		mListView.addHeaderView(mNewContactView, null, true);
		
		// specify what to do when clicked on items
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,	int arg2, long arg3) {
				if (!SimCard.getSingleton().isNumberAvailable())
					return;
				
				TabContactsItem item = (TabContactsItem) view;
				Conversation conv;
	    		if ((conv = item.getConversationHeader()) != null) {
		    		// clicked on a conversation
					try {
						if (StorageUtils.hasKeysExchangedForSim(conv))
		    				startConversation(conv);
					} catch (StorageFileException ex) {
						State.fatalException(ex);
						return;
					}
	    		} else {
	    			// clicked on the header
	    			
	    			// pick a contact from PKI
					Intent intent = new Intent(MyApplication.PKI_CONTACT_PICKER);
					intent.putExtra("pick", true);
			        intent.putExtra("Contact Criteria", "in_visible_group=1");
			        intent.putExtra("Key Criteria", "contact_id IN (SELECT contact_id FROM keys GROUP BY contact_id HAVING COUNT(key_name)>0)");
			        intent.putExtra("sort", "display_name COLLATE LOCALIZED ASC");
					try {
						startActivityForResult(intent, ACTIVITY_NEW_CONTACT);
    				} catch(ActivityNotFoundException e) {
    					// TODO: PKI unavailable
    				}
	    		}
			}
		});

    	// create the adapter
    	mAdapterContacts = new ArrayAdapter<Conversation>(mContext, R.layout.item_main_contacts, mContacts) {
    		@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TabContactsItem row;

				if (convertView == null)
					row = (TabContactsItem) mInflater.inflate(R.layout.item_main_contacts, mListView, false);
				else
					row = (TabContactsItem) convertView;
			    
				row.bind(getItem(position));
				return row;
			}
		};
		
		// prepare dialogs
		Utils.prepareDialogs(mDialogManager, this);
		mDialogManager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = mContext.getResources();
				
				// get phone numbers associated with the contact
				final ArrayList<Contact.PhoneNumber> phoneNumbers = 
					Contact.getPhoneNumbers(mContext, params.getLong(PARAMS_PHONE_NUMBER_PICKER_ID));

				final CharSequence[] items = new CharSequence[phoneNumbers.size()];
				for (int i = 0; i < phoneNumbers.size(); ++i)
					items[i] = phoneNumbers.get(i).toString();

				// display them in a dialog
				return new AlertDialog.Builder(mContext)
				       .setTitle(res.getString(R.string.contacts_pick_phone_number))
				       .setItems(items, new DialogInterface.OnClickListener() {
				    	   @Override
				    	   public void onClick(DialogInterface dialog, int item) {
				    		   Contact.PhoneNumber phoneNumber = phoneNumbers.get(item);
				    		   startConversation(phoneNumber.getPhoneNumber());
				    	   }
				       })
				       .create();
			}
			
			@Override
			public String getId() {
				return DIALOG_PHONE_NUMBER_PICKER;
			}
		});
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if (!Pki.isLoggedIn())
    		return;
    	
    	final Context context = getApplicationContext();
    	Resources res = context.getResources();
    	
    	switch (requestCode) {
    	case ACTIVITY_NEW_CONTACT:
    		if (resultCode == Activity.RESULT_OK) {
    			long contactId = data.getLongExtra("contact", 0);
    			String contactKey = data.getStringExtra("key name");
    			if (contactId > 0 && contactKey != null) {
    				// get phone numbers associated with the contact
    				final ArrayList<Contact.PhoneNumber> phoneNumbers = 
    					Contact.getPhoneNumbers(context, contactId);

    				if (phoneNumbers.size() > 1) {
    					Bundle params = new Bundle();
    					params.putLong(PARAMS_PHONE_NUMBER_PICKER_ID, contactId);
    					mDialogManager.showDialog(DIALOG_PHONE_NUMBER_PICKER, params);
    				} else if (phoneNumbers.size() == 1) {
    					startConversation(phoneNumbers.get(0).getPhoneNumber());
    				} else {
    					// no phone numbers assigned to the contact
	    				new AlertDialog.Builder(this)
	    					.setTitle(res.getString(R.string.contacts_no_phone_numbers))
	    					.setMessage(res.getString(R.string.contacts_no_phone_numbers_details))
	    					.setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
	    					.show();
    				}
    			}
    		}
    		break;
    	}
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		Pki.login(false);
	}

	@Override
	protected void onStart() {
		super.onStart();
		State.addListener(mStateListener);
    	Conversation.addListener(mConversationsListener);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		
		mDialogManager.setSavedState(state.getBundle(BUNDLE_DIALOG_MANAGER));
		mDialogManager.restoreState();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		mDialogManager.saveState();
		outState.putBundle(BUNDLE_DIALOG_MANAGER, mDialogManager.getSavedState());
	}

	@Override
	protected void onStop() {
		State.removeListener(mStateListener);
    	Conversation.removeListener(mConversationsListener);
		super.onStop();
	}

	private StateChangeListener mStateListener = new StateChangeListener() {
		
		@Override
		public void onSimState() {
			Utils.handleSimIssues(mContext, mDialogManager);
			
			// check SIM availability
			if (SimCard.getSingleton().isNumberAvailable()) {
    			setListAdapter(mAdapterContacts);
	    		mNewContactView.bind(getString(R.string.tab_contacts_new_contact), getString(R.string.tab_contacts_new_contact_details));
			} else {
		    	setListAdapter(null);
		        mNewContactView.bind(getString(R.string.tab_contacts_not_available), getString(R.string.tab_contacts_not_available_details));
		    }
		}
		
		@Override
		public void onPkiMissing() {
		}
		
		@Override
		public void onLogout() {
			setListAdapter(null);
			mDialogManager.saveState();
		}
		
		@Override
		public void onLogin() {
        	// initialize the list of Contacts
        	mConversationsListener.onUpdate();
        	// set adapter
			setListAdapter(mAdapterContacts);
			// restore state
			mDialogManager.restoreState();
	    }
		
		@Override
		public void onFatalException(Exception ex) {
		}
		
		@Override
		public void onDisconnect() {
		}
		
		@Override
		public void onConnect() {
		}
	};
	
	private ConversationsChangeListener mConversationsListener = new ConversationsChangeListener() {
		@Override
		public void onUpdate() {
			try {
				mContacts.clear();
				
				Conversation conv = Header.getHeader().getFirstConversation();
				while (conv != null) {
					if (StorageUtils.hasKeysForSim(conv))
						mContacts.add(conv);
					conv = conv.getNextConversation();
				}
				
				mAdapterContacts.notifyDataSetChanged();
			} catch (StorageFileException ex) {
				State.fatalException(ex);
				return;
			}
		}
	};
}
