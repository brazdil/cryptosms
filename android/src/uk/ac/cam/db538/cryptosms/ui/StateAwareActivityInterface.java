package uk.ac.cam.db538.cryptosms.ui;

import android.view.View;

public interface StateAwareActivityInterface {
	public ErrorOverlay getErrorOverlay();
	public View getMainView();

	public void onSimState();
	public void onPkiConnect();
	public void onPkiDisconnect();
	public void onPkiLogin();
	public void onPkiLogout();
	public void onPkiMissing();
	public void onFatalException(Exception ex);
}
