package uk.ac.cam.db538.cryptosms.storage;

import java.io.IOException;

import android.content.Context;

import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys.SessionKeysStatus;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys.SimNumber;

public class StorageUtils {

	/**
	 * Tries to find session keys for this particular SIM,
	 * either by phone number or (if not available) by SIM's serial number
	 * @param context
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 * @throws WrongKeyException 
	 */
	public static SessionKeys getSessionKeysForSIM(Conversation conv, final Context context) throws StorageFileException, IOException {
		SessionKeys result = null;
		
		String simPhoneNumberString = SimCard.getSingleton().getSimPhoneNumber(context);
		String simSerialString = SimCard.getSingleton().getSimSerialNumber(context);
		SimNumber simSerial = new SimNumber(simSerialString, true);
		SessionKeys keysSerial = conv.getSessionKeys(simSerial);
		
		if (simPhoneNumberString == null || simPhoneNumberString.length() == 0) {
			// no phone number, return keys for serial number or null
			result = keysSerial; 
		} else {
			SimNumber simPhoneNumber = new SimNumber(simPhoneNumberString, false);
			// try assigning all the (possible) keys assigned to 
			// SIM serial to the phone number
			if (keysSerial != null)
				Conversation.changeAllSessionKeys(simSerial, simPhoneNumber);
			result = conv.getSessionKeys(simPhoneNumber);
		}
		return result;
	}

	/**
	 * Tries to find session keys for this SIM and if it succeeds, 
	 * returns whether they have been successfully exchanged.
	 * @param context
	 * @param conv
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public static boolean hasKeysExchangedForSIM(Context context, Conversation conv) throws StorageFileException, IOException {
		SessionKeys keys = StorageUtils.getSessionKeysForSIM(conv, context);
		if (keys == null)
			return false;
		return keys.getStatus() == SessionKeysStatus.KEYS_EXCHANGED;
	}
}
