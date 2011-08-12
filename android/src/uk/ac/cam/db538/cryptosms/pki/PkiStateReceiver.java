package uk.ac.cam.db538.cryptosms.pki;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PkiStateReceiver extends BroadcastReceiver {
	
	private static final String INTENT_PKI_LOGIN = "uk.ac.cam.dje38.pki.login";
	private static final String INTENT_PKI_LOGOUT = "uk.ac.cam.dje38.pki.logout";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(INTENT_PKI_LOGIN))
			Pki.setLoggedIn(true);
		else if (intent.getAction().equals(INTENT_PKI_LOGOUT))
			Pki.setLoggedIn(false);
	}

}
