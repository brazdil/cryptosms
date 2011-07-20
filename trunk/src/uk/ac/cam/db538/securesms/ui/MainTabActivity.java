package uk.ac.cam.db538.securesms.ui;

import uk.ac.cam.db538.securesms.R;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class MainTabActivity extends TabActivity {
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.screen_main);

	    Resources res = getResources(); 	// Resource object to get Drawables
	    TabHost tabHost = getTabHost();  	// The activity TabHost
	    TabHost.TabSpec spec;  				// Resusable TabSpec for each tab
	    Intent intent;  					// Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, TabRecent.class);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("recent").setIndicator(res.getString(R.string.recent),
	                      res.getDrawable(R.drawable.ic_tab_recent))
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, TabContacts.class);
	    spec = tabHost.newTabSpec("contacts").setIndicator(res.getString(R.string.contacts),
	                      res.getDrawable(R.drawable.ic_tab_contacts))
	                  .setContent(intent);
	    tabHost.addTab(spec);

/*	    intent = new Intent().setClass(this, SongsActivity.class);
	    spec = tabHost.newTabSpec("songs").setIndicator("Songs",
	                      res.getDrawable(R.drawable.ic_tab_songs))
	                  .setContent(intent);
	    tabHost.addTab(spec);*/

	    tabHost.setCurrentTab(1);
	}	
}
