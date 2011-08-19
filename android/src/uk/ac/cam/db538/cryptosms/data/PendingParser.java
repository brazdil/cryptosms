package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;

import org.joda.time.DateTime;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageType;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public class PendingParser {
	public static enum PendingParseResult {
		OK_KEYS_MESSAGE,
		MISSING_PARTS,
		REDUNDANT_PARTS,
		COULD_NOT_DECRYPT,
		COULD_NOT_VERIFY,
		CORRUPTED_DATA,
		NO_SESSION_KEYS,
		UNKNOWN_SENDER
	}

	/**
	 * Holds the results of pending messages parsing
	 * @author db538
	 *
	 */
	public static class Event implements Comparable<Event> {
		private ArrayList<Pending> mIdGroup;
		private PendingParseResult mResult;
		private Message mMessage;
		private DateTime mTimeStamp;
		
		Event(ArrayList<Pending> idGroup, PendingParseResult result, Message message) {
			mIdGroup = idGroup;
			mResult = result;
			mMessage = message;

			DateTime timeStamp = new DateTime(0);
			for (Pending p : mIdGroup) {
				DateTime thisStamp = p.getTimeStamp();
				if (thisStamp.compareTo(timeStamp) > 0)
					timeStamp = thisStamp;
			}
			mTimeStamp = timeStamp;
		}
		
		public ArrayList<Pending> getIdGroup() {
			return mIdGroup;
		}
		
		public PendingParseResult getResult() {
			return mResult;
		}
		
		public Message getMessage() {
			return mMessage;
		}
		
		public DateTime getTimestamp() {
			return mTimeStamp;
		}

		@Override
		public int compareTo(Event another) {
			return this.getTimestamp().compareTo(another.getTimestamp());
		}
	}
	
	// TODO: What happens if two first messages have the same sender, type and ID???
	// Unit test it!!!
	
	/**
	 * Returns an array list of message parts sorted into separate array lists
	 * by their types and IDs.
	 * @param context
	 * @return
	 */
	private static ArrayList<ArrayList<Pending>> getMatchingParts(DbPendingAdapter database) {
		ArrayList<ArrayList<Pending>> result = new ArrayList<ArrayList<Pending>>();
		
		// for each first part
		ArrayList<Pending> firstParts = database.getAllFirstParts();
		for(Pending firstPart : firstParts) {
			// figure out the type of other parts
			MessageType type = MessageType.UNKNOWN;
			if (firstPart.getType() == MessageType.MESSAGE_FIRST)
				type = MessageType.MESSAGE_PART;
			else if (firstPart.getType() == MessageType.KEYS_FIRST)
				type = MessageType.KEYS_PART;
			// get the other parts
			ArrayList<Pending> otherParts = database.getAllParts(firstPart.getSender(), type, firstPart.getId());
			// append the first part
			otherParts.add(0, firstPart);
			// and add the whole thing to resulting array
			result.add(otherParts);
		}
		return result;
	}
	
	private static Event parseKeysMessage(ArrayList<Pending> idGroup) {
		// check the sender
		Contact contact = Contact.getContact(MyApplication.getSingleton().getApplicationContext(), idGroup.get(0).getSender());
		if (!contact.existsInDatabase())
			return new Event(idGroup, PendingParseResult.UNKNOWN_SENDER, null);

		// check we have all the parts
		// there shouldn't be more than 6 of them
		int groupSize = idGroup.size();
		int expectedGroupSize = KeysMessage.getPartsCount(); 
		if (groupSize < expectedGroupSize || groupSize <= 0)
			return new Event(idGroup, PendingParseResult.MISSING_PARTS, null);
		else if (groupSize > expectedGroupSize)
			return new Event(idGroup, PendingParseResult.REDUNDANT_PARTS, null);
		
		// get the data
		byte[][] dataParts = new byte[groupSize][];
		int filledParts = 0;
		for (Pending p : idGroup) {
			byte[] dataPart = p.getData();
			int index = KeysMessage.getMessageIndex(dataPart);
			if (index >= 0 && index < idGroup.size()) {
				// index is fine, check that there wasn't the same one already
				if (dataParts[index] == null) {
					// first time we stumbled upon this index
					// store the message part data in the array
					dataParts[index] = dataPart;
					filledParts++;
					// if it's not a first message, but it's got index 0
					if (Message.getMessageType(dataPart) != MessageType.KEYS_FIRST && index == 0)
						// sounds like rubbish to me
						return new Event(idGroup, PendingParseResult.CORRUPTED_DATA, null);
				} else
					// more parts of the same index
					return new Event(idGroup, PendingParseResult.REDUNDANT_PARTS, null);
			} else
				// index is bigger than the number of messages in ID group
				// therefore some parts have to be missing or the data is corrupted
				return new Event(idGroup, PendingParseResult.MISSING_PARTS, null);
		}
		// the array was filled with data, so check that there aren't any missing
		if (filledParts != expectedGroupSize)
			return new Event(idGroup, PendingParseResult.MISSING_PARTS, null);
		
		// lets put the data together
		byte[] dataEncryptedSigned = new byte[KeysMessage.getTotalDataLength()];
		for (int i = 0; i < expectedGroupSize; ++i) {
			try {
				// get the data 
				// it can't be too long, thanks to getMessageData
				// but it can be too short (throws IndexOutOfBounds exception
				byte[] relevantData = KeysMessage.getMessageData(dataParts[i]);
				System.arraycopy(relevantData, 0, dataEncryptedSigned, KeysMessage.getDataPartOffset(i), KeysMessage.getDataPartLength(i));
			} catch (RuntimeException e) {
				return new Event(idGroup, PendingParseResult.CORRUPTED_DATA, null);
			}
		}
		
		// cut out the rubbish part at the end
		dataEncryptedSigned = LowLevel.cutData(dataEncryptedSigned, 0, KeysMessage.getEncryptedDataLength());
		
		// check the signature
		byte[] dataEncrypted = null;
		try {
			 dataEncrypted = Encryption.getEncryption().verify(dataEncryptedSigned, contact.getId());
		} catch (EncryptionException e) {
			return new Event(idGroup, PendingParseResult.COULD_NOT_VERIFY, null);
		} catch (WrongKeyDecryptionException e) {
			return new Event(idGroup, PendingParseResult.COULD_NOT_VERIFY, null);
		}
		
		// now decrypt the data
		byte[] dataDecrypted = null;
		try {
			dataDecrypted = Encryption.getEncryption().decryptAsymmetric(dataEncrypted);
		} catch (EncryptionException e) {
			return new Event(idGroup, PendingParseResult.COULD_NOT_DECRYPT, null);
		} catch (WrongKeyDecryptionException e) {
			return new Event(idGroup, PendingParseResult.COULD_NOT_DECRYPT, null);
		}
		
		// check the length
		if (dataDecrypted.length != KeysMessage.LENGTH_KEYS)
			return new Event(idGroup, PendingParseResult.CORRUPTED_DATA, null);
		
		// all seems to be fine, so just retrieve the keys and return the result
		return new Event(idGroup, 
		                            PendingParseResult.OK_KEYS_MESSAGE, 
		                            new KeysMessage(
		                            	// we have to swap the keys
		                            	// the other guy's out-key is my in-key...
		                            	LowLevel.cutData(dataDecrypted, Encryption.SYM_KEY_LENGTH, Encryption.SYM_KEY_LENGTH),
		                            	LowLevel.cutData(dataDecrypted, 0, Encryption.SYM_KEY_LENGTH)
		                            ));
	}
	
	private static Event parseTextMessage(ArrayList<Pending> idGroup) {
		return null;
	}

	public static ArrayList<Event> parsePending(DbPendingAdapter database) {
		ArrayList<Event> result = new ArrayList<Event>();
		// have the pending messages sorted into groups by their type and ID
		ArrayList<ArrayList<Pending>> idGroups = getMatchingParts(database);
		for(ArrayList<Pending> idGroup : idGroups) {
			// check that there are not too many parts
			if (idGroup.size() > 255)
				result.add(new Event(idGroup, PendingParseResult.REDUNDANT_PARTS, null));
			else {
				// find the first part (usually first in the list)
				boolean firstFound = false; 
				for (Pending p : idGroup) {
					if (!firstFound) {
						MessageType type = p.getType();
						if (type == MessageType.KEYS_FIRST) {
							firstFound = true;
							result.add(parseKeysMessage(idGroup));
						} else if (type == MessageType.MESSAGE_FIRST) {
							firstFound = true;
							result.add(parseTextMessage(idGroup));
						}
					}
				}
			}
		}
		return result;
	}

}
