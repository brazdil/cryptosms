package uk.ac.cam.db538.cryptosms.ui;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.pki.Pki;
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

public class ErrorOverlay extends RelativeLayout {

	private TextView mTextView;
	private Button mTopButton;
	private Button mBottomButton;
	
	private void prepare() {
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.overlay_error, this, true);
		mTextView = (TextView) this.findViewById(R.id.overlay_error_text);
		mTopButton = (Button) this.findViewById(R.id.overlay_error_button_top);
		mBottomButton = (Button) this.findViewById(R.id.overlay_error_button_bottom);
	}
	
	public ErrorOverlay(Context context) {
		super(context);
		prepare();
	}
	
	public ErrorOverlay(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		prepare();
	}

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
	
	public void modeLogin() {
		mTextView.setText(R.string.logged_out);
		mTopButton.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Pki.login(false);
			}
        });
		mTopButton.setText(R.string.log_in);
		
		mTopButton.setVisibility(VISIBLE);
		mBottomButton.setVisibility(GONE);
	}

	public void modePkiMissing() {
		mTextView.setText(R.string.pki_install_message);
		
		mTopButton.setOnClickListener(new OnClickListener(){
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
			public void onClick(View v) {
				Pki.connect();
			}
        });
		mBottomButton.setText(R.string.try_again);

		mTopButton.setVisibility(VISIBLE);
		mBottomButton.setVisibility(VISIBLE);
	}
}
