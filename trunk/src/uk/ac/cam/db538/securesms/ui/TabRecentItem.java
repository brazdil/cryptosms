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

package uk.ac.cam.db538.securesms.ui;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.data.Contact;
import uk.ac.cam.db538.securesms.storage.Conversation;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import android.util.AttributeSet;
import android.view.View;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This class manages the view for given conversation.
 */
public class TabRecentItem extends RelativeLayout {
    private TextView mSubjectView;
    private TextView mFromView;
    private TextView mDateView;
    private View mDeliveryPendingView;
    private View mErrorIndicator;
    private QuickContactBadge mAvatarView;

    static private Drawable sDefaultContactImage;
    
    private Conversation mConversationHeader;

    public TabRecentItem(Context context) {
        super(context);
    }

    public TabRecentItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (sDefaultContactImage == null) {
            sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_contact_picture);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFromView = (TextView) findViewById(R.id.recent_from);
        mSubjectView = (TextView) findViewById(R.id.recent_subject);
        mDateView = (TextView) findViewById(R.id.recent_date);
        mDeliveryPendingView = findViewById(R.id.recent_attachment);
        mErrorIndicator = findViewById(R.id.recent_error);
        mAvatarView = (QuickContactBadge) findViewById(R.id.recent_avatar);
    }

    private void setConversationHeader(Conversation conv) {
    	mConversationHeader = conv;
    }

	public Conversation getConversationHeader() {
    	return mConversationHeader;
    }

/*    private CharSequence formatMessage(ConversationListItemData ch) {
        final int size = android.R.style.TextAppearance_Small;
        final int color = android.R.styleable.Theme_textColorSecondary;
        String from = ch.getFrom();

        SpannableStringBuilder buf = new SpannableStringBuilder(from);

        if (ch.getMessageCount() > 1) {
            buf.append(" (" + ch.getMessageCount() + ") ");
        }

        int before = buf.length();
        if (ch.hasDraft()) {
            buf.append(" ");
            buf.append(mContext.getResources().getString(R.string.has_draft));
            buf.setSpan(new TextAppearanceSpan(mContext, size, color), before,
                    buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            buf.setSpan(new ForegroundColorSpan(
                    mContext.getResources().getColor(R.drawable.text_color_red)),
                    before, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        // Unread messages are shown in bold
        if (!ch.isRead()) {
            buf.setSpan(STYLE_BOLD, 0, buf.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return buf;
    }*/


    public final void bind(final Conversation conv) {
    	Context context = this.getContext();
    	Resources res = context.getResources();
        setConversationHeader(conv);
        
        boolean hasError = false; 
        boolean hasNoEncryption = false;

        setBackgroundDrawable(
        	(conv.getMarkedUnread()) ?
        		res.getDrawable(R.drawable.conversation_item_background_unread) :
        		res.getDrawable(R.drawable.conversation_item_background_read)
        );

        LayoutParams attachmentLayout = (LayoutParams)mDeliveryPendingView.getLayoutParams();
        if (hasError) {
            attachmentLayout.addRule(RelativeLayout.LEFT_OF, R.id.recent_error);
        } else {
            attachmentLayout.addRule(RelativeLayout.LEFT_OF, R.id.recent_date);
        }

        LayoutParams subjectLayout = (LayoutParams)mSubjectView.getLayoutParams();
        subjectLayout.addRule(RelativeLayout.LEFT_OF, hasNoEncryption ? R.id.recent_attachment :
            (hasError ? R.id.recent_error : R.id.recent_date));

        mErrorIndicator.setVisibility(hasError ? VISIBLE : GONE);

        mDeliveryPendingView.setVisibility(hasNoEncryption ? VISIBLE : GONE);
        mDateView.setText(conv.getFormattedTime());

    	Contact contact = Contact.getContact(context, conv.getPhoneNumber());
        mFromView.setText(contact.getName());
        mSubjectView.setText(conv.getPreview());

    	Drawable avatarDrawable = contact.getAvatar(context, sDefaultContactImage);
        if (contact.existsInDatabase()) {
            mAvatarView.assignContactUri(contact.getUri());
        } else {
            mAvatarView.assignContactFromPhone(conv.getPhoneNumber(), true);
        }
        mAvatarView.setImageDrawable(avatarDrawable);
        mAvatarView.setVisibility(View.VISIBLE);
    }
}
