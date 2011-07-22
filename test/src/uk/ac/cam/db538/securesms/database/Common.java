package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.IOException;

import uk.ac.cam.db538.securesms.database.Database;

public class Common {
	public static final String TESTING_FILE = "/data/data/uk.ac.cam.db538.securesms/files/testing.db";

	public static void clearFile() throws IOException, DatabaseFileException {
		File file = new File(TESTING_FILE);
		if (file.exists())
			file.delete();
		
		// clear caches
		Header.forceClearCache();
		Empty.forceClearCache();
		Conversation.forceClearCache();
		SessionKeys.forceClearCache();
		Message.forceClearCache();
		MessagePart.forceClearCache();
		
		// free the singleton
		Database.freeSingleton();
		Database.initSingleton(TESTING_FILE);
	}

	public static boolean checkStructure() throws DatabaseFileException, IOException {
		boolean visitedAll = true;
		boolean corruptedPointers = false;
		boolean multiplePointers = false;
		
		// clear caches
		Header.forceClearCache();
		Empty.forceClearCache();
		Conversation.forceClearCache();
		SessionKeys.forceClearCache();
		Message.forceClearCache();
		MessagePart.forceClearCache();
		
		// initialize
		Database db = Database.getDatabase();
		int countEntries = (int) db.getEntriesCount();
		boolean[] visitedEntries = new boolean[countEntries];
		for (int i = 0; i < countEntries; ++i)
			visitedEntries[i] = false;
		
		// header
		Header header = Header.getHeader();
		visitedEntries[0] = true;
		
		// empty
		Empty empty = header.getFirstEmpty();
		while (empty != null) {
			if (visitedEntries[(int) empty.getEntryIndex()])
				multiplePointers = true;
			visitedEntries[(int) empty.getEntryIndex()] = true;
			empty = empty.getNextEmpty();
		}
		
		// conversation
		Conversation conv = header.getFirstConversation();
		long convPrevious = 0L;
		while (conv != null) {
			if (visitedEntries[(int) conv.getEntryIndex()])
				multiplePointers = true;
			visitedEntries[(int) conv.getEntryIndex()] = true;
			
			// pointers
			if (conv.getIndexPrev() != convPrevious)
				corruptedPointers = true;
			
			// session keys
			SessionKeys keys = conv.getFirstSessionKeys();
			long keysPrevious = 0L;
			while (keys != null) {
				if (visitedEntries[(int) keys.getEntryIndex()])
					multiplePointers = true;
				visitedEntries[(int) keys.getEntryIndex()] = true;
				
				// pointers
				if (keys.getIndexPrev() != keysPrevious)
					corruptedPointers = true;
				if (keys.getIndexParent() != conv.getEntryIndex())
					corruptedPointers = true;
				
				// parent
				if (keys.getIndexParent() != conv.getEntryIndex())
					corruptedPointers = true;

				// next
				keysPrevious = keys.getEntryIndex();
				keys = keys.getNextSessionKeys();
			}
			
			// message
			Message msg = conv.getFirstMessage();
			long msgPrevious = 0L;
			while (msg != null) {
				if (visitedEntries[(int) msg.getEntryIndex()])
					multiplePointers = true;
				visitedEntries[(int) msg.getEntryIndex()] = true;
				
				// pointers
				if (msg.getIndexPrev() != msgPrevious)
					corruptedPointers = true;
				if (msg.getIndexParent() != conv.getEntryIndex())
					corruptedPointers = true;
				
				// parent
				if (msg.getIndexParent() != conv.getEntryIndex())
					corruptedPointers = true;
				
				// message parts
				MessagePart part = msg.getFirstMessagePart();
				long partPrevious = 0L;
				while (part != null) {
					if (visitedEntries[(int) part.getEntryIndex()])
						multiplePointers = true;
					visitedEntries[(int) part.getEntryIndex()] = true;
					
					// pointers
					if (part.getIndexPrev() != partPrevious)
						corruptedPointers = true;

					// parent
					if (part.getIndexParent() != msg.getEntryIndex())
						corruptedPointers = true;

					partPrevious = part.getEntryIndex();
					part = part.getNextMessagePart();
				}
				
				// next
				msgPrevious = msg.getEntryIndex();
				msg = msg.getNextMessage();
			}

			// next
			convPrevious = conv.getEntryIndex();
			conv = conv.getNextConversation();
		}
		
		for (boolean b : visitedEntries)
			visitedAll = visitedAll && b;
		
		return (visitedAll && !corruptedPointers && !multiplePointers);
	}
}
