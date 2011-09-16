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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

/*
 * Overlay that is shown when application locks itself
 */
public class ErrorOverlay extends RelativeLayout {

	private TextView mTextView;
	private Button mTopButton;
	private Button mBottomButton;
	private View mMainView;
	
	private void prepare() {
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.overlay_error, this, true);
		mTextView = (TextView) this.findViewById(R.id.overlay_error_text);
		mTopButton = (Button) this.findViewById(R.id.overlay_error_button_top);
		mBottomButton = (Button) this.findViewById(R.id.overlay_error_button_bottom);
	}
	
	/**
	 * Instantiates a new error overlay.
	 *
	 * @param context the context
	 */
	public ErrorOverlay(Context context) {
		super(context);
		prepare();
	}
	
	/**
	 * Instantiates a new error overlay.
	 *
	 * @param context the context
	 * @param attrs the attrs
	 * @param defStyle the def style
	 */
	public ErrorOverlay(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		prepare();
	}

	/**
	 * Instantiates a new error overlay.
	 *
	 * @param context the context
	 * @param attrs the attrs
	 */
	public ErrorOverlay(Context context, AttributeSet attrs) {
		super(context, attrs);
		prepare();
	}

	public Button getTopButton() {
		return mTopButton;
	}

	public TextView getTextView() {
		return mTextView;
	}
	
	public void setMainView(View mainView) {
		mMainView = mainView;
	}
	
	/**
	 * Show the overlay.
	 */
	public void show() {
		this.setVisibility(VISIBLE);
		if (mMainView != null)
			mMainView.setVisibility(GONE);
	}
	
	/**
	 * Hide the overlay.
	 */
	public void hide() {
		this.setVisibility(GONE);
		if (mMainView != null)
			mMainView.setVisibility(VISIBLE);
	}

	/**
	 * Show message that the user needs to log in
	 */
	public void modeLogin() {
		mTextView.setText(R.string.logged_out);
		mTopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Pki.login(false);
			}
        });
		mTopButton.setText(R.string.log_in);
		
		mTopButton.setVisibility(VISIBLE);
		mBottomButton.setVisibility(GONE);
	}

	/**
	 * Show message that the PKI is missing
	 */
	public void modePkiMissing() {
		mTextView.setText(R.string.pki_install_message);
		
		mTopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + MyApplication.PKI_PACKAGE));
				try {
					getContext().startActivity(market);
				} catch(ActivityNotFoundException e2) {
				}
			}
        });
		mTopButton.setText(R.string.to_market);
		
		mBottomButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Pki.connect();
			}
        });
		mBottomButton.setText(R.string.try_again);

		mTopButton.setVisibility(VISIBLE);
		mBottomButton.setVisibility(VISIBLE);
	}

	/**
	 * Show message that the app is not connected to PKI
	 */
	public void modeDisconnected() {
		mTextView.setText(R.string.disconnected_message);
		mTopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Pki.connect();
			}
        });
		mTopButton.setText(R.string.reconnect);
		
		mTopButton.setVisibility(VISIBLE);
		mBottomButton.setVisibility(GONE);
	}

	/**
	 * Show message that there has been a fatal exception
	 *
	 * @param ex the exception
	 */
	public void modeFatalException(final Exception ex) {
		mTextView.setText(R.string.fatal_exception_message);

		mTopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// prepare report
				String report = "Exception: " + ex.getClass().getName();
				report += MyApplication.NEWLINE + "Message: " + ex.getMessage();
				report += MyApplication.NEWLINE + "Stack: ";
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ex.printStackTrace(new PrintWriter(baos));
				report += MyApplication.NEWLINE + baos.toString();
				// send report via email
				Intent emailIntent = new Intent(Intent.ACTION_SEND);
				emailIntent.setType("text/html");
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, MyApplication.REPORT_EMAILS);
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, report);			
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getContext().getResources().getString(R.string.report_subject));
				getContext().startActivity(Intent.createChooser(emailIntent, getContext().getResources().getString(R.string.send_email_report)));
			}
        });
		mTopButton.setText(R.string.report);

//		mBottomButton.setOnClickListener(new OnClickListener(){
//			@Override
//			public void onClick(View v) {
//				// kill the app
////				System.runFinalizersOnExit(true);
////	            System.exit(0);			
//				android.os.Process.killProcess(android.os.Process.myPid());
//			}
//        });
//		mBottomButton.setText(R.string.quit);
		
		mTopButton.setVisibility(VISIBLE);
		mBottomButton.setVisibility(GONE);
	}

	/**
	 * Show message that the file is corrupted
	 */
	public void modeCorruptedFile() {
		mTextView.setText(R.string.corrupted_file_message);

		mTopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				try {
					Storage.getStorage().closeFile();
					Storage.getStorage().deleteFile();
				} catch (StorageFileException e) {
					android.os.Process.killProcess(android.os.Process.myPid());
				}
			}
        });
		mTopButton.setText(R.string.delete_file);

		mTopButton.setVisibility(VISIBLE);
		mBottomButton.setVisibility(GONE);
	}
}
