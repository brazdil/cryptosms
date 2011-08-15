package uk.ac.cam.db538.cryptosms.ui;

import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.state.State.StateChangeListener;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

public class StateAwareActivity extends Activity implements StateAwareActivityInterface {
	private ErrorOverlay mErrorOverlay;
	private View mMainView;
	private DialogManager mDialogManager = new DialogManager();
	
	// CONTENT STUFF
	
	@Override
	public void setContentView(View view, LayoutParams params) {
		mMainView = view;
		mErrorOverlay = new ErrorOverlay(this);
		mErrorOverlay.setMainView(mMainView);
		
		RelativeLayout layoutRoot = new RelativeLayout(this);
		layoutRoot.addView(mMainView, params);
		layoutRoot.addView(mErrorOverlay, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		super.setContentView(layoutRoot);

        onPkiLogout();
	}

	@Override
	public void setContentView(View view) {
		setContentView(view, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	@Override
	public void setContentView(int layoutResID) {
		setContentView(LayoutInflater.from(this).inflate(layoutResID, null));
	}

	@Override
	public View findViewById(int id) {
		return mMainView.findViewById(id);
	}

	@Override
	public ErrorOverlay getErrorOverlay() {
		return mErrorOverlay;
	}

	@Override
	public View getMainView() {
		return mMainView;
	}
	
	public DialogManager getDialogManager() {
		return mDialogManager;
	}
	
	// STATE LISTENER

	private StateChangeListener mStateListener = new StateChangeListener() {
		@Override
		public void onSimState() {
			StateAwareActivity.this.onSimState();
		}
		
		@Override
		public void onPkiMissing() {
			StateAwareActivity.this.onPkiMissing();
		}
		
		@Override
		public void onLogout() {
			StateAwareActivity.this.onPkiLogout();
		}
		
		@Override
		public void onLogin() {
			StateAwareActivity.this.onPkiLogin();
		}
		
		@Override
		public void onFatalException(Exception ex) {
			StateAwareActivity.this.onFatalException(ex);
		}
		
		@Override
		public void onDisconnect() {
			StateAwareActivity.this.onPkiDisconnect();
		}
		
		@Override
		public void onConnect() {
			StateAwareActivity.this.onPkiConnect();
		}
	};
	
	public void onSimState() {
		
	}
	
	public void onPkiConnect() {

	}
	
	public void onPkiDisconnect() {
		getErrorOverlay().modeDisconnected();
		getErrorOverlay().show();
	}
	
	public void onPkiLogin() {
		getErrorOverlay().hide();
        mDialogManager.restoreState();
	}
	
	public void onPkiLogout() {
		getErrorOverlay().modeLogin();
		getErrorOverlay().show();
        mDialogManager.saveState();
	}
	
	public void onPkiMissing() {
		getErrorOverlay().modePkiMissing();
		getErrorOverlay().show();
	}
	
	public void onFatalException(Exception ex) {
		getErrorOverlay().modeFatalException(ex);
		getErrorOverlay().show();
	}

	// OVERRIDES
	
	@Override
	protected void onStart() {
		super.onStart();
		State.addListener(mStateListener);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		State.removeListener(mStateListener);
	}

}
