package uk.ac.cam.db538.cryptosms.pki;

import uk.ac.cam.db538.cryptosms.MyApplication;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PkiStateReceiver extends BroadcastReceiver {
	
	private static final String INTENT_PKI_LOGIN = "uk.ac.cam.dje38.pki.login";
	private static final String INTENT_PKI_LOGOUT = "uk.ac.cam.dje38.pki.logout";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(INTENT_PKI_LOGIN))
			Pki.setLoggedIn(true);
		else if (intent.getAction().equals(INTENT_PKI_LOGOUT))
			Pki.setLoggedIn(false);
		else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
			if (intent.getDataString().equals("package:" + MyApplication.PKI_PACKAGE))
				Pki.setPkiMissing();
		} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
			if (intent.getDataString().equals("package:" + MyApplication.PKI_PACKAGE))
				Pki.connect();
		}
	}

}
