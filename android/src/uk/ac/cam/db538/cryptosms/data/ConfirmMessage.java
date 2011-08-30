package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;

import android.util.Log;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageType;
import uk.ac.cam.db538.cryptosms.data.PendingParser.ParseResult;
import uk.ac.cam.db538.cryptosms.data.PendingParser.PendingParseResult;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
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

	/**
	 * Returns stored encrypted data in the message messages
	 * @param data
	 * @return
	 */
	public static byte[] getEncryptedData(byte[] data) {
		return LowLevel.cutData(data, OFFSET_DATA, Encryption.getEncryption().getSymmetricEncryptedLength(MESSAGE.length));
	}

	/**
	 * Parses a pending confirm message
	 * @param idGroup
	 * @return
	 */
	public static ParseResult parseConfirmMessage(ArrayList<Pending> idGroup) {
		// check that there is only one part
		int groupSize = idGroup.size();
		if (groupSize > 1)
			return new ParseResult(idGroup, PendingParseResult.REDUNDANT_PARTS, null);
		else if (groupSize < 1)
			return new ParseResult(idGroup, PendingParseResult.MISSING_PARTS, null);
		
		// check the sender
		String sender = idGroup.get(0).getSender();
		Contact contact = Contact.getContact(MyApplication.getSingleton().getApplicationContext(), sender);
		if (!contact.existsInDatabase())
			return new ParseResult(idGroup, PendingParseResult.UNKNOWN_SENDER, null);
		
		// check that we have session keys for this person
		Conversation conv = null;
		SessionKeys keys = null;
		try {
			conv = Conversation.getConversation(sender);
			if (conv != null)
				keys = conv.getSessionKeys(SimCard.getSingleton().getNumber());
		} catch (StorageFileException ex) {
			return new ParseResult(idGroup, PendingParseResult.INTERNAL_ERROR, null);
		}
		if (conv == null || keys == null)
			return new ParseResult(idGroup, PendingParseResult.NO_SESSION_KEYS, null);
		
		// check that we're waiting for the confirmation
		if (!keys.getKeysSent() || keys.getKeysConfirmed())		
			return new ParseResult(idGroup, PendingParseResult.NO_SESSION_KEYS, null);
		
		// get the data
		byte[] dataEncrypted = ConfirmMessage.getEncryptedData(idGroup.get(0).getData());
		
		// try to decrypt
		byte[] dataDecrypted = null;
		try {
			Log.d(MyApplication.APP_TAG, "Decrypting with " + LowLevel.toHex(keys.getSessionKey_In()));
			dataDecrypted = Encryption.getEncryption().decryptSymmetric(dataEncrypted, keys.getSessionKey_In());
			// if didn't throw an exception, increment the key
			keys.incrementIn(1);
			keys.saveToFile();
		} catch (EncryptionException e) {
			return new ParseResult(idGroup, PendingParseResult.COULD_NOT_DECRYPT, null);
		} catch (WrongKeyDecryptionException e) {
			return new ParseResult(idGroup, PendingParseResult.COULD_NOT_DECRYPT, null);
		} catch (StorageFileException e) {
			return new ParseResult(idGroup, PendingParseResult.INTERNAL_ERROR, null);
		}
		
		// check that it's long enough
		if (dataDecrypted.length < ConfirmMessage.MESSAGE.length)
			return new ParseResult(idGroup, PendingParseResult.CORRUPTED_DATA, null);
		
		// check that it contains the right bytes
		for (int i = 0; i < ConfirmMessage.MESSAGE.length; ++i)
			if (dataDecrypted[i] != ConfirmMessage.MESSAGE[i])
				return new ParseResult(idGroup, PendingParseResult.CORRUPTED_DATA, null);
		
		// if we got this far, everything is fine and the keys are confirmed
		keys.setKeysConfirmed(true);
		return new ParseResult(idGroup, PendingParseResult.OK_CONFIRM_MESSAGE, null);
	}
}
