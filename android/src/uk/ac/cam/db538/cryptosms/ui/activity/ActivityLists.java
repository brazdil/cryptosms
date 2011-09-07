package uk.ac.cam.db538.cryptosms.ui.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import roboguice.inject.InjectView;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.DbPendingAdapter;
import uk.ac.cam.db538.cryptosms.data.KeysMessage;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageSendingListener;
import uk.ac.cam.db538.cryptosms.data.PendingParser;
import uk.ac.cam.db538.cryptosms.data.PendingParser.ParseResult;
import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.Header;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.storage.Storage.StorageChangeListener;
import uk.ac.cam.db538.cryptosms.ui.DummyOnClickListener;
import uk.ac.cam.db538.cryptosms.ui.UtilsSendMessage;
import uk.ac.cam.db538.cryptosms.ui.UtilsSimIssues;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;
import uk.ac.cam.db538.cryptosms.ui.adapter.AdapterContacts;
import uk.ac.cam.db538.cryptosms.ui.adapter.AdapterConversations;
import uk.ac.cam.db538.cryptosms.ui.adapter.AdapterNotifications;
import uk.ac.cam.db538.cryptosms.ui.list.ListItemContact;
import uk.ac.cam.db538.cryptosms.ui.list.ListItemConversation;
import uk.ac.cam.db538.cryptosms.ui.list.ListItemNotification;
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
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
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
	private static final String TAB_NOTIFICATIONS = "NOTIFICATIONS";

	private static final int MENU_MOVE_SESSIONS = Menu.FIRST;
	private static final int MENU_PROCESS_PENDING = MENU_MOVE_SESSIONS + 1;
	
	private static final String DIALOG_PHONE_NUMBER_PICKER = "DIALOG_PHONE_NUMBER_PICKER";
	private static final String PARAMS_PHONE_NUMBER_PICKER_ID = "PARAMS_PHONE_NUMBER_PICKER_ID";
	private static final String PARAMS_PHONE_NUMBER_PICKER_KEY_NAME = "PARAMS_PHONE_NUMBER_PICKER_KEY_NAME";
	private static final String DIALOG_NO_PHONE_NUMBERS = "DIALOG_NO_PHONE_NUMBERS";
	private static final String DIALOG_CONFIRM_INVALIDATION = "DIALOG_CONFIRM_INVALIDATION";
	private static final String PARAMS_CONFIRM_INVALIDATION_PHONE_NUMBER = "PARAMS_CONFIRM_INVALIDATION_PHONE_NUMBER";
	private static final String DIALOG_ACCEPT_KEYS_AND_CONFIRM = "DIALOG_ACCEPT_KEYS_AND_CONFIRM";
	private static final String DIALOG_CLEAR_PENDING = "DIALOG_CLEAR_PENDING";
	private static final String DIALOG_CLEAR_ALL_PENDING = "DIALOG_CLEAR_ALL_PENDING";
	
	private LayoutInflater mInflater;
	
	@InjectView(R.id.tab_host)
	private TabHost mTabHost;

	private TabSpec mSpecConversations;
	private ListView mListConversations;
	private View mListConversationsLoading;
	private AdapterConversations mAdapterConversations;

	private TabSpec mSpecContacts;
	private ListView mListContacts;
	private View mListContactsLoading;
	private ListItemContact mNewContactView;
	private AdapterContacts mAdapterContacts;
	
	private TabSpec mSpecNotifications;
	private ListView mListNotifications;
	private View mListNotificationsLoading;
	private ListItemNotification mClearPendingView;
	private AdapterNotifications mAdapterNotifications;
	
	private ParseResult mNotificationsContextMenuItem;
	
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
	                        	  		View layout = mInflater.inflate(R.layout.view_listtab, mTabHost.getTabContentView(), false);
	                        	  		mListConversations = (ListView) layout.findViewById(android.R.id.list);
	                        	  		mListConversationsLoading = layout.findViewById(R.id.loadingState);
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
	                        			return layout;
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
	                        	  		View layout = mInflater.inflate(R.layout.view_listtab, mTabHost.getTabContentView(), false);
	                        	  		mListContacts = (ListView) layout.findViewById(android.R.id.list);
	                        	  		mListContactsLoading = layout.findViewById(R.id.loadingState);
	                        	        // set appearance of list view
	                        	        mListContacts.setFastScrollEnabled(true);
	                        	        // the New contact header
	                        			mNewContactView = (ListItemContact) mInflater.inflate(R.layout.item_main_contact, mListContacts, false);
	                        			mListContacts.addHeaderView(mNewContactView, null, true);
	                        			// specify what to do when clicked on items
	                        			mListContacts.setOnItemClickListener(new OnItemClickListener() {
	                        				@Override
	                        				public void onItemClick(AdapterView<?> adapterView, View view,	int position, long id) {
	                        					if (!SimCard.getSingleton().isNumberAvailable())
	                        						return;
	                        					
	                        		    		if (id < 0) {
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
	                        		    		} else {
	                        			    		// clicked on a conversation
		                        					ListItemContact item = (ListItemContact) view;
		                        					Conversation conv = item.getConversationHeader();
	                        						try {
	                        							if (StorageUtils.hasKeysExchangedForSim(conv))
	                        			    				startConversation(conv);
	                        						} catch (StorageFileException ex) {
	                        							State.fatalException(ex);
	                        							return;
	                        						}
	                        		    		}
	                        				}
	                        			});
	                        	    	// create the adapter
	                        	    	mAdapterContacts = new AdapterContacts(mInflater, mListContacts);
	                        			// prepare for context menus
	                        			ActivityLists.this.registerForContextMenu(mListContacts);
	                        			return layout;
									}
	                          	});
	    mTabHost.addTab(mSpecContacts);
	    // force it to inflate the UI
	    mTabHost.setCurrentTabByTag(TAB_CONTACTS);

	    // TAB OF EVENTS
	    mSpecNotifications = mTabHost.newTabSpec(TAB_NOTIFICATIONS)
	                          .setIndicator(res.getString(R.string.tab_notifications), res.getDrawable(R.drawable.tab_events))
	                          .setContent(new TabContentFactory() {
	                        	  	@Override
									public View createTabContent(String tag) {
	                        	  		View layout = mInflater.inflate(R.layout.view_listtab, mTabHost.getTabContentView(), false);
	                        	  		mListNotifications = (ListView) layout.findViewById(android.R.id.list);
	                        	  		mListNotificationsLoading = layout.findViewById(R.id.loadingState);
	                        	        // set appearance of list view
	                        		    mListNotifications.setFastScrollEnabled(true);
	                        	        // the Clear pending header
	                        			mClearPendingView = (ListItemNotification) mInflater.inflate(R.layout.item_main_notification, mListNotifications, false);
	                        			Log.d(MyApplication.APP_TAG, "Adding header");
	                        			mListNotifications.addHeaderView(mClearPendingView, null, true);
	                        	    	// create the adapter
	                        	    	mAdapterNotifications = new AdapterNotifications(mInflater, mListNotifications);
	                        			// specify what to do when clicked on items
	                        			mListNotifications.setOnItemClickListener(new OnItemClickListener() {
	                        				@Override
	                        				public void onItemClick(AdapterView<?> adapterView, View view,	int position, long id) {
	                        		    		if (id < 0) {
	                        		    			// clicked on the Clear Pending header
	                        		    			getDialogManager().showDialog(DIALOG_CLEAR_ALL_PENDING, null);
	                        		    		} else {
	                        			    		// clicked on a notification
	                        		    			ListItemNotification notification = (ListItemNotification) view;
	                        		    			notification.performLongClick();
	                        		    		}
	                        				}
	                        			});
	                        			// prepare for context menus
	                        			ActivityLists.this.registerForContextMenu(mListNotifications);
	                        			return layout;
									}
	                          });
	    mTabHost.addTab(mSpecNotifications);
	    // force it to inflate the UI
	    mTabHost.setCurrentTabByTag(TAB_NOTIFICATIONS);
	    
	    // select the Recent tab
	    mTabHost.setCurrentTabByTag(TAB_CONVERSATIONS);
	    
	    // PREPARE DIALOGS
		getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = ActivityLists.this.getResources();
				
				final long contactId = params.getLong(PARAMS_PHONE_NUMBER_PICKER_ID);
				
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
				    		   startKeyExchange(phoneNumber.getPhoneNumber());
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
		getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				
				return new AlertDialog.Builder(ActivityLists.this)
				       .setCancelable(false)
				       .setTitle(R.string.accept_and_confirm)
				       .setMessage(R.string.accept_and_confirm_details)
				       .setPositiveButton(R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// check the input
								if (mNotificationsContextMenuItem == null ||
									mNotificationsContextMenuItem.getResult() != PendingParser.PendingParseResult.OK_HANDSHAKE_MESSAGE ||
									! (mNotificationsContextMenuItem.getMessage() instanceof KeysMessage))
									return;
								
								final KeysMessage keysMsg = (KeysMessage) mNotificationsContextMenuItem.getMessage();
								final String phoneNumber = mNotificationsContextMenuItem.getSender();
								
								ActivityLists.this.getDialogManager().dismissDialog(getId());
								ActivityLists.this.getDialogManager().showDialog(UtilsSendMessage.DIALOG_SENDING, null);
								
								// send confirmation
//								Log.d(MyApplication.APP_TAG, "Encrypting with " + LowLevel.toHex(keysMsg.getKeyOut()));
//								Log.d(MyApplication.APP_TAG, "NOT encrypting with " + LowLevel.toHex(keysMsg.getKeyIn()));
								try {
									keysMsg.sendSMS(phoneNumber, ActivityLists.this, new MessageSendingListener() {

										@Override
										public void onMessageSent() {
											// remove id group from database
											removeParseResult(mNotificationsContextMenuItem);
											
											ActivityLists.this.getDialogManager().dismissDialog(UtilsSendMessage.DIALOG_SENDING);
										}

										@Override
										public void onPartSent(int index) {
										}

										@Override
										public void onError(Exception ex) {
											ActivityLists.this.getDialogManager().dismissDialog(UtilsSendMessage.DIALOG_SENDING);
											Bundle params = new Bundle();
											params.putString(UtilsSendMessage.PARAM_SENDING_ERROR, ex.getMessage());
											getDialogManager().showDialog(UtilsSendMessage.DIALOG_SENDING_ERROR, params);
										}
										
									});
								} catch (StorageFileException e) {
									State.fatalException(e);
									return;
								} catch (MessageException e) {
									State.fatalException(e);
									return;
								} catch (EncryptionException e) {
									State.fatalException(e);
									return;
								}
							}
						})
						.setNegativeButton(R.string.cancel, new DummyOnClickListener())
						.create();
			}
			
			@Override
			public String getId() {
				return DIALOG_ACCEPT_KEYS_AND_CONFIRM;
			}
		});
		getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = ActivityLists.this.getResources();
				return new AlertDialog.Builder(ActivityLists.this)
					   .setTitle(res.getString(R.string.notifications_clear_pending))
					   .setMessage(res.getString(R.string.notifications_clear_pending_details))
					   .setNegativeButton(res.getString(R.string.no), new DummyOnClickListener())
					   .setPositiveButton(res.getString(R.string.yes), new OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// check the input
								if (mNotificationsContextMenuItem == null)
									return;
								
								removeParseResult(mNotificationsContextMenuItem);
							}
					   })
					   .create();
			}
			
			@Override
			public String getId() {
				return DIALOG_CLEAR_PENDING;
			}
		});
		getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = ActivityLists.this.getResources();
				return new AlertDialog.Builder(ActivityLists.this)
					   .setTitle(res.getString(R.string.notifications_clear_all_pending))
					   .setMessage(res.getString(R.string.notifications_clear_all_pending_details))
					   .setNegativeButton(res.getString(R.string.no), new DummyOnClickListener())
					   .setPositiveButton(res.getString(R.string.yes), new OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								DbPendingAdapter database = new DbPendingAdapter(ActivityLists.this);
								database.open();
								try {
									database.clear();
								} finally {
									database.close();
								}
								mAdapterNotifications.setList(new ArrayList<PendingParser.ParseResult>());
								State.notifyNewEvent();
							}
					   })
					   .create();
			}
			
			@Override
			public String getId() {
				return DIALOG_CLEAR_ALL_PENDING;
			}
		});
        UtilsSendMessage.prepareDialogs(getDialogManager(), this);
        UtilsSimIssues.prepareDialogs(getDialogManager(), this);
	}
	
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
    					startKeyExchange(phoneNumbers.get(0).getPhoneNumber());
    				} else {
    					// no phone numbers assigned to the contact
    					getDialogManager().showDialog(DIALOG_NO_PHONE_NUMBERS, null);
    				}
    			}
    		}
    		break;
    	case ACTIVITY_CHOOSE_KEY:
    		if (resultCode == Activity.RESULT_OK) {
				startKeyExchange(mTempPhoneNumber);
    		}
    		break;
    	}
    }

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Log.d(MyApplication.APP_TAG, "Item: " + info.id);
		if (v == mListContacts && info.id >= 0) { 
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.lists_contacts_context, menu);	
		} else if (v == mListConversations) {
		} else if (v == mListNotifications && info.id >= 0) {
			MenuInflater inflater = getMenuInflater();
			mNotificationsContextMenuItem = (ParseResult) mAdapterNotifications.getItem((int)info.id);
			switch (mNotificationsContextMenuItem.getResult()) {
			case OK_HANDSHAKE_MESSAGE:
				inflater.inflate(R.menu.lists_notifications_context_keys, menu);
				break;
			default:
				inflater.inflate(R.menu.lists_notifications_context, menu);
				break;
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		if (info.targetView instanceof ListItemContact && info.id != -1) {
			Conversation conv = ((ListItemContact) info.targetView).getConversationHeader();
			switch (item.getItemId()) {
			case R.id.resend_keys:
				Contact contact = Contact.getContact(this, conv.getPhoneNumber());
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
		} else if (info.targetView instanceof ListItemNotification) {
			switch (item.getItemId()) {
			case R.id.accept:
				getDialogManager().showDialog(DIALOG_ACCEPT_KEYS_AND_CONFIRM, null);
				break;
			case R.id.discard:
				getDialogManager().showDialog(DIALOG_CLEAR_PENDING, null);
				break;
			}
		}
		return super.onContextItemSelected(item);
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
				PendingParser.getSingleton().parseEvents();
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
			mListNotifications.setAdapter(mAdapterNotifications);
			mListContacts.setAdapter(mAdapterContacts);
    		mNewContactView.bind(getString(R.string.tab_contacts_new_contact), getString(R.string.tab_contacts_new_contact_details));
    		mClearPendingView.bind(getString(R.string.clear_pending), getString(R.string.clear_pending_details));
    		
    		updateConversations();
    		updateEvents();
		} else {
			mListNotifications.setAdapter(null);
			mListContacts.setAdapter(null);
	        mNewContactView.bind(getString(R.string.tab_contacts_not_available), getString(R.string.tab_contacts_not_available_details));
	        mClearPendingView.bind(getString(R.string.tab_contacts_not_available), getString(R.string.tab_contacts_not_available_details));
	    }
	}

	@Override
	public void onPkiLogin() {
		super.onPkiLogin();
		Storage.addListener(mConversationChangeListener);
		mListConversations.setAdapter(mAdapterConversations);
		mListContacts.setAdapter(mAdapterContacts);
		mListNotifications.setAdapter(mAdapterNotifications);
	}

	@Override
	public void onPkiLogout() {
		Storage.removeListener(mConversationChangeListener);
		mListConversations.setAdapter(null);
		mListContacts.setAdapter(null);
		mListNotifications.setAdapter(null);
		super.onPkiLogout();
	}
	
	private boolean mFirstEventParsing = true;

	@Override
	public void onEventParsingStarted() {
		super.onEventParsingStarted();
		if (mFirstEventParsing && (mAdapterNotifications.getList() == null || mAdapterNotifications.getList().size() == 0)) {
			mListNotificationsLoading.setVisibility(View.VISIBLE);
			mListNotifications.setVisibility(View.GONE);
		}
		mFirstEventParsing = false;
		Log.d(MyApplication.APP_TAG, "Parsing started (apparently)");
	}

	@Override
	public void onEventParsingFinished() {
		super.onEventParsingFinished();
		updateEvents();
		mListNotificationsLoading.setVisibility(View.GONE);
		mListNotifications.setVisibility(View.VISIBLE);
	}

	private void startConversation(Conversation conv) {
		Intent intent = new Intent(ActivityLists.this, ActivityConversation.class);
		intent.putExtra(ActivityConversation.OPTION_PHONE_NUMBER, conv.getPhoneNumber());
		startActivity(intent);
	}
	
	private void startKeyExchange(String phoneNumber) {
		Intent intent = new Intent(ActivityLists.this, ActivityExchangeMethod.class);
		intent.putExtra(ActivityExchangeMethod.OPTION_PHONE_NUMBER, phoneNumber);
		startActivity(intent);
	}
	
	private void removeParseResult(ParseResult parseResult) {
		DbPendingAdapter database = new DbPendingAdapter(ActivityLists.this);
		database.open();
		try {
			parseResult.removeFromDb(database);			
		} finally {
			database.close();
		}
		PendingParser.getSingleton().getParseResults().remove(mNotificationsContextMenuItem);
		updateEvents();
		PendingParser.forceParsing();
	}
	
	private StorageChangeListener mConversationChangeListener = new StorageChangeListener() {
		
		@Override
		public void onUpdate() {
			updateConversations();
		}
	};
	
	private void updateConversations() {
		synchronized(mAdapterConversations) {
			new ConversationsUpdateTask().execute();
		}
	}
	
	private class ConversationsUpdateTask extends AsyncTask<Void, Void, Void> {
		private Exception mException = null;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.d(MyApplication.APP_TAG, "Conversation update");
			if (mAdapterConversations.getList() == null || mAdapterConversations.getList().size() == 0) {
				mListConversationsLoading.setVisibility(View.VISIBLE);
				mListConversations.setVisibility(View.GONE);
			}
			if (mAdapterContacts.getList() == null || mAdapterContacts.getList().size() == 0) {
				mListContactsLoading.setVisibility(View.VISIBLE);
				mListContacts.setVisibility(View.GONE);
			}
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			// update lists
			ArrayList<Conversation> listConversations = new ArrayList<Conversation>();
			ArrayList<Conversation> listContacts = new ArrayList<Conversation>();

			try {
	    		Conversation conv = Header.getHeader().getFirstConversation();
	    		while (conv != null) {
	    			if (conv.hasMessageData())
	    				listConversations.add(conv);
					if (StorageUtils.hasKeysForSim(conv))
						listContacts.add(conv);
	    			conv = conv.getNextConversation();
	    		}
			} catch (StorageFileException ex) {
				if (ex.getCause() instanceof EncryptionException) {
					// don't really care
					// probably Pki not ready
				} else {
					mException = ex;
					return null;
				}
			} catch (WrongKeyDecryptionException ex) {
				mException = ex;
				return null;
			}
    		Collections.sort(listConversations, Collections.reverseOrder());
    		Collections.sort(listContacts, new Comparator<Conversation>() {
				@Override
				public int compare(Conversation conv1, Conversation conv2) {
					Contact contact1 = Contact.getContact(ActivityLists.this, conv1.getPhoneNumber());
					Contact contact2 = Contact.getContact(ActivityLists.this, conv2.getPhoneNumber());
					return contact1.compareTo(contact2);
				}
			});
    		
    		mAdapterConversations.setList(listConversations);
    		mAdapterContacts.setList(listContacts);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (mException == null) {
				mAdapterConversations.notifyDataSetChanged();
				mAdapterContacts.notifyDataSetChanged();
				mListConversationsLoading.setVisibility(View.GONE);
				mListConversations.setVisibility(View.VISIBLE);
				mListContactsLoading.setVisibility(View.GONE);
				mListContacts.setVisibility(View.VISIBLE);
			} else {
				State.fatalException(mException);
				return;
			}
		}
	}

	public void updateEvents() {
		Log.d(MyApplication.APP_TAG, "Updating list");
		mAdapterNotifications.setList(PendingParser.getSingleton().getParseResults());
		mAdapterNotifications.notifyDataSetChanged();
	}
}
