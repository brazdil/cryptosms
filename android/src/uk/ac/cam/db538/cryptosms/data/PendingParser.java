package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;

import org.joda.time.DateTime;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageType;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.ui.ActivityLists;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public class PendingParser {
	public static enum PendingParseResult {
		OK_HANDSHAKE_MESSAGE,
		OK_TEXT_MESSAGE,
		OK_CONFIRM_MESSAGE,
		MISSING_PARTS,
		REDUNDANT_PARTS,
		COULD_NOT_DECRYPT,
		COULD_NOT_VERIFY,
		CORRUPTED_DATA,
		NO_SESSION_KEYS,
		UNKNOWN_SENDER,
		INTERNAL_ERROR
	}

	/**
	 * Holds the results of pending messages parsing
	 * @author db538
	 *
	 */
	public static class ParseResult implements Comparable<ParseResult> {
		private ArrayList<Pending> mIdGroup;
		private PendingParseResult mResult;
		private Message mMessage;
		private DateTime mTimeStamp;
		
		ParseResult(ArrayList<Pending> idGroup, PendingParseResult result, Message message) {
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
		public int compareTo(ParseResult another) {
			return this.getTimestamp().compareTo(another.getTimestamp());
		}
		
		public String getSender() {
			if (mIdGroup != null && mIdGroup.size() > 0)
				return mIdGroup.get(0).getSender();
			else
				return null;
		}
		
		public void removeFromDb(DbPendingAdapter database) {
			for (Pending p : mIdGroup)
				database.removeEntry(p);
		}
	}
	
	public static ArrayList<ParseResult> parsePending(DbPendingAdapter database) {
		ArrayList<ParseResult> result = new ArrayList<ParseResult>();
		// have the pending messages sorted into groups by their type and ID
		ArrayList<ArrayList<Pending>> idGroups = database.getAllIdGroups();
		for(ArrayList<Pending> idGroup : idGroups) {
			// check that there are not too many parts
			if (idGroup.size() > 255)
				result.add(new ParseResult(idGroup, PendingParseResult.REDUNDANT_PARTS, null));
			else if (idGroup.size() > 0) {
				switch (idGroup.get(0).getType()) {
				case HANDSHAKE:
				case CONFIRM:
					result.add(KeysMessage.parseKeysMessage(idGroup));
					break;
				case TEXT:
					result.add(TextMessage.parseTextMessage(idGroup));
					break;
				}
			}
		}
		return result;
	}

}
