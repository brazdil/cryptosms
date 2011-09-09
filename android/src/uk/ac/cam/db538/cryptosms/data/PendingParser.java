package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import org.joda.time.DateTime;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.SimCard;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.state.State.StateChangeListener;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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
	
	public static void init(Context context) {
		if (mParser == null)
			mParser = new PendingParser(context);
	}
	
	public static PendingParser getSingleton() {
		return mParser;
	}	
	
	private Context mContext;
	private ArrayList<ParseResult> mParseResults;
	
	private PendingParser(Context context) {
		mContext = context;
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
	
	public synchronized void parseEvents() {
		new EventsUpdateTask().execute();
	}

	private class EventsUpdateTask extends AsyncTask<Void, Void, Void> {

		private boolean mUpdateConversations;
		private ArrayList<ParseResult> mParseResults;
		private Exception mException = null;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			State.notifyEventParsingStarted();
			Log.d(MyApplication.APP_TAG, "Parsing...");
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				mParseResults = new ArrayList<PendingParser.ParseResult>();
				
				// parse pending stuff
				DbPendingAdapter database = new DbPendingAdapter(mContext);
				database.open();
				try {
					// have the pending messages sorted into groups by their type and ID
					ArrayList<ArrayList<Pending>> idGroups = database.getAllIdGroups();
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
								break;
							}
						}
					}
					
					mUpdateConversations = false;
					HashMap<String, Integer> mapToBeHashed = new HashMap<String, Integer>();
					
					for (ParseResult parseResult : mParseResults) {
						if (parseResult.getResult() == PendingParseResult.OK_CONFIRM_MESSAGE ||
							parseResult.getResult() == PendingParseResult.OK_TEXT_MESSAGE ) {
							mParseResults.remove(parseResult);
							parseResult.removeFromDb(database);
							mUpdateConversations = true;
						}
						
						if (parseResult.getResult() == PendingParseResult.OK_TEXT_MESSAGE) {
							Pending msgPart = parseResult.mIdGroup.get(0);
							String sender = msgPart.getSender();
							int toBeHashed = ((TextMessage) parseResult.getMessage()).getToBeHashed();
							Integer toBeHashedMax = mapToBeHashed.get(sender);
							if (toBeHashedMax == null)
								toBeHashedMax = Integer.valueOf(0);
							if (toBeHashed > toBeHashedMax)
								mapToBeHashed.put(sender, toBeHashed);
						}
					}
					
					// increment key ID to the highest we decoded
					for (Entry<String, Integer> elem : mapToBeHashed.entrySet()) {
						try {
							Conversation conv = Conversation.getConversation(elem.getKey());
							SessionKeys keys = conv.getSessionKeys(SimCard.getSingleton().getNumber());
							keys.incrementIn(elem.getValue());
							keys.saveToFile();
						} catch (StorageFileException ex) {
							mException = ex;
							return null;
						}
					}
	
					Collections.sort(mParseResults, Collections.reverseOrder());
				} finally {
					database.close();
				}
			} catch (WrongKeyDecryptionException ex) {
				mException = ex;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			if (mException != null) {
				State.fatalException(mException);
				return;
			}
			
			PendingParser.this.mParseResults = this.mParseResults;
			
			State.notifyEventParsingFinished();			
			if (mUpdateConversations)
				Storage.notifyChange();
		}
	}
	
	public static void forceParsing() {
		getSingleton().parseEvents();
	}
}
