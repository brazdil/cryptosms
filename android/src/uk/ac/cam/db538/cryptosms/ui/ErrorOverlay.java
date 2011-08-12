package uk.ac.cam.db538.cryptosms.ui;

import uk.ac.cam.db538.cryptosms.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ErrorOverlay extends RelativeLayout {

	private TextView mTextView;
	private Button mButton;
	
	private void prepare() {
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.overlay_error, this, true);
		mTextView = (TextView) this.findViewById(R.id.overlay_error_text);
		mButton = (Button) this.findViewById(R.id.overlay_error_button);
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

	public Button getButton() {
		return mButton;
	}

	public TextView getTextView() {
		return mTextView;
	}
}
