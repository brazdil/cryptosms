package uk.ac.cam.db538.securesms.database;

import uk.ac.cam.db538.securesms.R;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.text.format.Time;
import android.util.Log;

public class SMSHistoryAdapter {
	private static final String DATABASE_NAME = "SecureSMS.db";
	private static final String DATABASE_TABLE = "history";
	private static final int DATABASE_VERSION = 3;
	
	public static final String KEY_ID = "_id";
	public static final String KEY_PHONENUMBER = "phone_number";
	public static final int COLUMN_PHONENUMBER = 1;
	public static final String KEY_MSGBODY = "message_body";
	public static final int COLUMN_MSGBODY = 2;
	public static final String KEY_CREATIONTIME = "creation_time";
	public static final int COLUMN_CREATIONTIME = 3;
	
	private static final String[] ALL_COLUMNS = new String[] { KEY_ID, KEY_PHONENUMBER, KEY_MSGBODY, KEY_CREATIONTIME };	
	private static final String DATABASE_CREATE = "CREATE TABLE " + DATABASE_TABLE + " (" + KEY_ID + " integer primary key autoincrement, " + KEY_PHONENUMBER + " text not null, " + KEY_MSGBODY + " text not null, " + KEY_CREATIONTIME + " text not null);";
	
	private static class DbHelper extends SQLiteOpenHelper {
		public DbHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// CHANGE IF NECESSARY TO INCREASE THE VERSION OF DATABASE
			Log.w("SMSHistoryHelper", "Upgrading from version " + oldVersion + " to " + newVersion + " which will destroy all data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}
	}
	
	private SQLiteDatabase mDatabase;
	private final Context mContext;
	private DbHelper mHelper;
	
	public SMSHistoryAdapter(Context context) {
		this.mContext = context;
		this.mHelper = new DbHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public SMSHistoryAdapter open() throws SQLException {
		mDatabase = mHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		mDatabase.close();
	}
	
	public SMSHistoryEntry insertEntry(SMSHistoryEntry entry) throws DatabaseException {
		ContentValues newEntry = new ContentValues();
		newEntry.put(KEY_PHONENUMBER, entry.getPhoneNumber());
		newEntry.put(KEY_MSGBODY, entry.getMessageBody());
		newEntry.put(KEY_CREATIONTIME, entry.getCreationTime().format3339(false));
		long res;
		try {
			res = mDatabase.insert(DATABASE_TABLE, null, newEntry);
		}
		catch (Exception ex) {
			res = -1;
		}
		if (res < 0) {
			Resources r = this.mContext.getResources();
			throw new DatabaseException(r.getString(R.string.database_exception_insert));
		}
		entry.setIndex(res);
		return entry;
	}
	
	public boolean removeEntry(long index) {
		return mDatabase.delete(DATABASE_TABLE, KEY_ID + "=" + index, null) > 0;
	}
	
	public boolean removeEntry(SMSHistoryEntry entry) {
		return removeEntry(entry.getIndex());
	}
	
	public Cursor getAllEntries() {
		return mDatabase.query(DATABASE_TABLE, new String[] {KEY_ID, KEY_PHONENUMBER, KEY_MSGBODY}, null, null, null, null, null);
	}
	
	public SMSHistoryEntry getEntry(long index) {
		Cursor query = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_ID + "=" + index, null, null, null, null, "1");
		Time time = new Time();
		time.parse3339(query.getString(COLUMN_MSGBODY));
		return new SMSHistoryEntry(index, 
		                           query.getString(COLUMN_PHONENUMBER), 
		                           query.getString(COLUMN_MSGBODY), 
		                           time
		                           );
	}
}
