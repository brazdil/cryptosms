package uk.ac.cam.db538.securesms.data;

import java.util.zip.DataFormatException;

import uk.ac.cam.db538.securesms.Charset;
import uk.ac.cam.db538.securesms.Compression;
import uk.ac.cam.db538.securesms.storage.Message.MessageCharset;

public class MessageData {
	private MessageCharset mCharset;
	private boolean mCompression;
	private byte[] mData;
	private String mString;
	
	public MessageData(String text) {
		if (Charset.isConvertableToAscii(text)) {
			this.mCharset = MessageCharset.ASCII7;
			
			byte[] dataCompressed = Compression.compressZ(Charset.toAscii8(text)); 

			int lengthAscii7 = Charset.computeLengthInAscii7(text);
			int lengthZlib = dataCompressed.length;
			if (lengthAscii7 <= lengthZlib) {
				this.mCompression = false;
				this.mData = Charset.toAscii7(text);
			} else {
				this.mCompression = true;
				this.mData = dataCompressed;
			}
		} else {
			this.mCharset = MessageCharset.UNICODE;
			byte[] dataUnicode = Charset.toUnicode(text);
			byte[] dataCompressed = Compression.compressZ(dataUnicode);
			if (dataUnicode.length <= dataCompressed.length) {
				this.mCompression = false;
				this.mData = dataUnicode;
			} else {
				this.mCompression = true;
				this.mData = dataCompressed;
			}
		}
	}
	
	public MessageData(byte[] data, MessageCharset charset, boolean compressed) throws DataFormatException {
		this.mData = data;
		this.mCharset = charset;
		this.mCompression = compressed;
		
		if (mCompression)
			data = Compression.decompressZ(data);
		if (mCharset == MessageCharset.ASCII7)
			mString = Charset.fromAscii8(data);
		else
			mString = Charset.fromUnicode(data);
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
