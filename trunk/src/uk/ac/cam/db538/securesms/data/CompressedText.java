package uk.ac.cam.db538.securesms.data;

import java.util.zip.DataFormatException;

import uk.ac.cam.db538.securesms.Charset;
import uk.ac.cam.db538.securesms.Compression;

public class CompressedText {
	public enum TextCharset {
		ASCII7,
		UNICODE
	}
	
	private TextCharset mCharset;
	private boolean mCompression;
	private byte[] mData;
	private String mString;
	
	private CompressedText() {
	}
	
	public static CompressedText createFromString(String text) {
		CompressedText msg = new CompressedText();
		
		if (Charset.isConvertableToAscii(text)) {
			msg.mCharset = TextCharset.ASCII7;
			
			byte[] dataCompressed = Compression.compressZ(Charset.toAscii8(text)); 

			int lengthAscii7 = Charset.computeLengthInAscii7(text);
			int lengthZlib = dataCompressed.length;
			if (lengthAscii7 <= lengthZlib) {
				msg.mCompression = false;
				msg.mData = Charset.toAscii7(text);
			} else {
				msg.mCompression = true;
				msg.mData = dataCompressed;
			}
		} else {
			msg.mCharset = TextCharset.UNICODE;
			byte[] dataUnicode = Charset.toUnicode(text);
			byte[] dataCompressed = Compression.compressZ(dataUnicode);
			if (dataUnicode.length <= dataCompressed.length) {
				msg.mCompression = false;
				msg.mData = dataUnicode;
			} else {
				msg.mCompression = true;
				msg.mData = dataCompressed;
			}
		}
		
		return msg;
	}
	
	public static CompressedText decode(byte[] data, TextCharset charset, boolean compressed) throws DataFormatException {
		CompressedText msg = new CompressedText();
		msg.mData = data;
		msg.mCharset = charset;
		msg.mCompression = compressed;
		
		if (msg.mCompression)
			data = Compression.decompressZ(data);
		if (msg.mCharset == TextCharset.ASCII7)
			msg.mString = Charset.fromAscii8(data);
		else
			msg.mString = Charset.fromUnicode(data);
		
		return msg;
	}
	
	public int getLength() {
		return mData.length;
	}
	
	public byte[] getData() {
		return mData.clone();
	}
	
	public String getMessage() {
		return mString;
	}
}
