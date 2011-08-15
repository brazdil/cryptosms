package uk.ac.cam.db538.cryptosms.storage;

import uk.ac.cam.db538.cryptosms.data.SimCard;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys.SessionKeysStatus;
import uk.ac.cam.db538.cryptosms.utils.SimNumber;

public class StorageUtils {

	/**
	 * Tries to find session keys for this particular SIM,
	 * either by phone number or (if not available) by SIM's serial number
	 * @return
	 * @throws StorageFileException
	 */
	public static SessionKeys getSessionKeysForSim(Conversation conv) throws StorageFileException {
		SimNumber simNumber = SimCard.getSingleton().getNumber();
		
		if (simNumber == null)
			return null;
		else if (simNumber.isSerial())
			return conv.getSessionKeys(simNumber);
		else {
			SimNumber simSerial = SimCard.getSingleton().getSerialNumber();
			SessionKeys keysSerial = conv.getSessionKeys(simSerial);
			if (keysSerial != null)
				Conversation.changeAllSessionKeys(simSerial, simNumber);
			return conv.getSessionKeys(simNumber);
		}
	}

	/**
	 * Tries to find session keys for this SIM and if it succeeds, 
	 * returns whether they have been successfully exchanged.
	 * @param conv
	 * @return
	 * @throws StorageFileException
	 */
	public static boolean hasKeysExchangedForSim(Conversation conv) throws StorageFileException {
		SessionKeys keys = StorageUtils.getSessionKeysForSim(conv);
		if (keys == null)
			return false;
		return keys.getStatus() == SessionKeysStatus.KEYS_EXCHANGED;
	}

	/**
	 * Tries to find any session keys for this SIM
	 * @param conv
	 * @return
	 * @throws StorageFileException
	 */
	public static boolean hasKeysForSim(Conversation conv) throws StorageFileException {
		return getSessionKeysForSim(conv) != null;
	}
}
