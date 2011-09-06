package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;

import org.joda.time.DateTime;

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
		INTERNAL_ERROR,
		TIMESTAMP_IN_FUTURE,
		TIMESTAMP_OLD
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
		ArrayList<ParseResult> results = new ArrayList<ParseResult>();
		// have the pending messages sorted into groups by their type and ID
		ArrayList<ArrayList<Pending>> idGroups = database.getAllIdGroups();
		for(ArrayList<Pending> idGroup : idGroups) {
			// check that there are not too many parts
			if (idGroup.size() > 255)
				results.add(new ParseResult(idGroup, PendingParseResult.REDUNDANT_PARTS, null));
			else if (idGroup.size() > 0) {
				switch (idGroup.get(0).getType()) {
				case HANDSHAKE:
				case CONFIRM:
					results.add(KeysMessage.parseKeysMessage(idGroup));
					break;
				case TEXT:
					results.add(TextMessage.parseTextMessage(idGroup));
					// TODO: increment key ID to the lowest we decoded
					break;
				}
			}
		}
		
		for (ParseResult parseResult : results) {
			if (parseResult.getResult() == PendingParseResult.OK_CONFIRM_MESSAGE ||
				parseResult.getResult() == PendingParseResult.OK_TEXT_MESSAGE ) {
				results.remove(parseResult);
				parseResult.removeFromDb(database);
			}
		}
		
		return results;
	}

}
