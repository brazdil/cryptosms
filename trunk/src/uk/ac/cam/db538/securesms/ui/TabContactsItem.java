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

import java.io.IOException;

import uk.ac.cam.db538.securesms.R;
import uk.ac.cam.db538.securesms.database.Conversation;
import uk.ac.cam.db538.securesms.database.DatabaseFileException;
import uk.ac.cam.db538.securesms.database.SessionKeys;
import uk.ac.cam.db538.securesms.database.SessionKeys.SessionKeysStatus;

import android.content.Context;
import android.graphics.drawable.Drawable;

import android.util.AttributeSet;
import android.view.View;
import android.widget.QuickContactBadge;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This class manages the view for given conversation.
 */
public class TabContactsItem extends RelativeLayout {
    private TextView mMessageView;
    private TextView mFromView;
    private TextView mDateView;
    private View mBeingSentView;
    private View mWaitingForReplyView;
    private View mKeysExchangedView;
    private ImageView mPresenceView;
    private QuickContactBadge mAvatarView;

    static private Drawable sDefaultContactImage;
    
    private Conversation mConversationHeader;

    public TabContactsItem(Context context) {
        super(context);
    }

    public TabContactsItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (sDefaultContactImage == null) {
            sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_contact_picture);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFromView = (TextView) findViewById(R.id.contacts_from);
        mMessageView = (TextView) findViewById(R.id.contacts_subject);
        mDateView = (TextView) findViewById(R.id.contacts_date);
        mBeingSentView = findViewById(R.id.contacts_being_sent);
        mWaitingForReplyView = findViewById(R.id.contacts_waiting_for_reply);
        mKeysExchangedView = findViewById(R.id.contacts_keys_exchanged);
        mPresenceView = (ImageView) findViewById(R.id.contacts_presence);
        mAvatarView = (QuickContactBadge) findViewById(R.id.contacts_avatar);
    }

    public void setPresenceIcon(int iconId) {
        if (iconId == 0) {
            mPresenceView.setVisibility(View.GONE);
        } else {
            mPresenceView.setImageResource(iconId);
            mPresenceView.setVisibility(View.VISIBLE);
        }
    }
    
    private void setConversationHeader(Conversation conv) {
    	mConversationHeader = conv;
    }

    @SuppressWarnings("unused")
	private Conversation getConversationHeader() {
    	return mConversationHeader;
    }

    /**
     * Only used for header binding.
     */
    public void bind(String title, String explain) {
        mFromView.setText(title);
        mMessageView.setText(explain);
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

    private void updateAvatarView() {
/*    	
        ConversationListItemData ch = mConversationHeader;

        Drawable avatarDrawable;
        if (ch.getContacts().size() == 1) {
            Contact contact = ch.getContacts().get(0);
            avatarDrawable = contact.getAvatar(mContext, sDefaultContactImage);

            if (contact.existsInDatabase()) {
                mAvatarView.assignContactUri(contact.getUri());
            } else {
                mAvatarView.assignContactFromPhone(contact.getNumber(), true);
            }
        } else {
            // TODO get a multiple recipients asset (or do something else)
            avatarDrawable = sDefaultContactImage;
            mAvatarView.assignContactUri(null);
        }
        mAvatarView.setImageDrawable(avatarDrawable);
        mAvatarView.setVisibility(View.VISIBLE);*/
    	
    	Drawable avatarDrawable;
        avatarDrawable = sDefaultContactImage;
        //mAvatarView.assignContactUri(null);
        mAvatarView.assignContactFromPhone(mConversationHeader.getPhoneNumber(), true);
        mAvatarView.setImageDrawable(avatarDrawable);
        mAvatarView.setVisibility(View.VISIBLE);
    }

    /*
    private void updateFromView() {
        ConversationListItemData ch = mConversationHeader;
        ch.updateRecipients();
        mFromView.setText(formatMessage(ch));
        setPresenceIcon(ch.getContacts().getPresenceResId());
        updateAvatarView();
    }

    public void onUpdate(Contact updated) {
        mHandler.post(new Runnable() {
            public void run() {
                updateFromView();
            }
        });
    }*/

    public final void bind(Context context, final Conversation conv) {
        String simNumber = Utils.getSimNumber(context);
    	setConversationHeader(conv);
    	
		try {
			SessionKeys keys = conv.getSessionKeys(simNumber);
	    	if (keys != null) {
	    		switch(keys.getStatus()) {
	    		default:
	    		case BEING_SENT:
	    			mBeingSentView.setVisibility(VISIBLE);
	    			mMessageView.setText("Sending keys...");
	    			break;
	    		case WAITING_FOR_REPLY:
	    			mWaitingForReplyView.setVisibility(VISIBLE);
	    			mMessageView.setText("Waiting for confirmation...");
	    			break;
	    		case KEYS_EXCHANGED:
	    			mKeysExchangedView.setVisibility(VISIBLE);
	    			mMessageView.setText("Encrypted connection");
	    			break;
	    		}
	    	}
	    	else
	    		mMessageView.setText("No keys found");

	        mDateView.setText("");
	        mFromView.setText(conv.getPhoneNumber());
	        updateAvatarView();
		} catch (DatabaseFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public final void unbind() {
    }
}
