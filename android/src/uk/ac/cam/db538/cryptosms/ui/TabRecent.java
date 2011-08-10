package uk.ac.cam.db538.cryptosms.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.MyApplication.OnPkiAvailableListener;
import uk.ac.cam.db538.cryptosms.data.Utils;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.Header;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.Conversation.ConversationUpdateListener;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class TabRecent extends ListActivity {
		
	private ArrayList<Conversation> mRecent = new ArrayList<Conversation>();;
	private ArrayAdapter<Conversation> mAdapterRecent;
	private Context mContext = this;
	
	private void updateContacts() throws StorageFileException, IOException {
		mRecent.clear();

		Conversation conv = Header.getHeader().getFirstConversation();
		while (conv != null) {
			if (conv.getFirstMessageData() != null)
				mRecent.add(conv);
			conv = conv.getNextConversation();
		}
		
		Collections.sort(mRecent, Collections.reverseOrder());
	}
	
	private void startConversation(Conversation conv) {
		Intent intent = new Intent(TabRecent.this, ConversationActivity.class);
		intent.putExtra(ConversationActivity.OPTION_PHONE_NUMBER, conv.getPhoneNumber());
		intent.putExtra(ConversationActivity.OPTION_OFFER_KEYS_SETUP, false);
		startActivity(intent);
	}	
	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_main_listtab);

        final Context context = this;
        final ListView listView = getListView();
        final LayoutInflater inflater = LayoutInflater.from(this);
        
        // set appearance of list view
		listView.setFastScrollEnabled(true);
		
    	// create the adapter
    	final ArrayAdapter<Conversation> adapterContacts = new ArrayAdapter<Conversation>(this, R.layout.item_main_contacts, mRecent) {
    		@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TabRecentItem row;

				if (convertView == null)
					row = (TabRecentItem) inflater.inflate(R.layout.item_main_recent, listView, false);
				else
					row = (TabRecentItem) convertView;
			    
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
    	mAdapterRecent = adapterContacts;
		// specify what to do when clicked on items
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view,	int arg2, long arg3) {
				TabRecentItem item = (TabRecentItem) view;
				Conversation conv;
	    		if ((conv = item.getConversationHeader()) != null) {
		    		// clicked on a conversation
    				startConversation(conv);
	    		}
			}
		});
    }

	@Override
	protected void onResume() {
		super.onResume();
		setListAdapter(null);
		MyApplication.getSingleton().waitForPki(this, mPkiAvailableListener);
	}
	
	private OnPkiAvailableListener mPkiAvailableListener = new OnPkiAvailableListener() {
		@Override
		public void OnPkiAvailable() {
	        try {
	        	// initialize the list of conversations
	        	updateContacts();
			} catch (StorageFileException ex) {
				Utils.dialogDatabaseError(mContext, ex);
			} catch (IOException ex) {
				Utils.dialogIOError(mContext, ex);
			}
			setListAdapter(mAdapterRecent);
		}
	};
}
