package uk.ac.cam.db538.cryptosms.data;

import java.security.MessageDigest;
import java.util.ArrayList;

import com.google.inject.internal.Join.JoinException;

import android.util.Log;
import android.widget.DialerFilter;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.EllipticCurveDeffieHellman;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.data.PendingParser.ParseResult;
import uk.ac.cam.db538.cryptosms.data.PendingParser.PendingParseResult;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.Header;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

public class KeysMessage extends Message {
	
	public static final int OFFSET_PUBLIC_KEY = 0;
	public static final int OFFSET_SIGNATURE = EllipticCurveDeffieHellman.LENGTH_PUBLIC_KEY;
	public static final int LENGTH_CONTENT = OFFSET_SIGNATURE + Encryption.ASYM_SIGNATURE_LENGTH;
	
	private byte[] mPublicKey;
	private byte[] mPrivateKey;
	private byte mId;

	private byte[] mOtherPublicKey;
	private byte mOtherId;
	
	private boolean mIsConfirmation;
	
	EllipticCurveDeffieHellman mECDH;
	
	public KeysMessage() throws StorageFileException {
		mIsConfirmation = false;
		
		// get an ID for these keys
		mId = Header.getHeader().incrementKeyId();
		Header.getHeader().saveToFile();
		
		mECDH = new EllipticCurveDeffieHellman();
		mPublicKey = mECDH.getPublicKey();
		mPrivateKey = mECDH.getPrivateKey();
	}
	
	public KeysMessage(byte originalId, byte[] privateKey, byte otherId, byte[] otherPublicKey) {
		mIsConfirmation = false;
		
		mId = originalId;
		mECDH = new EllipticCurveDeffieHellman(privateKey);
		mPublicKey = mECDH.getPublicKey();
		mPrivateKey = mECDH.getPrivateKey();
		
		mOtherId = otherId;
		mOtherPublicKey = otherPublicKey;
	}
	
	public KeysMessage(byte otherId, byte[] otherPublicKey) throws StorageFileException {
		mIsConfirmation = true;
		
		// get an ID for these keys
		mId = Header.getHeader().incrementKeyId();
		Header.getHeader().saveToFile();
		
		mECDH = new EllipticCurveDeffieHellman();
		mPublicKey = mECDH.getPublicKey();
		mPrivateKey = mECDH.getPrivateKey();

		mOtherId = otherId;
		mOtherPublicKey = otherPublicKey;
	}
	
	public byte[] getPublicKey() {
		return mPublicKey;
	}
	
	public byte[] getPrivateKey() {
		return mPrivateKey;
	}
	
	private byte[] getKey(String prefix) {
		return Encryption.getEncryption().getHash(
				(prefix + mECDH.getSharedKey(mOtherPublicKey).toString()).getBytes() 
			);
	}
	
	public byte[] getKeyOut() {
		return getKey(mIsConfirmation ? "0" : "1");
	}
	
	public byte[] getKeyIn() {
		return getKey(mIsConfirmation ? "1" : "0");
	}
	
	public boolean isConfirmation() {
		return mIsConfirmation;
	}

	/**
	 * Returns data ready to be sent via SMS
	 * @return
	 * @throws StorageFileException 
	 * @throws MessageException 
	 * @throws EncryptionException 
	 */
	@Override
	public byte[] getBytes() throws StorageFileException, MessageException, EncryptionException {
		MessageDigest hashing = Encryption.getEncryption().getHashingFunction();
		Log.d(MyApplication.APP_TAG, "KEYS MESSAGE");
		if (mIsConfirmation) {
			hashing.update(getOtherHeader());
			Log.d(MyApplication.APP_TAG, "Other header: " + getOtherHeader());
			hashing.update(mOtherId);
			Log.d(MyApplication.APP_TAG, "Other id: " + mOtherId);
			hashing.update(mOtherPublicKey);
			Log.d(MyApplication.APP_TAG, "Other public key: " + LowLevel.toHex(mOtherPublicKey));
		}
		hashing.update(getHeader());
		Log.d(MyApplication.APP_TAG, "Header: " + getHeader());
		hashing.update(mId);
		Log.d(MyApplication.APP_TAG, "Id: " + mId);
		hashing.update(mPublicKey);
		Log.d(MyApplication.APP_TAG, "Public key: " + LowLevel.toHex(mPublicKey));

		byte[] hash = hashing.digest();
		Log.d(MyApplication.APP_TAG, "Hash: " + LowLevel.toHex(hash));

		byte[] signature = Encryption.getEncryption().sign(hash);
		Log.d(MyApplication.APP_TAG, "Signature: " + LowLevel.toHex(signature));
		
		byte[] data = new byte[LENGTH_CONTENT];
		System.arraycopy(mPublicKey, 0, data, 0, EllipticCurveDeffieHellman.LENGTH_PUBLIC_KEY);
		System.arraycopy(signature, 0, data, OFFSET_SIGNATURE, Encryption.ASYM_SIGNATURE_LENGTH);
		return data;
	}
	
	public static ParseResult parseKeysMessage(ArrayList<Pending> idGroup) {
		try {
			// check the sender
			Contact contact = Contact.getContact(MyApplication.getSingleton().getApplicationContext(), idGroup.get(0).getSender());
			if (!contact.existsInDatabase())
				return new ParseResult(idGroup, PendingParseResult.UNKNOWN_SENDER, null);
	
			byte[] dataJoined = null;
			try {
				dataJoined = joinParts(idGroup, getPartsCount());
			} catch (JoiningException ex) {
				return new ParseResult(idGroup, ex.getReason(), null);
			}
			
			String sender = idGroup.get(0).getSender();
			byte[] dataFirst = idGroup.get(0).getData();
			byte header = getMessageHeader(dataFirst);
			byte id = getMessageIdByte(dataFirst);
			MessageType type = getMessageType(dataFirst);
	
			byte[] publicKey = LowLevel.cutData(dataJoined, OFFSET_PUBLIC_KEY, EllipticCurveDeffieHellman.LENGTH_PUBLIC_KEY);
			byte[] signature = LowLevel.cutData(dataJoined, OFFSET_SIGNATURE, Encryption.ASYM_SIGNATURE_LENGTH);
	
			if (type == MessageType.HANDSHAKE) {
				MessageDigest hashing = Encryption.getEncryption().getHashingFunction();
				Log.d(MyApplication.APP_TAG, "PARSING KEYS MESSAGE");
				hashing.update(header);
				Log.d(MyApplication.APP_TAG, "Second header:" + header);
				hashing.update(id);
				Log.d(MyApplication.APP_TAG, "Second id:" + id);
				hashing.update(publicKey);
				Log.d(MyApplication.APP_TAG, "Second public key:" + LowLevel.toHex(publicKey));
				Log.d(MyApplication.APP_TAG, "Signature: " + LowLevel.toHex(signature));
				
				byte[] hash = hashing.digest();
				Log.d(MyApplication.APP_TAG, "Hash: " + LowLevel.toHex(hash));

				// cut out the rubbish part at the end
				dataJoined = LowLevel.cutData(dataJoined, 0, LENGTH_CONTENT);
				
				// check the signature
				boolean signatureVerified = false;
				try {
					signatureVerified = Encryption.getEncryption().verify(hash, signature, contact.getId());
				} catch (EncryptionException e) {
				} catch (WrongKeyDecryptionException e) {
				}
				if (!signatureVerified)
					return new ParseResult(idGroup, PendingParseResult.COULD_NOT_VERIFY, null);
				
				// all seems to be fine, so just retrieve the keys and return the result
				return new ParseResult(idGroup, 
				                            PendingParseResult.OK_HANDSHAKE_MESSAGE, 
				                            new KeysMessage(
				                            	id,
				                            	publicKey
				                            ));
			} else if (type == MessageType.CONFIRM) {
				// find the session keys for this person
				SessionKeys keys = Conversation.getConversation(sender).getSessionKeys(SimCard.getSingleton().getNumber());
				if (keys == null)
					return new ParseResult(idGroup, PendingParseResult.COULD_NOT_VERIFY, null);
				
				MessageDigest hashing = Encryption.getEncryption().getHashingFunction();
				Log.d(MyApplication.APP_TAG, "PARSING KEYS MESSAGE");
				hashing.update(HEADER_HANDSHAKE);
				Log.d(MyApplication.APP_TAG, "First header:" + HEADER_HANDSHAKE);
				hashing.update(keys.getKeysId());
				Log.d(MyApplication.APP_TAG, "First keys id:" + keys.getKeysId());
				hashing.update(new EllipticCurveDeffieHellman(keys.getPrivateKey()).getPublicKey());
				Log.d(MyApplication.APP_TAG, "First public key:" + LowLevel.toHex(new EllipticCurveDeffieHellman(keys.getPrivateKey()).getPublicKey()));
				hashing.update(header);
				Log.d(MyApplication.APP_TAG, "Second header:" + header);
				hashing.update(id);
				Log.d(MyApplication.APP_TAG, "Second id:" + id);
				hashing.update(publicKey);
				Log.d(MyApplication.APP_TAG, "Second public key:" + LowLevel.toHex(publicKey));
				Log.d(MyApplication.APP_TAG, "Signature: " + LowLevel.toHex(signature));

				byte[] hash = hashing.digest();
				Log.d(MyApplication.APP_TAG, "Hash: " + LowLevel.toHex(hash));
				
				// check the signature
				boolean signatureVerified = false;
				try {
					signatureVerified = Encryption.getEncryption().verify(hash, signature, contact.getId());
				} catch (EncryptionException e) {
				} catch (WrongKeyDecryptionException e) {
				}
				if (!signatureVerified)
					return new ParseResult(idGroup, PendingParseResult.COULD_NOT_VERIFY, null);
				
				// all seems to be fine, so save the result
                KeysMessage keysMsg = new KeysMessage(
                    	keys.getKeysId(),
                    	keys.getPrivateKey(),
                    	id,
                    	publicKey
                    );
                
                keys.setSessionKey_Out(keysMsg.getKeyOut());
                keys.setSessionKey_In(keysMsg.getKeyIn());
				Log.d(MyApplication.APP_TAG, "Key out: " + LowLevel.toHex(keys.getSessionKey_Out()));
				Log.d(MyApplication.APP_TAG, "Key in: " + LowLevel.toHex(keys.getSessionKey_In()));
                keys.setNextID_Out((byte) 0);
                keys.setLastID_In((byte) 0);
                keys.setKeysConfirmed(true);
                keys.saveToFile();
				
				return new ParseResult(idGroup, 
				                       PendingParseResult.OK_CONFIRM_MESSAGE,
				                       keysMsg);
			} else
				return new ParseResult(idGroup, PendingParseResult.COULD_NOT_DECRYPT, null);
		} catch (StorageFileException e) {
			return new ParseResult(idGroup, PendingParseResult.INTERNAL_ERROR, null);
		}
	}

	@Override
	public byte getHeader() {
		if (mIsConfirmation)
			return HEADER_CONFIRM;
		else
			return HEADER_HANDSHAKE;
	}

	public byte getOtherHeader() {
		if (mIsConfirmation)
			return HEADER_HANDSHAKE;
		else
			return HEADER_CONFIRM;
	}

	@Override
	public byte getId() {
		return mId;
	}
	
	@Override
	public int getMessagePartCount() {
		return getPartsCount();
	}

	public static int getPartsCount() {
		return LowLevel.roundUpDivision(LENGTH_CONTENT, LENGTH_DATA);
	}

}
