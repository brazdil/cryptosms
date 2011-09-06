package uk.ac.cam.db538.cryptosms.utils;

import java.rmi.server.UnicastRemoteObject;
import java.util.zip.DataFormatException;

import javax.xml.soap.Text;

import uk.ac.cam.db538.cryptosms.utils.Charset;
import uk.ac.cam.db538.cryptosms.utils.Compression;

public class CompressedText {
	public enum TextCharset {
		ASCII,
		UNICODE
	}

	protected static final byte HEADER_ASCII = (byte) 0x01;
	protected static final byte HEADER_COMPRESSED = (byte) 0x02;
	
	private TextCharset mCharset;
	private boolean mCompression;
	private byte[] mData;
	private String mString;
	
	private CompressedText() {
	}
	
	public static CompressedText createFromString(String text) {
		CompressedText msg = new CompressedText();
		msg.mString = text;
		
		if (Charset.isConvertableToAscii(text)) {
			msg.mCharset = TextCharset.ASCII;
			
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
	
	public static CompressedText decode(byte[] data) throws DataFormatException {
		TextCharset charset = ((data[0] & HEADER_ASCII) != 0x00) ? TextCharset.ASCII : TextCharset.UNICODE;
		boolean compressed = ((data[0] & HEADER_COMPRESSED) != 0x00);
		
		CompressedText msg = new CompressedText();
		msg.mData = LowLevel.cutData(data, 1, data.length - 1);
		msg.mCharset = charset;
		msg.mCompression = compressed;
		
		byte[] dataDecompressed = null;
		if (msg.mCompression)
			dataDecompressed = Compression.decompressZ(msg.mData);
		else
			dataDecompressed = msg.mData;
		
		if (msg.mCharset == TextCharset.ASCII)
			msg.mString = (msg.mCompression) ? Charset.fromAscii8(dataDecompressed) : Charset.fromAscii7(dataDecompressed);
		else
			msg.mString = Charset.fromUnicode(dataDecompressed);
		
		return msg;
	}
	
	public int getDataLength() {
		return mData.length + 1;
	}
	
	public byte[] getData() {
		// add header to the front
		byte header = 0x00;
		if (mCharset == TextCharset.ASCII)
			header |= HEADER_ASCII;
		if (mCompression)
			header |= HEADER_COMPRESSED;
		
		byte[] data = new byte[mData.length + 1];
		data[0] = header;
		System.arraycopy(mData, 0, data, 1, mData.length);
		
		return data;
	}
	
	public String getMessage() {
		return mString;
	}
	
	public boolean isCompressed() {
		return mCompression;
	}
	
	public TextCharset getCharset() {
		return mCharset;
	}

	@Override
	public boolean equals(Object o) {
		try {
			CompressedText another = (CompressedText) o;
			return (this.mCharset == another.mCharset && 
					this.mCompression == another.mCompression &&
					this.mString.compareTo(another.mString) == 0);
		} catch (Exception e) {
			return false;
		}
	}
}
