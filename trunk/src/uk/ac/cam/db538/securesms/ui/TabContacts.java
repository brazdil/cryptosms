package uk.ac.cam.db538.securesms.ui;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.data.Contact;
import uk.ac.cam.db538.securesms.data.DummyOnClickListener;
import uk.ac.cam.db538.securesms.data.SimCard;
import uk.ac.cam.db538.securesms.data.SimCard.OnSimStateListener;
import uk.ac.cam.db538.securesms.data.Utils;
import uk.ac.cam.db538.securesms.storage.Conversation;
import uk.ac.cam.db538.securesms.storage.StorageFileException;
import uk.ac.cam.db538.securesms.storage.Header;
import uk.ac.cam.db538.securesms.storage.SessionKeys;
import uk.ac.cam.db538.securesms.storage.Conversation.ConversationUpdateListener;
import uk.ac.cam.db538.securesms.storage.SessionKeys.SessionKeysStatus;
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
	
	private void updateContacts() throws StorageFileException, IOException {
		mContacts.clear();
		
		Conversation conv = Header.getHeader().getFirstConversation();
		while (conv != null) {
			if (conv.getSessionKeysForSIM(this) != null)
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
			Utils.dialogDatabaseError(this, ex);
		} catch (IOException ex) {
			Utils.dialogIOError(this, ex);
		}
	}

	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_main_recent);

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
						Utils.dialogDatabaseError(context, ex);
					} catch (IOException ex) {
						Utils.dialogIOError(context, ex);
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
						Utils.dialogDatabaseError(context, ex);
						return;
					} catch (IOException ex) {
						Utils.dialogIOError(context, ex);
						return;
					}
					
					TabContactsItem item = (TabContactsItem) view;
					Conversation conv;
		    		if ((conv = item.getConversationHeader()) != null) {
			    		// clicked on a conversation
						try {
							SessionKeys keys = conv.getSessionKeysForSIM(context);
			    			if (keys != null && keys.getStatus() == SessionKeysStatus.KEYS_EXCHANGED) 
			    				startConversation(conv);
						} catch (StorageFileException ex) {
							Utils.dialogDatabaseError(context, ex);							// TODO Auto-generated catch block
						} catch (IOException ex) {
							Utils.dialogIOError(context, ex);
						}
		    		} else {
		    			// clicked on the header
		    			
		    			// pick a contact from PKI
						Intent intent = new Intent("uk.ac.cam.PKI.getcontact");
				        intent.putExtra("Criteria", "in_visible_group=1 AND " + Data.MIMETYPE + "='" + KEY_MIME + "'");
				        intent.putExtra("sort", "display_name COLLATE LOCALIZED ASC");
						intent.putExtra("pick", true);
						try {
							startActivityForResult(intent, NEW_CONTACT);
	    				} catch(ActivityNotFoundException e) {
	    					// PKI not installed
	    					new AlertDialog.Builder(context)
	    					.setTitle(res.getString(R.string.error_pki_unavailable))
	    					.setMessage(res.getString(R.string.error_pki_unavailable_details))
	    					.setNegativeButton(res.getString(R.string.cancel), new DummyOnClickListener())
	    					.setPositiveButton(res.getString(R.string.to_market), new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,	int which) {
			    					Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=uk.ac.cam.PKI"));
			    					try {
			    						startActivity(market);
			    					} catch(ActivityNotFoundException e2) {
			    						// doesn't have Market
				    					new AlertDialog.Builder(context)
				    						.setTitle(res.getString(R.string.error_market_unavailable))
				    						.setMessage(res.getString(R.string.error_market_unavailable_details))
				    						.setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
				    						.show();
			    					}
								}
	    					})
	    					.show();
	    					
	    				}
		    		}
				}
			});
		} catch (StorageFileException ex) {
			Utils.dialogDatabaseError(this, ex);
		} catch (IOException ex) {
			Utils.dialogIOError(this, ex);
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
			Utils.dialogDatabaseError(this, ex);
		} catch (IOException ex) {
			Utils.dialogIOError(this, ex);
		}
	}
	
    public void onResume() {
    	super.onResume();
    	checkResources();
    }
}
