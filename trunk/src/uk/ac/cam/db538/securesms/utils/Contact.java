package uk.ac.cam.db538.securesms.utils;

import java.io.IOException;
import java.io.InputStream;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Presence;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;

public class Contact {
	
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
            + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
            + " AND " + Data.RAW_CONTACT_ID + " IN "
                    + "(SELECT raw_contact_id "
                    + " FROM phone_lookup"
                    + " WHERE normalized_number GLOB('+*'))";

    private static final Uri PHONES_WITH_PRESENCE_URI = Data.CONTENT_URI;

    private static final String[] CALLER_ID_PROJECTION = new String[] {
            Phone.NUMBER,                   // 0
            Phone.LABEL,                    // 1
            Phone.DISPLAY_NAME,             // 2
            Phone.CONTACT_ID,               // 3
            Phone.CONTACT_PRESENCE,         // 4
            Phone.CONTACT_STATUS,           // 5
    };

    private static final int PHONE_NUMBER_COLUMN = 0;
    private static final int PHONE_LABEL_COLUMN = 1;
    private static final int CONTACT_NAME_COLUMN = 2;
    private static final int CONTACT_ID_COLUMN = 3;
    private static final int CONTACT_PRESENCE_COLUMN = 4;
    private static final int CONTACT_STATUS_COLUMN = 5;
    
    private String mPhoneNumber;
    private String mLabel;
    private String mName;
    private long mPersonId;
    private int mPresenceResId;
    private String mPresenceText;
    private BitmapDrawable mAvatar;
    byte[] mAvatarData;
	
	public Contact(String phoneNumber) {
		setPhoneNumber(phoneNumber);
        setName("");
        setLabel("");
        mPersonId = 0;
        mPresenceResId = 0;
	}

    public synchronized Drawable getAvatar(Context context, Drawable defaultValue) {
        if (mAvatar == null) {
            if (mAvatarData != null) {
                Bitmap b = BitmapFactory.decodeByteArray(mAvatarData, 0, mAvatarData.length);
                mAvatar = new BitmapDrawable(context.getResources(), b);
            }
        }
        return mAvatar != null ? mAvatar : defaultValue;
    }

    public synchronized boolean existsInDatabase() {
        return (mPersonId > 0);
    }    

    public synchronized Uri getUri() {
        return ContentUris.withAppendedId(Contacts.CONTENT_URI, mPersonId);
    }

    /**
     * Queries the caller id info with the phone number.
     * @return a Contact containing the caller id info corresponding to the number.
     */
    public static Contact getContact(Context context, String phoneNumber) {
        phoneNumber = Common.formatPhoneNumber(phoneNumber);
        Contact entry = new Contact(phoneNumber);

        //if (LOCAL_DEBUG) log("queryContactInfoByNumber: number=" + number);

        // We need to include the phone number in the selection string itself rather then
        // selection arguments, because SQLite needs to see the exact pattern of GLOB
        // to generate the correct query plan
        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        Cursor cursor = context.getContentResolver().query(
                PHONES_WITH_PRESENCE_URI,
                CALLER_ID_PROJECTION,
                selection,
                new String[] { phoneNumber },
                null);

        if (cursor == null) {
            return entry;
        }

        try {
            if (cursor.moveToFirst()) {
                synchronized (entry) {
                    entry.setLabel(cursor.getString(PHONE_LABEL_COLUMN));
                    entry.setName(cursor.getString(CONTACT_NAME_COLUMN));
                    entry.mPersonId = cursor.getLong(CONTACT_ID_COLUMN);
                    entry.mPresenceResId = getPresenceIconResourceId(
                            cursor.getInt(CONTACT_PRESENCE_COLUMN));
                    entry.mPresenceText = cursor.getString(CONTACT_STATUS_COLUMN);
                }

                byte[] data = loadAvatarData(context, entry);

                synchronized (entry) {
                    entry.mAvatarData = data;
                }

            }
        } finally {
            cursor.close();
        }

        return entry;
    }

    private static int getPresenceIconResourceId(int presence) {
        // TODO: must fix for SDK
        if (presence != Presence.OFFLINE) {
            return Presence.getPresenceIconResourceId(presence);
        }

        return 0;
    }
    
    /*
     * Load the avatar data from the cursor into memory.  Don't decode the data
     * until someone calls for it (see getAvatar).  Hang onto the raw data so that
     * we can compare it when the data is reloaded.
     * TODO: consider comparing a checksum so that we don't have to hang onto
     * the raw bytes after the image is decoded.
     */
    private static byte[] loadAvatarData(Context context, Contact entry) {
        byte [] data = null;

        if (entry.mPersonId == 0 || entry.mAvatar != null) {
            return null;
        }

        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, entry.mPersonId);

        InputStream avatarDataStream = Contacts.openContactPhotoInputStream(
                    context.getContentResolver(),
                    contactUri);
        try {
            if (avatarDataStream != null) {
                data = new byte[avatarDataStream.available()];
                avatarDataStream.read(data, 0, data.length);
            }
        } catch (IOException ex) {
            //
        } finally {
            try {
                if (avatarDataStream != null) {
                    avatarDataStream.close();
                }
            } catch (IOException e) {
            }
        }

        return data;
    }

	public String getPhoneNumber() {
		return mPhoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.mPhoneNumber = phoneNumber;
	}

	public String getLabel() {
		return mLabel;
	}

	public void setLabel(String label) {
		this.mLabel = label;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}

}
