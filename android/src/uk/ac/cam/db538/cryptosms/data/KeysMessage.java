package uk.ac.cam.db538.cryptosms.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.util.Log;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageType;
import uk.ac.cam.db538.cryptosms.data.PendingParser.ParseResult;
import uk.ac.cam.db538.cryptosms.data.PendingParser.PendingParseResult;
import uk.ac.cam.db538.cryptosms.storage.Header;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public class KeysMessage extends Message {
	public static final int LENGTH_KEYS = 2 * Encryption.SYM_KEY_LENGTH;
	
	private byte[] mDataEncryptedAndSigned;
	private byte[] mKeyOut, mKeyIn;
	private byte mId;
	
	public KeysMessage(byte[] keyOut, byte[] keyIn) {
		mKeyOut = keyOut;
		mKeyIn = keyIn;
	}
	
	public KeysMessage(long contactId, String contactKey) throws StorageFileException, EncryptionException {
		mKeyOut = Encryption.getEncryption().generateRandomData(Encryption.SYM_KEY_LENGTH);
		mKeyIn = Encryption.getEncryption().generateRandomData(Encryption.SYM_KEY_LENGTH);
		
		// generate a pair of keys 
		byte[] data = new byte[LENGTH_KEYS];
		System.arraycopy(mKeyOut, 0, data, 0, Encryption.SYM_KEY_LENGTH);
		System.arraycopy(mKeyIn, 0, data, Encryption.SYM_KEY_LENGTH, Encryption.SYM_KEY_LENGTH);
		
		// encrypt and sign
		mDataEncryptedAndSigned = 
			Encryption.getEncryption().sign(
				Encryption.getEncryption().encryptAsymmetric(data, contactId, contactKey)
			);
		
		// get an ID for this keys
		mId = Header.getHeader().incrementKeyId();
		Header.getHeader().saveToFile();
	}
	
	public byte[] getKeyOut() {
		return mKeyOut;
	}
	
	public byte[] getKeyIn() {
		return mKeyIn;
	}

	/**
	 * Returns data ready to be sent via SMS
	 * @return
	 * @throws IOException 
	 * @throws StorageFileException 
	 * @throws MessageException 
	 */
	@Override
	public ArrayList<byte[]> getBytes() throws StorageFileException, MessageException {
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		ByteBuffer buf = ByteBuffer.allocate(MessageData.LENGTH_MESSAGE);
		
		Log.d(MyApplication.APP_TAG, "Keys data: " + LowLevel.toHex(mDataEncryptedAndSigned));
		
		// align to fit data messages exactly
		int alignedLength = mDataEncryptedAndSigned.length;
		int totalBytes = 0;
		do {
			totalBytes += LENGTH_DATA;
		} while (totalBytes <= alignedLength);
		mDataEncryptedAndSigned = LowLevel.wrapData(mDataEncryptedAndSigned, totalBytes);

		// create the message parts
		int index = 0, offset = 0;
		try {
			while (true) {
				buf = ByteBuffer.allocate(MessageData.LENGTH_MESSAGE);
				buf.put(HEADER_KEYS);
				buf.put(mId);
				buf.put(LowLevel.getBytesUnsignedByte(index++));
				buf.put(LowLevel.cutData(mDataEncryptedAndSigned, offset, LENGTH_DATA));
				offset += LENGTH_DATA;
				list.add(buf.array());
			}
		} catch (IndexOutOfBoundsException e) {
			// end
		}
		
		return list;
	}
	
	public static int getEncryptedDataLength() {
		int dataLength = LENGTH_KEYS;
		dataLength = Encryption.getEncryption().getAsymmetricAlignedLength(dataLength);
		dataLength += Encryption.MAC_LENGTH;
		dataLength += Encryption.ASYM_SIGNATURE_LENGTH;
		return dataLength;
	}
	
	/**
	 * Returns the number of messages necessary to send given number of bytes
	 * @return
	 */
	public static int getPartsCount() {
		int dataLength = getEncryptedDataLength();
		int count = 0; 
		int remains = dataLength;
		do {
			count++;
			remains -= LENGTH_DATA;
		} while (remains > 0);
		
		if (count > 255)
			return 255;
		return count;
	}
	
	public static int getTotalDataLength() {
		return getDataPartOffset(getPartsCount());
	}

	public static ParseResult parseKeysMessage(ArrayList<Pending> idGroup) {
		// check the sender
		Contact contact = Contact.getContact(MyApplication.getSingleton().getApplicationContext(), idGroup.get(0).getSender());
		if (!contact.existsInDatabase())
			return new ParseResult(idGroup, PendingParseResult.UNKNOWN_SENDER, null);

		// check we have all the parts
		// there shouldn't be more than 6 of them
		int groupSize = idGroup.size();
		int expectedGroupSize = KeysMessage.getPartsCount(); 
		if (groupSize < expectedGroupSize || groupSize <= 0)
			return new ParseResult(idGroup, PendingParseResult.MISSING_PARTS, null);
		else if (groupSize > expectedGroupSize)
			return new ParseResult(idGroup, PendingParseResult.REDUNDANT_PARTS, null);
		
		// get the data
		byte[][] dataParts = new byte[groupSize][];
		int filledParts = 0;
		for (Pending p : idGroup) {
			byte[] dataPart = p.getData();
			int index = KeysMessage.getMessageIndex(dataPart);
			if (index >= 0 && index < idGroup.size()) {
				// index is fine, check that there wasn't the same one already
				if (dataParts[index] == null) {
					// first time we stumbled upon this index
					// store the message part data in the array
					dataParts[index] = dataPart;
					filledParts++;
				} else
					// more parts of the same index
					return new ParseResult(idGroup, PendingParseResult.REDUNDANT_PARTS, null);
			} else
				// index is bigger than the number of messages in ID group
				// therefore some parts have to be missing or the data is corrupted
				return new ParseResult(idGroup, PendingParseResult.MISSING_PARTS, null);
		}
		// the array was filled with data, so check that there aren't any missing
		if (filledParts != expectedGroupSize)
			return new ParseResult(idGroup, PendingParseResult.MISSING_PARTS, null);
		
		// lets put the data together
		byte[] dataEncryptedSigned = new byte[KeysMessage.getTotalDataLength()];
		for (int i = 0; i < expectedGroupSize; ++i) {
			try {
				// get the data 
				// it can't be too long, thanks to getMessageData
				// but it can be too short (throws IndexOutOfBounds exception
				byte[] relevantData = KeysMessage.getMessageData(dataParts[i]);
				System.arraycopy(relevantData, 0, dataEncryptedSigned, KeysMessage.getDataPartOffset(i), Message.LENGTH_DATA);
			} catch (RuntimeException e) {
				return new ParseResult(idGroup, PendingParseResult.CORRUPTED_DATA, null);
			}
		}
		
		// cut out the rubbish part at the end
		dataEncryptedSigned = LowLevel.cutData(dataEncryptedSigned, 0, KeysMessage.getEncryptedDataLength());
		
		// check the signature
		byte[] dataEncrypted = null;
		try {
			 dataEncrypted = Encryption.getEncryption().verify(dataEncryptedSigned, contact.getId());
		} catch (EncryptionException e) {
			return new ParseResult(idGroup, PendingParseResult.COULD_NOT_VERIFY, null);
		} catch (WrongKeyDecryptionException e) {
			return new ParseResult(idGroup, PendingParseResult.COULD_NOT_VERIFY, null);
		}
		
		// now decrypt the data
		byte[] dataDecrypted = null;
		try {
			dataDecrypted = Encryption.getEncryption().decryptAsymmetric(dataEncrypted);
		} catch (EncryptionException e) {
			return new ParseResult(idGroup, PendingParseResult.COULD_NOT_DECRYPT, null);
		} catch (WrongKeyDecryptionException e) {
			return new ParseResult(idGroup, PendingParseResult.COULD_NOT_DECRYPT, null);
		}
		
		// check the length
		if (dataDecrypted.length != KeysMessage.LENGTH_KEYS)
			return new ParseResult(idGroup, PendingParseResult.CORRUPTED_DATA, null);
		
		// all seems to be fine, so just retrieve the keys and return the result
		return new ParseResult(idGroup, 
		                            PendingParseResult.OK_KEYS_MESSAGE, 
		                            new KeysMessage(
		                            	// we have to swap the keys
		                            	// the other guy's out-key is my in-key...
		                            	LowLevel.cutData(dataDecrypted, Encryption.SYM_KEY_LENGTH, Encryption.SYM_KEY_LENGTH),
		                            	LowLevel.cutData(dataDecrypted, 0, Encryption.SYM_KEY_LENGTH)
		                            ));
	}
}
