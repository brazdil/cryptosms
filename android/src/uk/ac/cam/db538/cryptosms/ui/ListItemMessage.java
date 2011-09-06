/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.db538.cryptosms.ui;

import java.util.zip.DataFormatException;

import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.TextMessage;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This class manages the view for given conversation.
 */
public class ListItemMessage extends RelativeLayout {
	private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);
	
	private TextView mFrom;
	private TextView mMessageBody;
	private TextView mTimeStamp;
    private View mErrorIndicator;

    private TextMessage mMessage;

    public ListItemMessage(Context context) {
        super(context);
    }

    public ListItemMessage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFrom = (TextView) findViewById(R.id.from);
        mMessageBody = (TextView) findViewById(R.id.message_body);
        mTimeStamp = (TextView) findViewById(R.id.timestamp);
        mErrorIndicator = findViewById(R.id.error);
    }

    private void setMessage(TextMessage message) {
    	mMessage = message;
    }

	public TextMessage getMessage() {
    	return mMessage;
    }

    private CharSequence formatSender() throws StorageFileException, DataFormatException {
    	Context context = this.getContext();
//        final int size = android.R.style.TextAppearance_Small;
//        final int color = 8; // android.R.styleable.Theme_textColorSecondary;

        String from = new String();
    	Contact contact = Contact.getContact(context, mMessage.getStorage().getParent().getPhoneNumber());
        from = contact.getPhoneNumber();
    	if (contact.getName() != null && contact.getName().length() > 0)
    		from = contact.getName();
        
        SpannableStringBuilder buf = new SpannableStringBuilder(from);

//        int before = buf.length();
//        if (ch.hasDraft()) {
//            buf.append(" ");
//            buf.append(context.getResources().getString(R.string.has_draft));
//            buf.setSpan(new TextAppearanceSpan(context, size, color), before,
//                    buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
//            buf.setSpan(new ForegroundColorSpan(
//            		context.getResources().getColor(R.drawable.text_color_red)),
//                    before, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
//        }

        // Unread messages are shown in bold
        if (mMessage.getStorage().getUnread()) {
            buf.setSpan(STYLE_BOLD, 0, buf.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        
        buf.append(mMessage.getText().getMessage());
        return buf;
    }
	
    public final void bind(final TextMessage message) {
    	Context context = this.getContext();
    	Resources res = context.getResources();
        setMessage(message);
        
        boolean hasError = false; 

        setBackgroundDrawable(
    		res.getDrawable(R.drawable.conversation_item_background_read)
        );

        try {
        	mMessageBody.setText(formatSender());
        } catch (DataFormatException ex) {
        	hasError = true;
        } catch (StorageFileException e) {
        	hasError = true;
        }

    	mTimeStamp.setText(UtilsTextFormat.formatDateTime(mMessage.getStorage().getTimeStamp()));

        mErrorIndicator.setVisibility(hasError ? VISIBLE : GONE);
    }
}
