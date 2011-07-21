package uk.ac.cam.db538.securesms.ui;

import java.util.ArrayList;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.database.Conversation;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TabContacts extends ListActivity {
	
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_main_recent);
        
        final ListView listView = getListView();
        final LayoutInflater inflater = LayoutInflater.from(this);
        
        // the New contact header
        ConversationListItem headerView = (ConversationListItem) inflater.inflate(R.layout.item_main_recent, listView, false);
        headerView.bind(getString(R.string.new_contact), getString(R.string.create_new_contact));
        listView.addHeaderView(headerView, null, true);
        
//        try {
			setListAdapter(new ArrayAdapter<Conversation>(this, R.layout.item_main_recent, /*INSERT ARRAYLIST*/ new ArrayList<Conversation>()) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					ConversationListItem row;
				   
					if (convertView == null)
						row = (ConversationListItem) inflater.inflate(R.layout.item_main_recent, listView, false);
					else
						row = (ConversationListItem) convertView;
				    
					row.bind(this.getContext(), getItem(position));
				    
					return row;
				}
			});
//		} catch (DatabaseFileException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

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
