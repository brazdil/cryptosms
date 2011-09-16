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

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;

public class UtilsContactBadge {
	
	/**
	 * Sets the badge.
	 *
	 * @param contact the contact
	 * @param root the root
	 */
	public static void setBadge(Contact contact, View root) {
		TextView viewName = (TextView) root.findViewById(R.id.person_name);
		TextView viewPhoneNumber = (TextView) root.findViewById(R.id.person_phone_number);
		QuickContactBadge viewAvatar = (QuickContactBadge) root.findViewById(R.id.person_avatar);
		
	    if (contact.getName().length() > 0) {
	    	viewName.setText(contact.getName());
	    	viewPhoneNumber.setText(contact.getPhoneNumber());
	    }
	    else {
	    	viewName.setText(contact.getPhoneNumber());
	    	viewPhoneNumber.setText(new String());
	    }

	    Drawable avatarDrawable = contact.getAvatar(root.getContext(), MyApplication.getSingleton().getDefaultContactImage());
        if (contact.existsInDatabase()) {
        	viewAvatar.assignContactUri(contact.getUri());
        } else {
        	viewAvatar.assignContactFromPhone(contact.getPhoneNumber(), true);
        }
        viewAvatar.setImageDrawable(avatarDrawable);
        viewAvatar.setVisibility(View.VISIBLE);
	}
}
