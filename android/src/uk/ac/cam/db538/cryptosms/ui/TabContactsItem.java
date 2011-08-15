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

import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.StorageUtils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import android.util.AttributeSet;
import android.view.View;
import android.widget.QuickContactBadge;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This class manages the view for given contact.
 */
public class TabContactsItem extends RelativeLayout {
    private TextView mFromView;
    private TextView mStatusView;
    private ImageView mIconView;
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
        mStatusView = (TextView) findViewById(R.id.contacts_status);
        mIconView = (ImageView) findViewById(R.id.contacts_icon);
        mAvatarView = (QuickContactBadge) findViewById(R.id.contacts_avatar);
    }

    private void setConversationHeader(Conversation conv) {
    	mConversationHeader = conv;
    }

	public Conversation getConversationHeader() {
    	return mConversationHeader;
    }

    /**
     * Only used for header binding.
     */
    public void bind(String title, String explain) {
        mFromView.setText(title);
        mStatusView.setText(explain);
    }

    public final void bind(Conversation conv) {
    	setConversationHeader(conv);
    	
    	Context context = this.getContext();
    	Resources res = context.getResources();

		try {
			SessionKeys keys = StorageUtils.getSessionKeysForSim(conv);
	    	if (keys != null) {
	    		switch(keys.getStatus()) {
	    		default:
	    		case SENDING_KEYS:
	    			mStatusView.setText(res.getString(R.string.item_contacts_sending_keys));
	    			mIconView.setImageDrawable(res.getDrawable(R.drawable.item_contacts_sending_something));
	    			break;
	    		case SENDING_CONFIRMATION:
	    			mStatusView.setText(res.getString(R.string.item_contacts_sending_confirmation));
	    			mIconView.setImageDrawable(res.getDrawable(R.drawable.item_contacts_sending_something));
	    			break;
	    		case WAITING_FOR_REPLY:
	    			mStatusView.setText(res.getString(R.string.item_contacts_waiting_for_reply));
	    			mIconView.setImageDrawable(res.getDrawable(R.drawable.item_contacts_waiting_for_reply));
	    			break;
	    		case KEYS_EXCHANGED:
	    			mStatusView.setText(res.getString(R.string.item_contacts_keys_exchanged));
	    			mIconView.setImageDrawable(res.getDrawable(R.drawable.item_contacts_keys_exchanged));
	    			break;
	    		}
	    	}
	    	else {
	    		mStatusView.setText(res.getString(R.string.item_contacts_keys_error));
	    		mIconView.setImageDrawable(context.getResources().getDrawable(R.drawable.item_contacts_keys_error));
	    	}
	    	
	    	Contact contact = Contact.getContact(context, conv.getPhoneNumber());
	    	if (contact.getName().length() > 0)
	    		mFromView.setText(contact.getName());
	    	else
	    		mFromView.setText(contact.getPhoneNumber());
	        mIconView.setVisibility(VISIBLE);

	    	Drawable avatarDrawable = contact.getAvatar(context, sDefaultContactImage);
            if (contact.existsInDatabase()) {
                mAvatarView.assignContactUri(contact.getUri());
            } else {
                mAvatarView.assignContactFromPhone(conv.getPhoneNumber(), true);
            }
	        mAvatarView.setImageDrawable(avatarDrawable);
	        mAvatarView.setVisibility(View.VISIBLE);
	        
		} catch (StorageFileException ex) {
			State.fatalException(ex);
			return;
		}
    }
}
