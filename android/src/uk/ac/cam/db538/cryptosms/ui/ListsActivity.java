package uk.ac.cam.db538.cryptosms.ui;

import java.util.ArrayList;
import java.util.Collections;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.Header;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.storage.Conversation.ConversationsChangeListener;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;

public class ListsActivity extends StateAwareActivity {
	private static final int ACTIVITY_NEW_CONTACT = 1;

	private static final int MENU_MOVE_SESSIONS = Menu.FIRST;
	
	private static final String DIALOG_PHONE_NUMBER_PICKER = "DIALOG_PHONE_NUMBER_PICKER";
	private static final String PARAMS_PHONE_NUMBER_PICKER_ID = "PARAMS_PHONE_NUMBER_PICKER_ID";
	private static final String DIALOG_NO_PHONE_NUMBERS = "DIALOG_NO_PHONE_NUMBERS";

	private TabHost mTabHost;
	private LayoutInflater mInflater;
	
	private TabSpec mSpecRecent;
	private ListView mListRecent;
	private ArrayList<Conversation> mRecent = new ArrayList<Conversation>();
	private ArrayAdapter<Conversation> mAdapterRecent;

	private TabSpec mSpecContacts;
	private ListView mListContacts;
	private ArrayList<Conversation> mContacts = new ArrayList<Conversation>();
	private ListItemContact mNewContactView;
	private ArrayAdapter<Conversation> mAdapterContacts;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.screen_main);
	    
	    mInflater = getLayoutInflater();
	    Resources res = getResources();
	    
	    mTabHost = (TabHost) findViewById(R.id.screen_main_tabhost);
	    mTabHost.setup();

	    // TAB OF RECENT CONVERSATIONS
	    mSpecRecent = mTabHost.newTabSpec("recent")
	                          .setIndicator(res.getString(R.string.tab_recent), res.getDrawable(R.drawable.tab_recent))
	                          .setContent(new TabContentFactory() {
	                        	  	@Override
									public View createTabContent(String tag) {
	                        	  		mListRecent = (ListView) mInflater.inflate(R.layout.screen_main_listtab, mTabHost.getTabContentView(), false);
	                        	        // set appearance of list view
	                        		    mListRecent.setFastScrollEnabled(true);
	                        	    	// create the adapter
	                        	    	mAdapterRecent = new ArrayAdapter<Conversation>(ListsActivity.this, R.layout.item_main_contacts, mRecent) {
	                        	    		@Override
	                        				public View getView(int position, View convertView, ViewGroup parent) {
	                        					ListItemRecent row;

	                        					if (convertView == null)
	                        						row = (ListItemRecent) mInflater.inflate(R.layout.item_main_recent, mListRecent, false);
	                        					else
	                        						row = (ListItemRecent) convertView;
	                        				    
	                        					row.bind(getItem(position));
	                        					return row;
	                        				}
	                        			};
	                        			// specify what to do when clicked on items
	                        			mListRecent.setOnItemClickListener(new OnItemClickListener() {
	                        				@Override
	                        				public void onItemClick(AdapterView<?> adapterView, View view,	int arg2, long arg3) {
	                        					ListItemRecent item = (ListItemRecent) view;
	                        					Conversation conv;
	                        		    		if ((conv = item.getConversationHeader()) != null) {
	                        			    		// clicked on a conversation
	                        	    				startConversation(conv);
	                        		    		}
	                        				}
	                        			});
	                        			return mListRecent;
									}
	                          });
	    mTabHost.addTab(mSpecRecent);
	    
	    // TAB OF CONTACTS
	    mSpecContacts = mTabHost.newTabSpec("contacts")
	                          	.setIndicator(res.getString(R.string.tab_contacts), res.getDrawable(R.drawable.tab_contacts))
	                          	.setContent(new TabContentFactory() {
	                        	  	@Override
									public View createTabContent(String tag) {
	                        	  		mListContacts = (ListView) mInflater.inflate(R.layout.screen_main_listtab, mTabHost.getTabContentView(), false);

	                        	        // set appearance of list view
	                        	        mListContacts.setFastScrollEnabled(true);
	                        	        // the New contact header
	                        			mNewContactView = (ListItemContact) mInflater.inflate(R.layout.item_main_contacts, mListContacts, false);
	                        			mListContacts.addHeaderView(mNewContactView, null, true);
	                        			// specify what to do when clicked on items
	                        			mListContacts.setOnItemClickListener(new OnItemClickListener() {
	                        				@Override
	                        				public void onItemClick(AdapterView<?> adapterView, View view,	int arg2, long arg3) {
	                        					if (!SimCard.getSingleton().isNumberAvailable())
	                        						return;
	                        					
	                        					ListItemContact item = (ListItemContact) view;
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
	                        	    	mAdapterContacts = new ArrayAdapter<Conversation>(ListsActivity.this, R.layout.item_main_contacts, mContacts) {
	                        	    		@Override
	                        				public View getView(int position, View convertView, ViewGroup parent) {
	                        					ListItemContact row;

	                        					if (convertView == null)
	                        						row = (ListItemContact) mInflater.inflate(R.layout.item_main_contacts, mListContacts, false);
	                        					else
	                        						row = (ListItemContact) convertView;
	                        				    
	                        					row.bind(getItem(position));
	                        					return row;
	                        				}
	                        			};
	                        			onSimState();
	                        			return mListContacts;
									}
	                          	});
	    mTabHost.addTab(mSpecContacts);
		
        // PREPARE DIALOGS
		getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = ListsActivity.this.getResources();
				
				// get phone numbers associated with the contact
				final ArrayList<Contact.PhoneNumber> phoneNumbers = 
					Contact.getPhoneNumbers(ListsActivity.this, params.getLong(PARAMS_PHONE_NUMBER_PICKER_ID));

				final CharSequence[] items = new CharSequence[phoneNumbers.size()];
				for (int i = 0; i < phoneNumbers.size(); ++i)
					items[i] = phoneNumbers.get(i).toString();

				// display them in a dialog
				return new AlertDialog.Builder(ListsActivity.this)
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
		getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = ListsActivity.this.getResources();
				return new AlertDialog.Builder(ListsActivity.this)
					   .setTitle(res.getString(R.string.contacts_no_phone_numbers))
					   .setMessage(res.getString(R.string.contacts_no_phone_numbers_details))
					   .setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
					   .create();
			}
			
			@Override
			public String getId() {
				return DIALOG_NO_PHONE_NUMBERS;
			}
		});
        Utils.prepareDialogs(getDialogManager(), this);
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if (!Pki.isLoggedIn())
    		return;
    	
    	Resources res = this.getResources();
    	
    	switch (requestCode) {
    	case ACTIVITY_NEW_CONTACT:
    		if (resultCode == Activity.RESULT_OK) {
    			long contactId = data.getLongExtra("contact", 0);
    			String contactKey = data.getStringExtra("key name");
    			if (contactId > 0 && contactKey != null) {
    				// get phone numbers associated with the contact
    				final ArrayList<Contact.PhoneNumber> phoneNumbers = Contact.getPhoneNumbers(this, contactId);
    				if (phoneNumbers.size() > 1) {
    					Bundle params = new Bundle();
    					params.putLong(PARAMS_PHONE_NUMBER_PICKER_ID, contactId);
    					getDialogManager().showDialog(DIALOG_PHONE_NUMBER_PICKER, params);
    				} else if (phoneNumbers.size() == 1) {
    					startConversation(phoneNumbers.get(0).getPhoneNumber());
    				} else {
    					// no phone numbers assigned to the contact
    					getDialogManager().showDialog(DIALOG_NO_PHONE_NUMBERS, null);
    				}
    			}
    		}
    		break;
    	}
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		Resources res = this.getResources();
		
		int idGroup = 0;
		MenuItem menuMoveSessions = menu.add(idGroup, MENU_MOVE_SESSIONS, Menu.NONE, res.getString(R.string.menu_move_sessions));
		menuMoveSessions.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Utils.moveSessionKeys(getDialogManager());                                 
				return true;
			}
		});
		return true;
	}

	@Override
	public void onSimState() {
		super.onSimState();
		
		Utils.handleSimIssues(this, getDialogManager());
		
		// check SIM availability
		if (mListContacts != null) {
			if (SimCard.getSingleton().isNumberAvailable()) {
				mListContacts.setAdapter(mAdapterContacts);
	    		mNewContactView.bind(getString(R.string.tab_contacts_new_contact), getString(R.string.tab_contacts_new_contact_details));
			} else {
				mListContacts.setAdapter(null);
		        mNewContactView.bind(getString(R.string.tab_contacts_not_available), getString(R.string.tab_contacts_not_available_details));
		    }
		}
	}

	@Override
	public void onPkiLogin() {
		super.onPkiLogin();
		if (mListRecent != null) mListRecent.setAdapter(mAdapterRecent);
		if (mListContacts != null) mListContacts.setAdapter(mAdapterContacts);
	}

	@Override
	public void onPkiLogout() {
		super.onPkiLogout();
		if (mListRecent != null) mListRecent.setAdapter(null);
		if (mListContacts != null) mListContacts.setAdapter(null);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Conversation.addListener(mConversationChangeListener);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Conversation.removeListener(mConversationChangeListener);
	}

	private void startConversation(Conversation conv) {
		Intent intent = new Intent(ListsActivity.this, ConversationActivity.class);
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
	
	private ConversationsChangeListener mConversationChangeListener = new ConversationsChangeListener() {
		
		@Override
		public void onUpdate() {
			try {
				// update lists
	    		mRecent.clear();
				mContacts.clear();
				
	    		Conversation conv = Header.getHeader().getFirstConversation();
	    		while (conv != null) {
	    			if (conv.getFirstMessageData() != null)
	    				mRecent.add(conv);
					if (StorageUtils.hasKeysForSim(conv))
						mContacts.add(conv);
	    			conv = conv.getNextConversation();
	    		}

	    		Collections.sort(mRecent, Collections.reverseOrder());
	    		// TODO: sort the contacts by name
	    		
	    		if (mAdapterRecent != null) mAdapterRecent.notifyDataSetChanged();
	    		if (mAdapterContacts != null) mAdapterContacts.notifyDataSetChanged();
			} catch (StorageFileException ex) {
				State.fatalException(ex);
				return;
			}
		}
	};
}
