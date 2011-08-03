package uk.ac.cam.db538.securesms.data;

import java.io.IOException;
import java.util.Random;
import java.util.zip.DataFormatException;

import junit.framework.TestCase;
import uk.ac.cam.db538.securesms.CustomAsserts;
import uk.ac.cam.db538.securesms.data.CompressedText.TextCharset;
import uk.ac.cam.db538.securesms.data.Message.MessageException;
import uk.ac.cam.db538.securesms.encryption.Encryption;
import uk.ac.cam.db538.securesms.storage.Common;
import uk.ac.cam.db538.securesms.storage.Conversation;
import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.StorageFileException;

public class TextMessage_Test  extends TestCase {
	
	protected void setUp() throws Exception {
		super.setUp();
		Common.clearStorageFile();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	private CompressedText text = CompressedText.createFromString("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nisl nisl, ullamcorper eu fermentum ac, eleifend quis orci. Pellentesque ut nulla erat, ac imperdiet tellus. Morbi vulputate mauris in nibh ornare imperdiet. Duis venenatis ligula quis mi vestibulum posuere. Etiam quis felis non justo dignissim viverra. Vivamus dapibus risus id risus sodales tincidunt eu vel orci. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus sed purus sed odio aliquam gravida vestibulum vitae augue. Nullam neque elit, dapibus eu tincidunt eget, accumsan eu mi. Duis eget sagittis nisi. Aenean consectetur est nisl, non molestie lorem. Sed tincidunt turpis sit amet augue convallis mollis ut vehicula nisi. Morbi lobortis interdum mattis. Donec tincidunt suscipit vehicula. Praesent pharetra molestie arcu. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Aenean facilisis auctor ipsum, id bibendum sapien molestie sed. Donec risus enim, rhoncus vel ultrices a, lobortis non mauris. Aenean sit amet magna a dolor sollicitudin lacinia sit amet eu sapien. Sed semper pretium varius.");

	public void testTextMessage() throws StorageFileException, IOException, DataFormatException {
		Conversation conv = Conversation.createConversation();
		TextMessage msg = new TextMessage(MessageData.createMessageData(conv));
		
		// this should work
		try {
			msg.setText(text);
		} catch (MessageException e) {
			fail("Should work");
		}
		CompressedText textDecompressed = CompressedText.decode(text.getData(), text.getCharset(), text.isCompressed());
		assertEquals(textDecompressed, text);
		CustomAsserts.assertArrayEquals(msg.getStoredData(), text.getData());
		CompressedText textRead = msg.getText();
		assertEquals(textRead, text);
		
		// too long message
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
		}
	}
}
