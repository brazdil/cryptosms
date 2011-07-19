package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.IOException;
import android.text.format.Time;
import uk.ac.cam.db538.securesms.database.Message.MessageType;
import junit.framework.TestCase;

public class Message_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();

		// delete the file before each test
		File file = new File(Database_Test.TESTING_FILE);
		if (file.exists())
			file.delete();
		
		// and free the singleton
		Database.freeSingleton();
		Database.initSingleton(Database_Test.TESTING_FILE);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testMessages() {
		Message msg;
		
		// ASSIGNMENT
		
		try {
			msg = new Message(120L);
			assertEquals(msg.getIndexEntry(), 120L);
		} catch (IndexOutOfBoundsException ex) {
			assertTrue(false);
		}
		
		// INDEX OUT OF BOUNDS
		
		try {
			msg = new Message(0x100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			msg = new Message(0L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			msg = new Message(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testSaveUpdate() throws DatabaseFileException, IOException {
		Database database = Database.getSingleton();
		
		boolean deliveredPart = true;
		boolean deliveredAll = true;
		MessageType messageType = MessageType.OUTGOING;
		Time timeStamp = new Time(); timeStamp.set(5, 2, 1928);
		String messageBody = "Testing body";
		
		Conversation conv = database.createConversation("+123456789012");

		Message msgWrite = conv.newMessage(MessageType.INCOMING);
		Message msgRead = new Message(msgWrite.getIndexEntry());
		
		msgWrite.setDeliveredPart(deliveredPart);
		msgWrite.setDeliveredAll(deliveredAll);
		msgWrite.setMessageType(messageType);
		msgWrite.setTimeStamp(timeStamp);
		msgWrite.setMessageBody(messageBody);
		msgWrite.save();
		
		msgRead.update();
		assertEquals(deliveredPart, msgRead.getDeliveredPart());
		assertEquals(deliveredAll, msgRead.getDeliveredAll());
		assertEquals(messageType, msgRead.getMessageType());
		assertEquals(0, Time.compare(timeStamp, msgRead.getTimeStamp()));
		assertEquals(messageBody, msgRead.getMessageBody());
	}
}
