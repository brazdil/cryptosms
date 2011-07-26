package uk.ac.cam.db538.securesms.ui;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.database.Conversation;
import uk.ac.cam.db538.securesms.database.DatabaseFileException;
import uk.ac.cam.db538.securesms.database.Header;
import uk.ac.cam.db538.securesms.database.SessionKeys;
import uk.ac.cam.db538.securesms.database.Conversation.ConversationUpdateListener;
import uk.ac.cam.db538.securesms.database.SessionKeys.SessionKeysStatus;
import uk.ac.cam.db538.securesms.utils.Common;
import uk.ac.cam.db538.securesms.utils.Contact;
import uk.ac.cam.db538.securesms.utils.DummyOnClickListener;
import uk.ac.cam.db538.securesms.utils.Common.OnSimStateListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.ContactsContract.Data;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class TabContacts extends ListActivity {
	private static final String KEY_MIME = "vnd.android.cursor.item/PKI_KEY";
	private static final int NEW_CONTACT = 1;
	
	private ArrayList<Conversation> mContacts = new ArrayList<Conversation>();;
	private TabContactsItem mNewContactView;
	private ArrayAdapter<Conversation> mAdapterContacts;
	
	private void updateContacts(Context context) throws DatabaseFileException, IOException {
		mContacts.clear();
		
		Conversation conv = Header.getHeader().getFirstConversation();
		while (conv != null) {
			if (Common.getSessionKeysForSIM(this, conv) != null)
				mContacts.add(conv);
			conv = conv.getNextConversation();
		}
	}
	
	private void startConversation(Conversation conv) {
		Intent intent = new Intent(TabContacts.this, ConversationActivity.class);
		intent.putExtra("phoneNumber", conv.getPhoneNumber());
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
		} catch (DatabaseFileException ex) {
			Common.dialogDatabaseError(this, ex);
		} catch (IOException ex) {
			Common.dialogIOError(this, ex);
		}
	}

	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_main_recent);

        final Context context = this;
        final ListView listView = getListView();
        final LayoutInflater inflater = LayoutInflater.from(this);
        
        // set appearance of list view
		listView.setFastScrollEnabled(true);
		
        // register for changes in SIM state
        Common.registerSimStateListener(this, new OnSimStateListener() {
			@Override
			public void onChange() {
				checkResources();
			}
		});
        

        // the New contact header
		mNewContactView = (TabContactsItem) inflater.inflate(R.layout.item_main_contacts, listView, false);
        listView.addHeaderView(mNewContactView, null, true);
       
        try {
        	// initialize the list of Contacts
        	updateContacts(getApplicationContext());
        	// create the adapter
        	final ArrayAdapter<Conversation> adapterContacts = new ArrayAdapter<Conversation>(this, R.layout.item_main_contacts, mContacts) {
        		@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					TabContactsItem row;

					if (convertView == null)
						row = (TabContactsItem) inflater.inflate(R.layout.item_main_contacts, listView, false);
					else
						row = (TabContactsItem) convertView;
				    
					row.bind(getItem(position));
					return row;
				}
			};
        	// add listeners			
        	Conversation.addUpdateListener(new ConversationUpdateListener() {
				public void onUpdate() {
					try {
						updateContacts(getApplicationContext());
						adapterContacts.notifyDataSetChanged();
					} catch (DatabaseFileException ex) {
						Common.dialogDatabaseError(context, ex);
					} catch (IOException ex) {
						Common.dialogIOError(context, ex);
					}
				}
			});
        	// set adapter
        	mAdapterContacts = adapterContacts;
			setListAdapter(mAdapterContacts);
			// specify what to do when clicked on items
			listView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,	int arg2, long arg3) {
					// check that the SIM is available
					if (!Common.checkSimPhoneNumberAvailable(context))
						return;
					
					TabContactsItem item = (TabContactsItem) arg1;
					Conversation conv;
		    		if ((conv = item.getConversationHeader()) != null) {
			    		// clicked on a conversation
						try {
							SessionKeys keys = Common.getSessionKeysForSIM(context, conv);
			    			if (keys != null && keys.getStatus() == SessionKeysStatus.KEYS_EXCHANGED) 
			    				startConversation(conv);
						} catch (DatabaseFileException ex) {
							Common.dialogDatabaseError(context, ex);							// TODO Auto-generated catch block
						} catch (IOException ex) {
							Common.dialogIOError(context, ex);
						}
		    		} else {
		    			// clicked on the header
		    			// pick a contact from PKI
						Intent intent = new Intent("uk.ac.cam.PKI.getcontact");
				        intent.putExtra("Criteria", "in_visible_group=1 AND " + Data.MIMETYPE + "='" + KEY_MIME + "'");
				        intent.putExtra("sort", "display_name COLLATE LOCALIZED ASC");
						intent.putExtra("pick", true);
						startActivityForResult(intent, NEW_CONTACT);
		    		}
				}
			});
		} catch (DatabaseFileException ex) {
			Common.dialogDatabaseError(this, ex);
		} catch (IOException ex) {
			Common.dialogIOError(this, ex);
		}
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	final Context context = getApplicationContext();
    	Resources res = context.getResources();
    	
    	switch (requestCode) {
    	case NEW_CONTACT:
    		if (resultCode == Activity.RESULT_OK) {
    			long contactId = data.getLongExtra("contact", 0);
    			if (contactId > 0) {
    				// get phone numbers associated with the contact
    				final ArrayList<Contact.PhoneNumber> phoneNumbers = 
    					Contact.getPhoneNumbers(context, contactId);

    				if (phoneNumbers.size() > 1) {
        				final CharSequence[] items = new CharSequence[phoneNumbers.size()];
    					for (int i = 0; i < phoneNumbers.size(); ++i)
    						items[i] = phoneNumbers.get(i).toString();
	
	    				// display them in a dialog
	    				new AlertDialog.Builder(this)
	    					.setTitle(res.getString(R.string.contacts_pick_phone_number))
	    					.setItems(items, new DialogInterface.OnClickListener() {
		    				    public void onClick(DialogInterface dialog, int item) {
		    				    	Contact.PhoneNumber phoneNumber = phoneNumbers.get(item);
		    				    	startConversation(phoneNumber.getPhoneNumber());
		    				    }
		    				})
	    					.show();
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

	
	private void checkResources() {
		// check SIM availability
    	if (Common.checkSimPhoneNumberAvailable(this)) {
    		if (getListAdapter() == null)
    			setListAdapter(mAdapterContacts);
    		mNewContactView.bind(getString(R.string.tab_contacts_new_contact), getString(R.string.tab_contacts_new_contact_details));
    	} else {
    		setListAdapter(null);
            mNewContactView.bind(getString(R.string.tab_contacts_not_available), getString(R.string.tab_contacts_not_available_details));
    	}
	}
	
    public void onResume() {
    	super.onResume();
    	checkResources();
    }
}
