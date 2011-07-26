package uk.ac.cam.db538.securesms.ui;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.utils.Common;
import uk.ac.cam.db538.securesms.utils.Common.OnSimStateListener;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class MainTabActivity extends TabActivity {
	
	private static final int TAB_INDEX_RECENT = 0;
	private static final int TAB_INDEX_CONTACTS = 1;
	
	private TabHost.TabSpec specRecent;
	private TabHost.TabSpec specContacts;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.screen_main);

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
	    
        // register for changes in SIM state
        Common.registerSimStateListener(this, new OnSimStateListener() {
			@Override
			public void onChange() {
				checkResources();
			}
		});
	}
	
	private void checkResources() {
		TabHost tabHost = getTabHost(); 

		// check SIM availability
    	if (Common.checkSimNumberAvailable(this)) {
    	    tabHost.getTabWidget().getChildTabViewAt(TAB_INDEX_CONTACTS).setEnabled(true);
    	} else {
    	    tabHost.setCurrentTab(TAB_INDEX_RECENT);
    	    tabHost.getTabWidget().getChildTabViewAt(TAB_INDEX_CONTACTS).setEnabled(false);
    	}
	}
	
    public void onResume() {
    	super.onResume();
    	checkResources();
    }
}
