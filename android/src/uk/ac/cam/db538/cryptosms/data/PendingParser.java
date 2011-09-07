package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;
import java.util.Collections;

import org.joda.time.DateTime;

import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.state.State.StateChangeListener;
import uk.ac.cam.db538.cryptosms.storage.Storage;

import android.os.AsyncTask;

public class PendingParser {
	public enum PendingParseResult {
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
	
	private static PendingParser mParser;
	
	public static void init(DbPendingAdapter database) {
		if (mParser == null)
			mParser = new PendingParser(database);
	}
	
	public static PendingParser getSingleton() {
		return mParser;
	}	
	
	private DbPendingAdapter mDatabase;
	private ArrayList<ParseResult> mParseResults;
	
	private PendingParser(DbPendingAdapter database) {
		mDatabase = database;
		mParseResults = new ArrayList<ParseResult>();
		
		State.addListener(new StateChangeListener() {
			@Override
			public void onNewEvent() {
				parseEvents();
			}
		});
	}
	
	public ArrayList<ParseResult> getParseResults() {
		return mParseResults;
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
	
	public void parseEvents() {
		synchronized(mDatabase) {
			new EventsUpdateTask(mParseResults, mDatabase).execute();
		}
	}

	private class EventsUpdateTask extends AsyncTask<Void, Void, Void> {

		private DbPendingAdapter mDatabase;
		private ArrayList<ParseResult> mParseResults;
		private boolean mUpdateConversations;
		
		public EventsUpdateTask(ArrayList<ParseResult> parseResults, DbPendingAdapter database) {
			this.mParseResults = parseResults;
			this.mDatabase = database;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			mParseResults.clear();
			
			// parse pending stuff
			mDatabase.open();
			try {
				// have the pending messages sorted into groups by their type and ID
				ArrayList<ArrayList<Pending>> idGroups = mDatabase.getAllIdGroups();
				for(ArrayList<Pending> idGroup : idGroups) {
					// check that there are not too many parts
					if (idGroup.size() > 255)
						mParseResults.add(new ParseResult(idGroup, PendingParseResult.REDUNDANT_PARTS, null));
					else if (idGroup.size() > 0) {
						switch (idGroup.get(0).getType()) {
						case HANDSHAKE:
						case CONFIRM:
							mParseResults.add(KeysMessage.parseKeysMessage(idGroup));
							break;
						case TEXT:
							mParseResults.add(TextMessage.parseTextMessage(idGroup));
							// TODO: increment key ID to the lowest we decoded
							break;
						}
					}
				}
				
				mUpdateConversations = false;
				
				for (ParseResult parseResult : mParseResults) {
					if (parseResult.getResult() == PendingParseResult.OK_CONFIRM_MESSAGE ||
						parseResult.getResult() == PendingParseResult.OK_TEXT_MESSAGE ) {
						mParseResults.remove(parseResult);
						parseResult.removeFromDb(mDatabase);
						mUpdateConversations = true;
					}
				}

				Collections.sort(mParseResults, Collections.reverseOrder());
			} finally {
				mDatabase.close();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			State.notifyEventsParsed();			
			if (mUpdateConversations)
				Storage.notifyChange();
		}
	}
	
	public static void forceParsing() {
		getSingleton().parseEvents();
	}
}
