package uk.ac.cam.db538.securesms.database;

import java.io.File;
import java.io.IOException;

import android.text.format.Time;

import uk.ac.cam.db538.securesms.CustomAsserts;
import uk.ac.cam.db538.securesms.database.Message.MessageType;
import uk.ac.cam.db538.securesms.encryption.Encryption;

import junit.framework.TestCase;

public class MessagePart_Test extends TestCase {

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
		MessagePart msgPart;
		
		// ASSIGNMENT
		
		try {
			msgPart = new MessagePart(120L);
			assertEquals(msgPart.getIndexEntry(), 120L);
		} catch (IndexOutOfBoundsException ex) {
			assertTrue(false);
		}
		
		// INDEX OUT OF BOUNDS
		
		try {
			msgPart = new MessagePart(0x100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			msgPart = new MessagePart(0L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			msgPart = new MessagePart(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testSaveUpdate() throws DatabaseFileException, IOException {
		Database database = Database.getSingleton();
		
		boolean deliveredPart = true;
		String messageBody = "Testing body";
		
		Conversation conv = database.createConversation("+123456789012");
		Message msg = conv.newMessage(MessageType.OUTGOING);

		MessagePart msgPartWrite = msg.newMessagePart();
		MessagePart msgPartRead = new MessagePart(msgPartWrite.getIndexEntry());
		
		msgPartWrite.setDeliveredPart(deliveredPart);
		msgPartWrite.setMessageBody(messageBody);
		msgPartWrite.save();
		
		msgPartRead.update();
		assertEquals(deliveredPart, msgPartRead.getDeliveredPart());
		assertEquals(messageBody, msgPartRead.getMessageBody());
	}
}
