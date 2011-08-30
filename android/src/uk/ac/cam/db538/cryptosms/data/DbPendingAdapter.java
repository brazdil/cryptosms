package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.joda.time.format.ISODateTimeFormat;

import uk.ac.cam.db538.cryptosms.data.Message.MessageType;
import uk.ac.cam.db538.cryptosms.utils.PhoneNumber;

import android.content.ContentValues;
import android.content.Context;
import android.database.*;
import android.database.sqlite.*;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public class DbPendingAdapter {
	private static final String DATABASE_NAME = "pending.db";
	private static final String DATABASE_TABLE = "sms";
	private static final int DATABASE_VERSION = 1;
 
  	// The index (key) column name for use in where clauses
	public static final String KEY_ID = "_id";
	public static final int COLUMN_ID = 0;
	
	// The name and column index of each column in your database
	public static final String KEY_SENDER = "sender"; 
	public static final int COLUMN_SENDER = 1;
	public static final String KEY_TIMESTAMP = "timeStamp"; 
	public static final int COLUMN_TIMESTAMP = 2;
	public static final String KEY_DATA = "data"; 
	public static final int COLUMN_DATA = 3;
  
  	// SQL Statement to create a new database
	private static final String DATABASE_CREATE_TABLE = "create table " + 
		DATABASE_TABLE + " (" + KEY_ID + 
		" integer primary key autoincrement, " +
		KEY_SENDER + " text not null, " + 
		KEY_TIMESTAMP + " datetime not null, " +
		KEY_DATA + " blob not null );";

  	// Variable to hold the database instance
	private SQLiteDatabase mDatabase;
	// Context of the application using the database.
	private final Context mContext;
	// Database open/upgrade helper
	private DbPendingHelper mHelper;

	public DbPendingAdapter(Context context) {
		mContext = context;
		mHelper = new DbPendingHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public DbPendingAdapter open() throws SQLException {
		mDatabase = mHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDatabase.close();
	}
	
	private ContentValues getValues(Pending pending) {
		ContentValues values = new ContentValues();
		values.put(KEY_SENDER, pending.getSender());
		values.put(KEY_TIMESTAMP, ISODateTimeFormat.dateTime().print(pending.getTimeStamp()));
		values.put(KEY_DATA, pending.getData());
		return values;
	}
	
	private ArrayList<Pending> getPending(Cursor cursor) {
		ArrayList<Pending> list = new ArrayList<Pending>(cursor.getCount()); 
		if (cursor.moveToFirst()) {
			do {
				Pending pending = new Pending(
						cursor.getString(COLUMN_SENDER),
						ISODateTimeFormat.dateTimeParser().parseDateTime(cursor.getString(COLUMN_TIMESTAMP)),
						cursor.getBlob(COLUMN_DATA));
				pending.setRowIndex(cursor.getLong(COLUMN_ID));
				list.add(pending);
			} while (cursor.moveToNext());
		}
		return list;
	}

	public long insertEntry(Pending pending) {
		pending.setRowIndex(mDatabase.insert(DATABASE_TABLE, null, getValues(pending)));
		return pending.getRowIndex();
	}

	public boolean removeEntry(Pending pending) {
		return mDatabase.delete(DATABASE_TABLE, KEY_ID + "=" + pending.getRowIndex(), null) > 0;
	}

	public Pending getEntry(long rowIndex) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, 
				                        null,
				                        KEY_ID + "=" + rowIndex,
				                        null,
				                        null,
				                        null,
				                        null);
		ArrayList<Pending> result = getPending(cursor);
		if (result.size() > 0)
			return result.get(0);
		else
			return null;
	}
	
	private ArrayList<Pending> getAllMatchingEntries(String where) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, 
		                                null,
		                                where,
		                                null,
		                                null,
		                                null,
		                                null);
		ArrayList<Pending> result = getPending(cursor);
		cursor.close();
		return result;
	}

	public ArrayList<Pending> getAllEntries() {
		return getAllMatchingEntries(null);
	}
	
	public ArrayList<Pending> getAllSenderEntries(String sender) {
		return getAllMatchingEntries(KEY_SENDER + "='" + sender + "'");
	}
	
	public ArrayList<ArrayList<Pending>> getAllIdGroups() {
		ArrayList<Pending> allEntries = getAllEntries();
		Comparator<Pending> comparatorMain = new Comparator<Pending>() {
			@Override
			public int compare(Pending object1, Pending object2) {
				// level 1 - senders 
				if (PhoneNumber.compare(object1.getSender(), object2.getSender())) {
					// level 2 - types
					MessageType type1 = object1.getType();
					MessageType type2 = object2.getType();
					if (type1.equals(type2)) {
						// level 3 - ids
						return object1.getId() - object2.getId();
					} else
						return type1.compareTo(type2);
				} else
					return object1.getSender().compareToIgnoreCase(object2.getSender()); 
			}
		};
//		Comparator<Pending> comparatorIndex = new Comparator<Pending>() {
//			@Override
//			public int compare(Pending object1, Pending object2) {
//				return object1.getIndex() - object2.getIndex();
//			}
//		};
		
		// sort the entries according to their 
		// 1) sender
		// 2) type
		// 3) ID
		// 4) index
		Collections.sort(allEntries, comparatorMain);
		
		// now divide into groups
		ArrayList<ArrayList<Pending>> idGroups = new ArrayList<ArrayList<Pending>>();
		ArrayList<Pending> thisIdGroup = null;
		Pending lastItem = null;
		
		for (Pending p : allEntries) {
			// do we need to create a new id group?
			if (lastItem == null || comparatorMain.compare(lastItem, p) != 0) {
				thisIdGroup = new ArrayList<Pending>();
				idGroups.add(thisIdGroup);
			}
			// add item into id group
			thisIdGroup.add(p);
			lastItem = p;
		}
		
		return idGroups;
	}

	public boolean updateEntry(Pending pending) {
		return mDatabase.update(DATABASE_TABLE, getValues(pending), KEY_ID + "=" + pending.getRowIndex(), null) > 0;
	}

	private static class DbPendingHelper extends SQLiteOpenHelper {

		public DbPendingHelper(Context context, String name, 
                          CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		// Called when no database exists in disk and the helper class needs
		// to create a new one. 
		@Override
		public void onCreate(SQLiteDatabase _db) {
			_db.execSQL(DATABASE_CREATE_TABLE);
		}

		// Called when there is a database version mismatch meaning that the version
		// of the database on disk needs to be upgraded to the current version.
		@Override
		public void onUpgrade(SQLiteDatabase _db, int _oldVersion, int _newVersion) {
			// Log the version upgrade.
			Log.w("TaskDBAdapter", "Upgrading from version " + 
                             _oldVersion + " to " +
                             _newVersion + ", which will destroy all old data");
        
			// Upgrade the existing database to conform to the new version. Multiple 
			// previous versions can be handled by comparing _oldVersion and _newVersion
			// values.
			
			// The simplest case is to drop the old table and create a new one.
      		_db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
      		
      		// Create a new one.
      		onCreate(_db);
		}
	}
}

