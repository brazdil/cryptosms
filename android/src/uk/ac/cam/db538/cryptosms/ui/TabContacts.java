package uk.ac.cam.db538.cryptosms.ui;

import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.DummyOnClickListener;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.data.Utils;
import uk.ac.cam.db538.cryptosms.data.SimCard.OnSimStateListener;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.Header;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.storage.Conversation.ConversationUpdateListener;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys.SessionKeysStatus;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class TabContacts extends ListActivity {
	private static final int NEW_CONTACT = 1;
	
	private ArrayList<Conversation> mContacts = new ArrayList<Conversation>();;
	private TabContactsItem mNewContactView;
	private ArrayAdapter<Conversation> mAdapterContacts;
	
	private void updateContacts() throws StorageFileException {
		mContacts.clear();
		
		Conversation conv = Header.getHeader().getFirstConversation();
		while (conv != null) {
			if (StorageUtils.getSessionKeysForSIM(conv, this) != null)
				mContacts.add(conv);
			conv = conv.getNextConversation();
		}
	}
	
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

	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_main_listtab);

        final Context context = this;
        final Resources res = getResources();
        final ListView listView = getListView();
        final LayoutInflater inflater = LayoutInflater.from(this);
        
        // set appearance of list view
		listView.setFastScrollEnabled(true);
		
        // register for changes in SIM state
        SimCard.getSingleton().registerSimStateListener(this, new OnSimStateListener() {
			public void onChange() {
				checkResources();
			}
		});
        

        // the New contact header
		mNewContactView = (TabContactsItem) inflater.inflate(R.layout.item_main_contacts, listView, false);
        listView.addHeaderView(mNewContactView, null, true);
       
        try {
        	// initialize the list of Contacts
        	updateContacts();
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
						updateContacts();
						adapterContacts.notifyDataSetChanged();
					} catch (StorageFileException ex) {
						State.fatalException(ex);
						return;
					}
				}
			});
        	// set adapter
        	mAdapterContacts = adapterContacts;
			setListAdapter(mAdapterContacts);
			// specify what to do when clicked on items
			listView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> adapterView, View view,	int arg2, long arg3) {
					// check that the SIM is available
					try {
						if (!Utils.checkSimPhoneNumberAvailable(context))
							return;
					} catch (StorageFileException ex) {
						State.fatalException(ex);
						return;
					}
					
					TabContactsItem item = (TabContactsItem) view;
					Conversation conv;
		    		if ((conv = item.getConversationHeader()) != null) {
			    		// clicked on a conversation
						try {
							SessionKeys keys = StorageUtils.getSessionKeysForSIM(conv, context);
			    			if (keys != null && keys.getStatus() == SessionKeysStatus.KEYS_EXCHANGED) 
			    				startConversation(conv);
						} catch (StorageFileException ex) {
							State.fatalException(ex);
							return;
						}
		    		} else {
		    			// clicked on the header
		    			
		    			// pick a contact from PKI
						Intent intent = new Intent("uk.ac.cam.PKI.getcontact");
				        intent.putExtra("Contact Criteria", "in_visible_group=1");
				        intent.putExtra("Key Criteria", "contact_id IN (SELECT contact_id FROM keys GROUP BY contact_id HAVING COUNT(key_name)>0)");
				        intent.putExtra("sort", "display_name COLLATE LOCALIZED ASC");
						intent.putExtra("pick", true);
						try {
							startActivityForResult(intent, NEW_CONTACT);
	    				} catch(ActivityNotFoundException e) {
	    					// TODO: PKI unavailable
	    				}
		    		}
				}
			});
		} catch (StorageFileException ex) {
			State.fatalException(ex);
			return;
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
		try {
	    	if (Utils.checkSimPhoneNumberAvailable(this)) {
	    		if (getListAdapter() == null)
	    			setListAdapter(mAdapterContacts);
	    		mNewContactView.bind(getString(R.string.tab_contacts_new_contact), getString(R.string.tab_contacts_new_contact_details));
	    	} else {
	    		setListAdapter(null);
	            mNewContactView.bind(getString(R.string.tab_contacts_not_available), getString(R.string.tab_contacts_not_available_details));
	    	}
		} catch (StorageFileException ex) {
			State.fatalException(ex);
		}
	}
	
    public void onResume() {
    	super.onResume();
    }
}
