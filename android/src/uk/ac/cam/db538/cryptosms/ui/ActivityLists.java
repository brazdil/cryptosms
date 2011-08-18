package uk.ac.cam.db538.cryptosms.ui;

import java.util.ArrayList;
import java.util.Collections;

import roboguice.inject.InjectView;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.DbPendingAdapter;
import uk.ac.cam.db538.cryptosms.data.PendingParser;
import uk.ac.cam.db538.cryptosms.data.PendingParser.Event;
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;

public class ActivityLists extends ActivityAppState {
	private static final int ACTIVITY_NEW_CONTACT = 1;
	private static final int ACTIVITY_CHOOSE_KEY = 2;
	
	private static final String TAB_CONVERSATIONS = "CONVERSATIONS";
	private static final String TAB_CONTACTS = "CONTACTS";
	private static final String TAB_EVENTS = "EVENTS";

	private static final int MENU_MOVE_SESSIONS = Menu.FIRST;
	private static final int MENU_PROCESS_PENDING = MENU_MOVE_SESSIONS + 1;
	
	private static final String DIALOG_PHONE_NUMBER_PICKER = "DIALOG_PHONE_NUMBER_PICKER";
	private static final String PARAMS_PHONE_NUMBER_PICKER_ID = "PARAMS_PHONE_NUMBER_PICKER_ID";
	private static final String PARAMS_PHONE_NUMBER_PICKER_KEY_NAME = "PARAMS_PHONE_NUMBER_PICKER_KEY_NAME";
	private static final String DIALOG_NO_PHONE_NUMBERS = "DIALOG_NO_PHONE_NUMBERS";
	private static final String DIALOG_CONFIRM_INVALIDATION = "DIALOG_CONFIRM_INVALIDATION";
	private static final String PARAMS_CONFIRM_INVALIDATION_PHONE_NUMBER = "PARAMS_CONFIRM_INVALIDATION_PHONE_NUMBER";

	private LayoutInflater mInflater;
	
	@InjectView(R.id.tab_host)
	private TabHost mTabHost;

	private TabSpec mSpecConversations;
	private ListView mListConversations;
	private AdapterConversations mAdapterConversations;

	private TabSpec mSpecContacts;
	private ListView mListContacts;
	private ListItemContact mNewContactView;
	private AdapterContacts mAdapterContacts;
	
	private TabSpec mSpecEvents;
	private ListView mListEvents;
	private ListItemEvent mClearPendingView;
	private AdapterEvents mAdapterEvents;
	
	// TODO: listen to contact name changes
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.screen_lists);
	    
	    mInflater = getLayoutInflater();
	    Resources res = getResources();
	    
	    mTabHost.setup();
	    
	    // TAB OF RECENT CONVERSATIONS
	    mSpecConversations = mTabHost.newTabSpec(TAB_CONVERSATIONS)
	                          .setIndicator(res.getString(R.string.tab_conversations), res.getDrawable(R.drawable.tab_conversations))
	                          .setContent(new TabContentFactory() {
	                        	  	@Override
									public View createTabContent(String tag) {
	                        	  		mListConversations = (ListView) mInflater.inflate(R.layout.view_listtab, mTabHost.getTabContentView(), false);
	                        	        // set appearance of list view
	                        		    mListConversations.setFastScrollEnabled(true);
	                        	    	// create the adapter
	                        	    	mAdapterConversations = new AdapterConversations(mInflater, mListConversations); 
	                        	    	//specify what to do when clicked on items
	                        			mListConversations.setOnItemClickListener(new OnItemClickListener() {
	                        				@Override
	                        				public void onItemClick(AdapterView<?> adapterView, View view,	int arg2, long arg3) {
	                        					ListItemConversation item = (ListItemConversation) view;
	                        					Conversation conv;
	                        		    		if ((conv = item.getConversationHeader()) != null) {
	                        			    		// clicked on a conversation
	                        	    				startConversation(conv);
	                        		    		}
	                        				}
	                        			});
	                        			// prepare for context menus
	                        			ActivityLists.this.registerForContextMenu(mListConversations);
	                        			return mListConversations;
									}
	                          });
	    mTabHost.addTab(mSpecConversations);
	    // force it to inflate the UI
	    mTabHost.setCurrentTabByTag(TAB_CONVERSATIONS);

	    // TAB OF CONTACTS
	    mSpecContacts = mTabHost.newTabSpec(TAB_CONTACTS)
	                          	.setIndicator(res.getString(R.string.tab_contacts), res.getDrawable(R.drawable.tab_contacts))
	                          	.setContent(new TabContentFactory() {
	                        	  	@Override
									public View createTabContent(String tag) {
	                        	  		mListContacts = (ListView) mInflater.inflate(R.layout.view_listtab, mTabHost.getTabContentView(), false);

	                        	        // set appearance of list view
	                        	        mListContacts.setFastScrollEnabled(true);
	                        	        // the New contact header
	                        			mNewContactView = (ListItemContact) mInflater.inflate(R.layout.item_main_contact, mListContacts, false);
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
	                        		    			Context context = ActivityLists.this;
	                        		    			Resources res = context.getResources();
	                        		    			
	                        		    			// pick a contact from PKI
	                        						Intent intent = new Intent(MyApplication.PKI_CONTACT_PICKER);
	                        						intent.putExtra("pick", true);
	                        				        intent.putExtra("Contact Criteria", "in_visible_group=1");
	                        				        intent.putExtra("Key Criteria", "contact_id IN (SELECT contact_id FROM keys GROUP BY contact_id HAVING COUNT(key_name)>0)");
	                        				        intent.putExtra("sort", "display_name COLLATE LOCALIZED ASC");
	                        				        intent.putExtra("empty", res.getString(R.string.pki_contact_picker_empty) );
	                        						try {
	                        							startActivityForResult(intent, ACTIVITY_NEW_CONTACT);
	                        	    				} catch(ActivityNotFoundException e) {
	                        	    					State.notifyPkiMissing();
	                        	    				}
	                        		    		}
	                        				}
	                        			});
	                        	    	// create the adapter
	                        	    	mAdapterContacts = new AdapterContacts(mInflater, mListContacts);
	                        			// prepare for context menus
	                        			ActivityLists.this.registerForContextMenu(mListContacts);
	                        			return mListContacts;
									}
	                          	});
	    mTabHost.addTab(mSpecContacts);
	    // force it to inflate the UI
	    mTabHost.setCurrentTabByTag(TAB_CONTACTS);

	    // TAB OF EVENTS
	    mSpecEvents = mTabHost.newTabSpec(TAB_EVENTS)
	                          .setIndicator(res.getString(R.string.tab_events), res.getDrawable(R.drawable.tab_events))
	                          .setContent(new TabContentFactory() {
	                        	  	@Override
									public View createTabContent(String tag) {
	                        	  		mListEvents = (ListView) mInflater.inflate(R.layout.view_listtab, mTabHost.getTabContentView(), false);
	                        	        // set appearance of list view
	                        		    mListEvents.setFastScrollEnabled(true);
	                        	        // the Clear pending header
	                        			mClearPendingView = (ListItemEvent) mInflater.inflate(R.layout.item_main_event, mListEvents, false);
	                        			mListEvents.addHeaderView(mClearPendingView, null, true);
	                        	    	// create the adapter
	                        	    	mAdapterEvents = new AdapterEvents(mInflater, mListEvents);
	                        			// specify what to do when clicked on items
	                        			mListEvents.setOnItemClickListener(new OnItemClickListener() {
	                        				@Override
	                        				public void onItemClick(AdapterView<?> adapterView, View view,	int arg2, long arg3) {
	                        				}
	                        			});
	                        			// prepare for context menus
	                        			ActivityLists.this.registerForContextMenu(mListEvents);
	                        			return mListEvents;
									}
	                          });
	    mTabHost.addTab(mSpecEvents);
	    // force it to inflate the UI
	    mTabHost.setCurrentTabByTag(TAB_EVENTS);
	    
	    // select the Recent tab
	    mTabHost.setCurrentTabByTag(TAB_CONVERSATIONS);
	    
	    // PREPARE DIALOGS
		getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = ActivityLists.this.getResources();
				
				final long contactId = params.getLong(PARAMS_PHONE_NUMBER_PICKER_ID);
				final String keyName = params.getString(PARAMS_PHONE_NUMBER_PICKER_KEY_NAME);
				
				// get phone numbers associated with the contact
				final ArrayList<Contact.PhoneNumber> phoneNumbers = 
					Contact.getPhoneNumbers(ActivityLists.this, contactId);

				final CharSequence[] items = new CharSequence[phoneNumbers.size()];
				for (int i = 0; i < phoneNumbers.size(); ++i)
					items[i] = phoneNumbers.get(i).toString();

				// display them in a dialog
				return new AlertDialog.Builder(ActivityLists.this)
				       .setTitle(res.getString(R.string.contacts_pick_phone_number))
				       .setItems(items, new DialogInterface.OnClickListener() {
				    	   @Override
				    	   public void onClick(DialogInterface dialog, int item) {
				    		   Contact.PhoneNumber phoneNumber = phoneNumbers.get(item);
				    		   startKeyExchange(contactId, phoneNumber.getPhoneNumber(), keyName);
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
				Resources res = ActivityLists.this.getResources();
				return new AlertDialog.Builder(ActivityLists.this)
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
		getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				final String phoneNumber = params.getString(PARAMS_CONFIRM_INVALIDATION_PHONE_NUMBER);
				
				return new AlertDialog.Builder(ActivityLists.this)
				       .setTitle(R.string.invalidate_encryption)
				       .setMessage(R.string.invalidate_confirm)
				       .setPositiveButton(R.string.yes, new OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								try {
									Conversation conv = Conversation.getConversation(phoneNumber);
									conv.deleteSessionKeys(SimCard.getSingleton().getNumber());
								} catch (StorageFileException e) {
									State.fatalException(e);
									return;
								}
							}
				       })
				       .setNegativeButton(R.string.no, new DummyOnClickListener())
				       .create();
			}
			
			@Override
			public String getId() {
				return DIALOG_CONFIRM_INVALIDATION;
			}
		});
        UtilsSimIssues.prepareDialogs(getDialogManager(), this);
	}
	
	private long mTempContactId;
	private String mTempPhoneNumber;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

    	if (!Pki.isLoggedIn())
    		return;
    	
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
    					params.putString(PARAMS_PHONE_NUMBER_PICKER_KEY_NAME, contactKey);
    					getDialogManager().showDialog(DIALOG_PHONE_NUMBER_PICKER, params);
    				} else if (phoneNumbers.size() == 1) {
    					startKeyExchange(contactId, phoneNumbers.get(0).getPhoneNumber(), contactKey);
    				} else {
    					// no phone numbers assigned to the contact
    					getDialogManager().showDialog(DIALOG_NO_PHONE_NUMBERS, null);
    				}
    			}
    		}
    		break;
    	case ACTIVITY_CHOOSE_KEY:
    		if (resultCode == Activity.RESULT_OK) {
    			String keyName = data.getStringExtra("result");
				startKeyExchange(mTempContactId, mTempPhoneNumber, keyName);
    		}
    		break;
    	}
    }

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		if (v == mListContacts && info.id != -1) { 
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.lists_contacts_context, menu);	
		} else if (v == mListConversations) {
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
//		try {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			if (info.targetView instanceof ListItemContact && info.id != -1) {
				Conversation conv = ((ListItemContact) info.targetView).getConversationHeader();
				switch (item.getItemId()) {
				case R.id.resend_keys:
					Contact contact = Contact.getContact(this, conv.getPhoneNumber());
					mTempContactId = contact.getId();
					mTempPhoneNumber = contact.getPhoneNumber();
					
	    			// pick a key from PKI
					Intent intent = new Intent(MyApplication.PKI_KEY_PICKER);
			        intent.putExtra("contact", contact.getId());
			        intent.putExtra("empty", this.getResources().getString(R.string.pki_key_picker_empty) );
					try {
						startActivityForResult(intent, ACTIVITY_CHOOSE_KEY);
    				} catch(ActivityNotFoundException e) {
    					State.notifyPkiMissing();
    				}
					return true;
				case R.id.invalidate:
					Bundle params = new Bundle();
					params.putString(PARAMS_CONFIRM_INVALIDATION_PHONE_NUMBER, conv.getPhoneNumber());
					getDialogManager().showDialog(DIALOG_CONFIRM_INVALIDATION, params);
					return true;
				}
			} else if (info.targetView instanceof ListItemConversation) {
				switch (item.getItemId()) {
				}
			}
			return super.onContextItemSelected(item);
//		} catch (StorageFileException e) {
//			State.fatalException(e);
//			return true;
//		}
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
				UtilsSimIssues.moveSessionKeys(getDialogManager());                                 
				return true;
			}
		});
		MenuItem menuProcessPending = menu.add(idGroup, MENU_PROCESS_PENDING, Menu.NONE, res.getString(R.string.menu_process_pending));
		menuProcessPending.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				ActivityLists.this.onNewEvent();
				return true;
			}
		});
		return true;
	}

	@Override
	public void onSimState() {
		super.onSimState();
		
		UtilsSimIssues.handleSimIssues(this, getDialogManager());
		
		// check SIM availability
		if (SimCard.getSingleton().isNumberAvailable()) {
			mListEvents.setAdapter(mAdapterEvents);
			mListContacts.setAdapter(mAdapterContacts);
    		mNewContactView.bind(getString(R.string.tab_contacts_new_contact), getString(R.string.tab_contacts_new_contact_details));
    		mClearPendingView.bind(getString(R.string.clear_pending), getString(R.string.clear_pending_details));
    		
			new ConversationsUpdateTask().execute();
			new EventsUpdateTask().execute();
		} else {
			mListEvents.setAdapter(null);
			mListContacts.setAdapter(null);
	        mNewContactView.bind(getString(R.string.tab_contacts_not_available), getString(R.string.tab_contacts_not_available_details));
	        mClearPendingView.bind(getString(R.string.tab_contacts_not_available), getString(R.string.tab_contacts_not_available_details));
	    }
	}

	@Override
	public void onPkiLogin() {
		super.onPkiLogin();
		Log.d(MyApplication.APP_TAG, "Login");
		mListConversations.setAdapter(mAdapterConversations);
		mListContacts.setAdapter(mAdapterContacts);
		mListEvents.setAdapter(mAdapterEvents);
	}

	@Override
	public void onPkiLogout() {
		super.onPkiLogout();
		mListConversations.setAdapter(null);
		mListContacts.setAdapter(null);
		mListEvents.setAdapter(null);
	}

	@Override
	public void onNewEvent() {
		super.onNewEvent();
		new EventsUpdateTask().execute();
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
		Intent intent = new Intent(ActivityLists.this, ActivityConversation.class);
		intent.putExtra(ActivityConversation.OPTION_PHONE_NUMBER, conv.getPhoneNumber());
		startActivity(intent);
	}
	
	private void startKeyExchange(long contactId, String phoneNumber, String keyName) {
		Intent intent = new Intent(ActivityLists.this, ActivityExchangeMethod.class);
		intent.putExtra(ActivityExchangeMethod.OPTION_CONTACT_ID, contactId);
		intent.putExtra(ActivityExchangeMethod.OPTION_PHONE_NUMBER, phoneNumber);
		intent.putExtra(ActivityExchangeMethod.OPTION_CONTACT_KEY, keyName);
		startActivity(intent);
	}
	
	private ConversationsChangeListener mConversationChangeListener = new ConversationsChangeListener() {
		
		@Override
		public void onUpdate() {
			new ConversationsUpdateTask().execute();
		}
	};
	
	private class ConversationsUpdateTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				// update lists
				ArrayList<Conversation> listConversations = new ArrayList<Conversation>();
				ArrayList<Conversation> listContacts = new ArrayList<Conversation>();
				
	    		Conversation conv = Header.getHeader().getFirstConversation();
	    		while (conv != null) {
	    			if (conv.getFirstMessageData() != null)
	    				listConversations.add(conv);
					if (StorageUtils.hasKeysForSim(conv))
						listContacts.add(conv);
	    			conv = conv.getNextConversation();
	    		}

	    		Collections.sort(listConversations, Collections.reverseOrder());
	    		// TODO: sort the contacts by name
	    		
	    		mAdapterConversations.setList(listConversations);
	    		mAdapterContacts.setList(listContacts);
			} catch (StorageFileException ex) {
				State.fatalException(ex);
				return null;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
    		mAdapterConversations.notifyDataSetChanged();
    		mAdapterContacts.notifyDataSetChanged();
		}
	}

	private class EventsUpdateTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			// parse pending stuff
			DbPendingAdapter database = new DbPendingAdapter(ActivityLists.this);
			database.open();
			try {
				mAdapterEvents.setList(PendingParser.parsePending(database));
			} finally {
				database.close();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mAdapterEvents.notifyDataSetChanged();
		}
	}
}
