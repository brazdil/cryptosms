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
	private byte[] mDataEncrypted;
	private byte[] mSmsData;
	
	public ConfirmMessage(byte[] key, byte[] nonce) {
		mDataEncrypted =
				LowLevel.wrapData(
					Encryption.getEncryption().encryptSymmetric(nonce, key),
					LENGTH_DATA);

		mSmsData = new byte[MessageData.LENGTH_MESSAGE];
		mSmsData[OFFSET_HEADER] = HEADER_CONFIRM;
		mSmsData[OFFSET_ID] = 0x00;
		mSmsData[OFFSET_INDEX] = 0x00;
		System.arraycopy(mDataEncrypted, 0, mSmsData, OFFSET_DATA, LENGTH_DATA);
 	}

	@Override
	public ArrayList<byte[]> getBytes() throws StorageFileException,
			MessageException, EncryptionException {
		ArrayList<byte[]> result = new ArrayList<byte[]>();
		result.add(mSmsData);
		return result;
	}

	/**
	 * Returns stored encrypted data in the message messages
	 * @param data
	 * @return
	 */
	public static byte[] getEncryptedData(byte[] data) {
		return LowLevel.cutData(data, OFFSET_DATA, Encryption.getEncryption().getSymmetricEncryptedLength(Encryption.SYM_CONFIRM_NONCE_LENGTH));
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
		if (dataDecrypted.length < Encryption.SYM_CONFIRM_NONCE_LENGTH)
			return new ParseResult(idGroup, PendingParseResult.CORRUPTED_DATA, null);
		
		// check that it contains the right bytes
		byte[] nonce = keys.getConfirmationNonce();
		for (int i = 0; i < Encryption.SYM_CONFIRM_NONCE_LENGTH; ++i)
			if (dataDecrypted[i] != nonce[i])
				return new ParseResult(idGroup, PendingParseResult.CORRUPTED_DATA, null);
		
		// if we got this far, everything is fine and the keys are confirmed
		keys.setKeysConfirmed(true);
		return new ParseResult(idGroup, PendingParseResult.OK_CONFIRM_MESSAGE, null);
	}
}
