package uk.ac.cam.db538.securesms.ui;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.database.Conversation;
import uk.ac.cam.db538.securesms.database.DatabaseFileException;
import uk.ac.cam.db538.securesms.database.Header;
import uk.ac.cam.db538.securesms.database.Conversation.ConversationUpdateListener;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TabContacts extends ListActivity {
	
	private ArrayList<Conversation> mContacts = new ArrayList<Conversation>();;
	
	private void updateContacts(Context context) throws DatabaseFileException, IOException {
		String simNumber = Utils.getSimNumber(context);
		mContacts.clear();
		
		Conversation conv = Header.getHeader().getFirstConversation();
		while (conv != null) {
			if (conv.getSessionKeys(simNumber) != null)
				mContacts.add(conv);
			conv = conv.getNextConversation();
		}
	}
	
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_main_recent);
        
        final ListView listView = getListView();
        final LayoutInflater inflater = LayoutInflater.from(this);
        
        // the New contact header
        TabRecentItem headerView = (TabRecentItem) inflater.inflate(R.layout.item_main_recent, listView, false);
        headerView.bind(getString(R.string.new_contact), getString(R.string.create_new_contact));
        listView.addHeaderView(headerView, null, true);
        
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
				    
					row.bind(this.getContext(), getItem(position));
				    
					return row;
				}
			};
        	// add listeners			
        	Conversation.addUpdateListener(new ConversationUpdateListener() {
				@Override
				public void onUpdate() {
					try {
						updateContacts(getApplicationContext());
						adapterContacts.notifyDataSetChanged();
					} catch (DatabaseFileException ex) {
						// TODO: Something went horribly wrong!
					} catch (IOException ex) {
						// TODO: Something went EVEN MORE horribly wrong!
					}
				}
			});
			setListAdapter(adapterContacts);
		} catch (DatabaseFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

        /*
        listView.setOnCreateContextMenuListener(mConvListOnCreateContextMenuListener);
        listView.setOnKeyListener(mThreadListKeyListener);

        initListAdapter();

        mTitle = getString(R.string.app_label);

        mHandler = new Handler();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean checkedMessageLimits = mPrefs.getBoolean(CHECKED_MESSAGE_LIMITS, false);
        if (DEBUG) Log.v(TAG, "checkedMessageLimits: " + checkedMessageLimits);
        if (!checkedMessageLimits || DEBUG) {
            runOneTimeStorageLimitCheckForLegacyMessages();
        }*/
    }

    /*private void initListAdapter() {
        mListAdapter = new ConversationListAdapter(this, null);
        mListAdapter.setOnContentChangedListener(mContentChangedListener);
        setListAdapter(mListAdapter);
        getListView().setRecyclerListener(mListAdapter);
    }*/
}
