package uk.ac.cam.db538.securesms.ui;

import java.io.IOException;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.database.Database;
import uk.ac.cam.db538.securesms.database.DatabaseFileException;
import android.app.ListActivity;
import android.os.Bundle;

public class TabRecent extends ListActivity {
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_main_recent);
        
        try {
			Database.initSingleton(this);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DatabaseFileException e) {
			e.printStackTrace();
		}
        
		/*        ListView listView = getListView();
        LayoutInflater inflater = LayoutInflater.from(this);
        
        // the New Message header
        ConversationListItem headerView = (ConversationListItem) inflater.inflate(R.layout.item_main_recent, listView, false);
        headerView.bind(getString(R.string.new_message), getString(R.string.create_new_message));
        listView.addHeaderView(headerView, null, true);*/
        
        //setListAdapter(new ConversationListAdapter(this));

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
