package uk.ac.cam.db538.cryptosms.ui;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.state.State.StateChangeListener;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.TabHost;

public class MainTabActivity extends TabActivity {
	private static final int MENU_MOVE_SESSIONS = Menu.FIRST;

	private View mMainLayout;
	private ErrorOverlay mErrorOverlay;
	
	private DialogManager mDialogManager = new DialogManager();
	
	private StateChangeListener mStateListener = new StateChangeListener(){
		@Override
		public void onConnect() {
		}

		@Override
		public void onDisconnect() {
			Log.d(MyApplication.APP_TAG, "Disconnect error overlay");
			mErrorOverlay.modeDisconnected();
			mErrorOverlay.show();
		}

		@Override
		public void onFatalException(Exception ex) {
			Log.d(MyApplication.APP_TAG, "Fatal exception error overlay");
			mErrorOverlay.modeFatalException(ex);
			mErrorOverlay.show();
		}

		@Override
		public void onLogin() {
			mErrorOverlay.hide();
	        mDialogManager.restoreState();
		}

		@Override
		public void onLogout() {
			Log.d(MyApplication.APP_TAG, "Logout error overlay");
			mErrorOverlay.modeLogin();
			mErrorOverlay.show();
	        mDialogManager.saveState();
		}

		@Override
		public void onPkiMissing() {
			Log.d(MyApplication.APP_TAG, "PkiMissing error overlay");
			mErrorOverlay.modePkiMissing();
			mErrorOverlay.show();
		}

		@Override
		public void onSimState() {
		}
	};
	
	private TabHost.TabSpec specContacts;
	
	private TabHost.TabSpec specRecent;

	@Override
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

	    // error overlay
	    mMainLayout = findViewById(R.id.screen_main);
	    mErrorOverlay = (ErrorOverlay) findViewById(R.id.screen_main_error);
	    mErrorOverlay.setMainView(mMainLayout);
        mStateListener.onLogout();
        
        // prepare dialogs
        Utils.prepareDialogs(mDialogManager, this);
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
				Utils.moveSessionKeys(mDialogManager);                                 
				return true;
			}
		});
		return true;
	}

	@Override
	protected void onStart() {
        // listen for logins/logouts
        State.addListener(mStateListener);
		super.onStart();
	}

	@Override
	protected void onStop() {
		Log.d(MyApplication.APP_TAG, "Removing listener");
		State.removeListener(mStateListener);
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Pki.login(false);
	}
}
