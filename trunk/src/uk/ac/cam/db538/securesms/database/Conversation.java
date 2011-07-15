package uk.ac.cam.db538.securesms.database;

public class Conversation {
	private long mIndexEntry;
	
	Conversation(long indexEntry) {
		setIndexEntry(indexEntry);
	}

	void setIndexEntry(long indexEntry) {
		this.mIndexEntry = indexEntry;
	}

	long getIndexEntry() {
		return mIndexEntry;
	}
	
	
}
