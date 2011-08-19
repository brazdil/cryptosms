package uk.ac.cam.db538.cryptosms.ui;

import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.KeysMessage;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

public class UtilsSendMessage {
	public static final String DIALOG_SENDING = "DIALOG_SENDING";
	public static final String DIALOG_SENDING_MULTIPART = "DIALOG_SENDING_MULTIPART";
	public static final String PARAM_SENDING_MULTIPART_MAX = "PARAM_SENDING_MULTIPART_MAX";
	public static final String DIALOG_SENDING_ERROR = "DIALOG_EXCHANGE_SENDING_ERROR";
	public static final String PARAM_SENDING_ERROR = "PARAM_EXCHANGE_SENDING_ERROR";
	
	public static void prepareDialogs(DialogManager dialogManager, final Context context) {
		dialogManager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = context.getResources();
				ProgressDialog pd = new ProgressDialog(context);
				pd.setCancelable(false);
				pd.setMessage(res.getString(R.string.sending));
				return pd;
			}
			
			@Override
			public String getId() {
				return DIALOG_SENDING;
			}
		});
		dialogManager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				Resources res = context.getResources();
				ProgressDialog pd = new ProgressDialog(context);
				pd.setCancelable(false);
				pd.setMessage(res.getString(R.string.sending));
				pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				pd.setMax(params.getInt(PARAM_SENDING_MULTIPART_MAX));
				return pd;
			}
			
			@Override
			public String getId() {
				return DIALOG_SENDING_MULTIPART;
			}
		});
		dialogManager.addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				String error = params.getString(PARAM_SENDING_ERROR);
				Resources res = context.getResources();
				
				return new AlertDialog.Builder(context)
				       .setTitle(res.getString(R.string.error_sms_service))
				       .setMessage(res.getString(R.string.error_sms_service_details) + "\nError: " + error)
				       .setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
				       .create();
			}
			
			@Override
			public String getId() {
				return DIALOG_SENDING_ERROR;
			}
		});
	}
}
