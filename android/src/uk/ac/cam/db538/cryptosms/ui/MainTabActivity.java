package uk.ac.cam.db538.cryptosms.ui;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.MyApplication.OnPkiAvailableListener;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.data.DbPendingAdapter;
import uk.ac.cam.db538.cryptosms.data.Pending;
import uk.ac.cam.db538.cryptosms.data.TextMessage;
import uk.ac.cam.db538.cryptosms.data.Utils;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageType;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.TabHost;

public class MainTabActivity extends TabActivity {
	private TabHost.TabSpec specRecent;
	private TabHost.TabSpec specContacts;
	
	private static final int MENU_MOVE_SESSIONS = Menu.FIRST;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.screen_main);
	    
	    final Context context = this;
	    Resources res = getResources(); 	// Resource object to get Drawables
	    TabHost tabHost = getTabHost();  	// The activity TabHost
	    Intent intent;  					// Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, TabRecent.class);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    specRecent = tabHost.newTabSpec("recent").setIndicator(res.getString(R.string.tab_recent),
	                      res.getDrawable(R.drawable.tab_recent))
	                  .setContent(intent);
	    tabHost.addTab(specRecent);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, TabContacts.class);
	    specContacts = tabHost.newTabSpec("contacts").setIndicator(res.getString(R.string.tab_contacts),
	                      res.getDrawable(R.drawable.tab_contacts))
	                  .setContent(intent);
	    tabHost.addTab(specContacts);
	    
	    MyApplication.getSingleton().waitForPki(this, new OnPkiAvailableListener() {
			@Override
			public void OnPkiAvailable() {
				// TODO: Check for incoming messages
				DbPendingAdapter database = new DbPendingAdapter(context);
				database.open();
				ArrayList<Pending> listPending = database.getAllEntries();
				
				boolean found = false;
				do {
					// let's look for messages of type MESSAGE_FIRST
					Pending pendingFirst = null;
					for (Pending p : listPending)
						if (p.getMessageType() == MessageType.MESSAGE_FIRST) {
							pendingFirst = p;
							break;
						}
					// have we found one?
					if (pendingFirst != null) {
						found = true;
						listPending.remove(pendingFirst);
						
						// do we have Session Keys for this person?
						SessionKeys keys = null;
						try {
							keys = StorageUtils.getSessionKeysForSIM(Conversation.getConversation(pendingFirst.getSender()), context);
						} catch (StorageFileException e1) {
						} catch (IOException e1) {
						}
						
						if (keys != null) {
							int ID = TextMessage.getMessageID(pendingFirst.getData());
							int totalBytes = TextMessage.getMessageDataLength(pendingFirst.getData());
							int partCount = -1;
							try {
								partCount = TextMessage.computeNumberOfMessageParts(totalBytes);
							} catch (MessageException e) {
								// too long or data corrupted
							}
							if (partCount > 0) {
								// look for other parts
								ArrayList<Pending> listParts = new ArrayList<Pending>();
								listParts.add(pendingFirst);
								for (Pending p : listPending)
									if (p.getMessageType() == MessageType.MESSAGE_PART &&
										TextMessage.getMessageID(p.getData()) == ID)
										listParts.add(p);
								// have we found them all?
								if (listParts.size() == partCount) {
									int totalDataLength = TextMessage.LENGTH_FIRST_MESSAGEBODY + (partCount - 1) * TextMessage.LENGTH_PART_MESSAGEBODY;
									
									// get all the data in one byte array
									byte[] dataEncrypted = new byte[TextMessage.LENGTH_FIRST_ENCRYPTION + totalDataLength];
									for (Pending p : listParts) {
										int index = TextMessage.getMessageIndex(p.getData());
										System.arraycopy(TextMessage.getMessageData(p.getData()), 0, 
										                 dataEncrypted, TextMessage.LENGTH_FIRST_ENCRYPTION + TextMessage.getExpectedDataOffset(totalDataLength, index), 
										                 TextMessage.getExpectedDataLength(totalDataLength, index));
										if (index == 0)
											System.arraycopy(TextMessage.getMessageEncryptionData(p.getData()), 0, 
									                 dataEncrypted, 0, 
									                 TextMessage.LENGTH_FIRST_ENCRYPTION);
									}
									dataEncrypted = LowLevel.cutData(dataEncrypted, 0, dataEncrypted.length - (dataEncrypted.length % Encryption.AES_BLOCK_LENGTH));
									
									// decrypt it
									byte[] dataDecrypted = null;
									try {
										dataDecrypted = Encryption.getEncryption().decryptSymmetric(dataEncrypted, keys.getSessionKey_In());
									} catch (EncryptionException e) {
										// TODO: PROBABLY BAD KEY, but potentially problem with PKI 
									}
									
									if (dataDecrypted != null) {
										// take only the relevant part
										byte[] dataPlain = LowLevel.cutData(dataDecrypted, 0, totalBytes);
									} else {
										// TODO: couldn't decrypt
									}
								}
							} else {
								// TODO: notify about error!!! (probably too long)
							}
						} else {
							// TODO: notify about error!!! (no keys found)
						}
					}
							
				} while (found);
			}
		});
	}
	
	public void onStart() {
		super.onStart();
	    // just to show the possible error ASAP
		try {
			Utils.checkSimPhoneNumberAvailable(this);
		} catch (StorageFileException ex) {
			Utils.dialogDatabaseError(this, ex);
			this.finish();
		} catch (IOException ex) {
			Utils.dialogIOError(this, ex);
			this.finish();
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		final Context context = this;
		Resources res = this.getResources();
		
		int idGroup = 0;
		MenuItem menuMoveSessions = menu.add(idGroup, MENU_MOVE_SESSIONS, Menu.NONE, res.getString(R.string.menu_move_sessions));
		menuMoveSessions.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Utils.moveSessionKeys(context);
				return true;
			}
		});
		return true;
	}
}
