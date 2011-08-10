package uk.ac.cam.db538.securesms.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import uk.ac.cam.db538.securesms.MyApplication;
import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.data.DummyOnClickListener;

public class PkiInstallActivity extends Activity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_pki_install);
		setTitle(R.string.pki_install_title);
		
		final PkiInstallActivity context = this;
		final Resources res = context.getResources();
		
		Button buttonMarket = (Button)findViewById(R.id.pki_install_button_market);
		buttonMarket.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=uk.ac.cam.PKI"));
				try {
					context.startActivity(market);
					context.simulateBackPressed();
				} catch(ActivityNotFoundException e2) {
					// doesn't have Market
					new AlertDialog.Builder(context)
						.setTitle(res.getString(R.string.error_market_unavailable))
						.setMessage(res.getString(R.string.error_market_unavailable_details))
						.setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
						.show();
				}
			}
		});
		Button buttonTryAgain = (Button)findViewById(R.id.pki_install_button_try_again);
		buttonTryAgain.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (MyApplication.getSingleton().checkPki()) {
					context.simulateBackPressed();
				}
			}
		});
	}

	public void simulateBackPressed() {
		super.onBackPressed();
	}
	
	@Override
	public void onBackPressed() {
	}
}
