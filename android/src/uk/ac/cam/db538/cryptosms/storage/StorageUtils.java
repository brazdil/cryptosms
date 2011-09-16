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
package uk.ac.cam.db538.cryptosms.storage;

import uk.ac.cam.db538.cryptosms.SimCard;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys.SessionKeysStatus;
import uk.ac.cam.db538.cryptosms.utils.SimNumber;

/*
 * Utilities for common tasks on storage file
 */
public class StorageUtils {

	/**
	 * Tries to find session keys for this particular SIM,
	 * either by phone number or (if not available) by SIM's serial number.
	 *
	 * @param conv the conv
	 * @return the session keys for sim
	 * @throws StorageFileException the storage file exception
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
	 *
	 * @param conv the conv
	 * @return true, if successful
	 * @throws StorageFileException the storage file exception
	 */
	public static boolean hasKeysExchangedForSim(Conversation conv) throws StorageFileException {
		SessionKeys keys = StorageUtils.getSessionKeysForSim(conv);
		if (keys == null)
			return false;
		return keys.getStatus() == SessionKeysStatus.KEYS_EXCHANGED;
	}

	/**
	 * Tries to find any session keys for this SIM.
	 *
	 * @param conv the conv
	 * @return true, if successful
	 * @throws StorageFileException the storage file exception
	 */
	public static boolean hasKeysForSim(Conversation conv) throws StorageFileException {
		return getSessionKeysForSim(conv) != null;
	}
}
