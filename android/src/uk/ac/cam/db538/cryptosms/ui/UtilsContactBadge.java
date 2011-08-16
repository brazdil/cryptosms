package uk.ac.cam.db538.cryptosms.ui;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;

public class UtilsContactBadge {
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
