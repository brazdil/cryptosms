package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public class ConfirmMessage extends Message {
	
	private static final int OFFSET_DATA = OFFSET_HEADER + LENGTH_HEADER;
	public static final int LENGTH_DATA = MessageData.LENGTH_MESSAGE - OFFSET_DATA;
	
	public static final byte[] MESSAGE = LowLevel.fromHex("FACECABBAGEB00BAGE");
	
	private byte[] mDataEncrypted;
	
	public ConfirmMessage(byte[] key) {
		mDataEncrypted =
			LowLevel.wrapData(
				Encryption.getEncryption().encryptSymmetric(MESSAGE, key),
				LENGTH_DATA);
	}

	@Override
	public ArrayList<byte[]> getBytes() throws StorageFileException,
			MessageException, EncryptionException {
		byte[] smsData = new byte[MessageData.LENGTH_MESSAGE];
		smsData[OFFSET_HEADER] = HEADER_CONFIRM;
		System.arraycopy(mDataEncrypted, 0, smsData, OFFSET_DATA, LENGTH_DATA);
		
		ArrayList<byte[]> result = new ArrayList<byte[]>();
		result.add(smsData);
		return result;
	}

}
