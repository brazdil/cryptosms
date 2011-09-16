/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package uk.ac.cam.db538.cryptosms.ui;

import uk.ac.cam.db538.cryptosms.R;
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
	
	/**
	 * Prepare dialogs.
	 *
	 * @param dialogManager the dialog manager
	 * @param context the context
	 */
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
