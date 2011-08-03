package uk.ac.cam.db538.securesms.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.zip.DataFormatException;

import android.content.Context;
import android.test.mock.MockContext;

import junit.framework.TestCase;
import uk.ac.cam.db538.securesms.CustomAsserts;
import uk.ac.cam.db538.securesms.data.Message.MessageException;
import uk.ac.cam.db538.securesms.storage.Common;
import uk.ac.cam.db538.securesms.storage.Conversation;
import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.SessionKeys;
import uk.ac.cam.db538.securesms.storage.StorageFileException;
import uk.ac.cam.db538.securesms.storage.SessionKeys.SimNumber;

public class TextMessage_Test  extends TestCase {
	
	protected void setUp() throws Exception {
		super.setUp();
		Common.clearStorageFile();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	private final CompressedText textLong = CompressedText.createFromString("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nisl nisl, ullamcorper eu fermentum ac, eleifend quis orci. Pellentesque ut nulla erat, ac imperdiet tellus. Morbi vulputate mauris in nibh ornare imperdiet. Duis venenatis ligula quis mi vestibulum posuere. Etiam quis felis non justo dignissim viverra. Vivamus dapibus risus id risus sodales tincidunt eu vel orci. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus sed purus sed odio aliquam gravida vestibulum vitae augue. Nullam neque elit, dapibus eu tincidunt eget, accumsan eu mi. Duis eget sagittis nisi. Aenean consectetur est nisl, non molestie lorem. Sed tincidunt turpis sit amet augue convallis mollis ut vehicula nisi. Morbi lobortis interdum mattis. Donec tincidunt suscipit vehicula. Praesent pharetra molestie arcu. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Aenean facilisis auctor ipsum, id bibendum sapien molestie sed. Donec risus enim, rhoncus vel ultrices a, lobortis non mauris. Aenean sit amet magna a dolor sollicitudin lacinia sit amet eu sapien. Sed semper pretium varius.");
	private final CompressedText textShort = CompressedText.createFromString("I'll be there at 5pm. Promise...");
	private final byte headerShort = (byte) 0xA0;

	public void testTextMessage() throws StorageFileException, IOException, DataFormatException {
		Conversation conv = Conversation.createConversation();
		TextMessage msg = new TextMessage(MessageData.createMessageData(conv));
		
		// this should work
		try {
			msg.setText(textLong);
		} catch (MessageException e) {
			fail("Should work");
		}
		CompressedText textDecompressed = CompressedText.decode(textLong.getData(), textLong.getCharset(), textLong.isCompressed());
		assertEquals(textDecompressed, textLong);
		CustomAsserts.assertArrayEquals(msg.getStoredData(), textLong.getData());
		CompressedText textRead = msg.getText();
		assertEquals(textRead, textLong);
		
		// too long message
		// EXTREME!!!
		/*
		Random random = new Random();
		CompressedText tooLong;
		String str = "";
		while ((tooLong = CompressedText.createFromString(str)).getLength() <= 34870)
			if (tooLong.getLength() < 28000)
				for (int i = 0; i < 2000; ++i)
					str += (char) random.nextInt(40000);
			else
				str += (char) random.nextInt(40000);
		
		try {
			msg.setText(tooLong);
			fail("Should have thrown an exception");
		} catch (MessageException e) {
		}*/
	}
	
	public void testSMSData() throws StorageFileException, IOException, MessageException {
		Context context = new MockContext();
		Conversation conv = Conversation.createConversation();
		TextMessage msg = new TextMessage(MessageData.createMessageData(conv));
		msg.setText(textShort);
		
		try {
			msg.getSMSData(context);
			fail("Should have thrown exception");
		} catch (MessageException e) {
		}

		// generate keys
		SessionKeys keys = SessionKeys.createSessionKeys(conv);
		keys.setSimNumber(new SimNumber("+441234567890", false));
		// get the data
		ArrayList<byte[]> data = msg.getSMSData(context);
		assertEquals(data.size(), 1);
		assertEquals(data.get(0)[0], headerShort);
		//TODO: doesn't work!!!
	}
}
