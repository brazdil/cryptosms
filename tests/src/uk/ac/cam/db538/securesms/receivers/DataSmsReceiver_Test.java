package uk.ac.cam.db538.securesms.receivers;

import java.io.File;
import java.io.IOException;

import android.content.Context;

import uk.ac.cam.db538.securesms.MyApplication;
import uk.ac.cam.db538.securesms.data.CompressedText;
import uk.ac.cam.db538.securesms.data.DbPendingAdapter;
import uk.ac.cam.db538.securesms.data.Pending;
import uk.ac.cam.db538.securesms.data.SimCard;
import uk.ac.cam.db538.securesms.data.TextMessage;
import uk.ac.cam.db538.securesms.data.Message.MessageException;
import uk.ac.cam.db538.securesms.storage.Common;
import uk.ac.cam.db538.securesms.storage.Conversation;
import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.SessionKeys;
import uk.ac.cam.db538.securesms.storage.StorageFileException;

import junit.framework.TestCase;

public class DataSmsReceiver_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		
		Common.clearStorageFile();
		// delete database file
		File file = new File("/data/data/uk.ac.cam.db538.securesms/databases/pending.db");
		if (file.exists())
			file.delete();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testCheckPending() throws StorageFileException, IOException, MessageException {
		Context context = MyApplication.getSingleton().getApplicationContext();
		DbPendingAdapter database = new DbPendingAdapter(context);
		database.open();

		// prepare conversation
		Conversation conv = Conversation.createConversation();
		SessionKeys keys = SessionKeys.createSessionKeys(conv);
		keys.setSimNumber(SimCard.getSingleton().getSimNumberWrapped(context));
		
		// generate a single part text message
		TextMessage msg = new TextMessage(MessageData.createMessageData(conv));
		CompressedText text = CompressedText.createFromString("Hi there!");
		msg.setText(text);
		Pending pending = new Pending("+44210987654321", msg.getBytes(context).get(0));
		database.insertEntry(pending);
		
	}

}
