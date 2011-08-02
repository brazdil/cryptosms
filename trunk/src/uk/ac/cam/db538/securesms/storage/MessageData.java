package uk.ac.cam.db538.securesms.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.text.format.Time;

import uk.ac.cam.db538.securesms.Charset;
import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing a message entry in the secure storage file.
 * 
 * @author David Brazdil
 *
 */
public class MessageData {
	// FILE FORMAT
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_TIMESTAMP = 29;
	private static final int LENGTH_MESSAGEBODYLEN = 2;
	private static final int LENGTH_MESSAGEBODY = 140;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_TIMESTAMP = OFFSET_FLAGS + LENGTH_FLAGS;
	private static final int OFFSET_MESSAGEBODYLEN = OFFSET_TIMESTAMP + LENGTH_TIMESTAMP;
	private static final int OFFSET_MESSAGEBODY = OFFSET_MESSAGEBODYLEN + LENGTH_MESSAGEBODYLEN;

	private static final int OFFSET_RANDOMDATA = OFFSET_MESSAGEBODY + LENGTH_MESSAGEBODY;

	private static final int OFFSET_NEXTINDEX = Storage.ENCRYPTED_ENTRY_SIZE - 4;
	private static final int OFFSET_PREVINDEX = OFFSET_NEXTINDEX - 4;
	private static final int OFFSET_MSGSINDEX = OFFSET_PREVINDEX - 4;
	private static final int OFFSET_PARENTINDEX = OFFSET_MSGSINDEX - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_PARENTINDEX - OFFSET_RANDOMDATA;	
	
	public enum MessageType {
		INCOMING,
		OUTGOING
	}
	
	// STATIC
	
	private static ArrayList<MessageData> cacheMessageData = new ArrayList<MessageData>();
	
	/**
	 * Removes all instances from the list of cached objects.
	 * Be sure you don't use the instances afterwards.
	 */
	public static void forceClearCache() {
		synchronized (cacheMessageData) {
			cacheMessageData = new ArrayList<MessageData>();
		}
	}

	/**
	 * Returns an instance of a new MessageData entry in the storage file.
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public static MessageData createMessageData(Conversation parent) throws StorageFileException, IOException {
		return createMessageData(parent, true);
	}
	
	/**
	 * Returns an instance of a new MessageData entry in the storage file.
	 * @param lockAllow		Allow file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public static MessageData createMessageData(Conversation parent, boolean lockAllow) throws StorageFileException, IOException {
		// create a new one
		MessageData msg = new MessageData(Empty.getEmptyIndex(lockAllow), false, lockAllow);
		parent.attachMessageData(msg, lockAllow);
		return msg;
	}

	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 */
	static MessageData getMessageData(long index) throws StorageFileException, IOException {
		return getMessageData(index, true);
	}
	
	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 * @param lock		File lock allow
	 */
	static MessageData getMessageData(long index, boolean lockAllow) throws StorageFileException, IOException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheMessageData) {
			for (MessageData empty: cacheMessageData)
				if (empty.getEntryIndex() == index)
					return empty; 
		}
		
		// create a new one
		return new MessageData(index, true, lockAllow);
	}
	
	// INTERNAL FIELDS
	private long mEntryIndex; // READ ONLY
	private boolean mDeliveredPart;
	private boolean mDeliveredAll;
	private MessageType mMessageType;
	private boolean mUnread;
	private boolean mCompressed;
	private boolean mAscii;
	
	private Time mTimeStamp;
	private byte[] mMessageBody;
	private long mIndexParent;
	private long mIndexMessageParts;
	private long mIndexPrev ;
	private long mIndexNext;
	
	// CONSTRUCTORS
	
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @throws StorageFileException
	 * @throws IOException
	 */
	private MessageData(long index, boolean readFromFile) throws StorageFileException, IOException {
		this(index, readFromFile, true);
	}
	
	/**
	 * Constructor
	 * @param index			Which chunk of data should occupy in file
	 * @param readFromFile	Does this entry already exist in the file?
	 * @param lockAllow		Allow the file to be locked
	 * @throws StorageFileException
	 * @throws IOException
	 */
	private MessageData(long index, boolean readFromFile, boolean lockAllow) throws StorageFileException, IOException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Storage.getDatabase().getEntry(index, lockAllow);
			byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
			
			byte flags = dataPlain[OFFSET_FLAGS];
			boolean deliveredPart = ((flags & (1 << 7)) == 0) ? false : true;
			boolean deliveredAll = ((flags & (1 << 6)) == 0) ? false : true;
			boolean messageOutgoing = ((flags & (1 << 5)) == 0) ? false : true;
			boolean unread = ((flags & (1 << 4)) == 0) ? false : true;
			boolean compressed = ((flags & (1 << 3)) == 0) ? false : true;
			boolean ascii = ((flags & (1 << 2)) == 0) ? false : true;
			
			Time timeStamp = new Time();
			timeStamp.parse3339(Charset.fromAscii8(dataPlain, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP));

			setDeliveredPart(deliveredPart);
			setDeliveredAll(deliveredAll);
			setMessageType((messageOutgoing) ? MessageType.OUTGOING : MessageType.INCOMING);
			setUnread(unread);
			setCompressed(compressed);
			setAscii(ascii);
			setTimeStamp(timeStamp);
			int messageBodyLength = Math.min(LENGTH_MESSAGEBODY, Storage.getShort(dataPlain, OFFSET_MESSAGEBODYLEN));
			setMessageBody(Storage.cutData(dataPlain, OFFSET_MESSAGEBODY, messageBodyLength));
			setIndexParent(Storage.getUnsignedInt(dataPlain, OFFSET_PARENTINDEX));
			setIndexMessageParts(Storage.getUnsignedInt(dataPlain, OFFSET_MSGSINDEX));
			setIndexPrev(Storage.getUnsignedInt(dataPlain, OFFSET_PREVINDEX));
			setIndexNext(Storage.getUnsignedInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			Time timeStamp = new Time(); 
			timeStamp.setToNow();
			
			setDeliveredPart(false);
			setDeliveredAll(false);
			setMessageType(MessageType.OUTGOING);
			setUnread(false);
			setCompressed(false);
			setAscii(true);
			setTimeStamp(timeStamp);
			setMessageBody(new byte[0]);
			setIndexParent(0L);
			setIndexMessageParts(0L);
			setIndexPrev(0L);
			setIndexNext(0L);
			
			saveToFile(lockAllow);
		}
		
		synchronized (cacheMessageData) {
			cacheMessageData.add(this);
		}
	}

	// FUNCTIONS

	/**
	 * Save the contents of this class to its place in the storage file.
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public void saveToFile() throws StorageFileException, IOException {
		saveToFile(true);
	}

	/**
	 * Save the contents of this class to its place in the storage file.
	 * @param lock		Allow the file to be locked
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public void saveToFile(boolean lock) throws StorageFileException, IOException {
		ByteBuffer msgBuffer = ByteBuffer.allocate(Storage.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		if (this.mDeliveredPart)
			flags |= (byte) ((1 << 7) & 0xFF);
		if (this.mDeliveredAll)
			flags |= (byte) ((1 << 6) & 0xFF);
		if (this.mMessageType == MessageType.OUTGOING)
			flags |= (byte) ((1 << 5) & 0xFF);
		if (this.mUnread)
			flags |= (byte) ((1 << 4) & 0xFF);
		if (this.mCompressed)
			flags |= (byte) ((1 << 3) & 0xFF);
		if (this.mAscii)
			flags |= (byte) ((1 << 2) & 0xFF);
		msgBuffer.put(flags);
		
		// time stamp
		msgBuffer.put(Charset.toAscii8(this.mTimeStamp.format3339(false), LENGTH_TIMESTAMP));

		// message body
		msgBuffer.put(Storage.getBytes((short) this.mMessageBody.length));
		msgBuffer.put(Storage.wrapData(mMessageBody, LENGTH_MESSAGEBODY));

		// random data
		msgBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		msgBuffer.put(Storage.getBytes(this.mIndexParent)); 
		msgBuffer.put(Storage.getBytes(this.mIndexMessageParts)); 
		msgBuffer.put(Storage.getBytes(this.mIndexPrev));
		msgBuffer.put(Storage.getBytes(this.mIndexNext));
		
		byte[] dataEncrypted = Encryption.encryptSymmetric(msgBuffer.array(), Encryption.retreiveEncryptionKey());
		Storage.getDatabase().setEntry(mEntryIndex, dataEncrypted, lock);
	}

	/**
	 * Returns an instance of the Conversation class that is the parent of this Message in the data structure
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public Conversation getParent() throws StorageFileException, IOException {
		return getParent(true);
	}
	
	/**
	 * Returns an instance of the Conversation class that is the parent of this Message in the data structure
	 * @param lockAllow		Allow file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public Conversation getParent(boolean lockAllow) throws StorageFileException, IOException {
		if (mIndexParent == 0)
			return null;
		return Conversation.getConversation(mIndexParent, lockAllow);
	}
	
	/**
	 * Returns an instance of the Message that's predecessor of this one in the linked list of Messages of this Conversation
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public MessageData getPreviousMessageData() throws StorageFileException, IOException {
		return getPreviousMessageData(true);
	}
	
	/**
	 * Returns an instance of the Message that's predecessor of this one in the linked list of Messages of this Conversation
	 * @param lockAllow		Allow file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public MessageData getPreviousMessageData(boolean lockAllow) throws StorageFileException, IOException {
		if (mIndexPrev == 0)
			return null;
		return MessageData.getMessageData(mIndexPrev, lockAllow);
	}

	/**
	 * Returns an instance of the Message that's successor of this one in the linked list of Messages of this Conversation
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public MessageData getNextMessageData() throws StorageFileException, IOException {
		return getNextMessageData(true);
	}
	
	/**
	 * Returns an instance of the Message that's successor of this one in the linked list of Messages of this Conversation
	 * @param lockAllow		Allow file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public MessageData getNextMessageData(boolean lockAllow) throws StorageFileException, IOException {
		if (mIndexNext == 0)
			return null;
		return MessageData.getMessageData(mIndexNext, lockAllow);
	}
	
	/**
	 * Returns first message part in the linked list of MessageParts.
	 * Should not be public - Message has API for making this seamlessly
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	MessageDataPart getFirstMessageDataPart() throws StorageFileException, IOException {
		return getFirstMessageDataPart(true);
	}
	
	/**
	 * Returns first message part in the linked list of MessageParts.
	 * Should not be public - Message has API for making this seamlessly
	 * @param lockAllow		Allow the file to be locked
	 * @return
	 * @throws StorageFileException
	 * @throws IOException
	 */
	MessageDataPart getFirstMessageDataPart(boolean lockAllow) throws StorageFileException, IOException {
		if (mIndexMessageParts == 0)
			return null;
		return MessageDataPart.getMessageDataPart(mIndexMessageParts, lockAllow);
	}
	
	/**
	 * Replaces assigned message parts with given list
	 * @param list
	 * @throws IOException
	 * @throws StorageFileException
	 */
	void assignMessageDataParts(ArrayList<MessageDataPart> list) throws IOException, StorageFileException {
		assignMessageDataParts(list, true);
	}
	
	/**
	 * Replaces assigned message parts with given list
	 * @param list
	 * @param lockAllow		Allow the file to be locked
	 * @throws IOException
	 * @throws StorageFileException
	 */
	void assignMessageDataParts(ArrayList<MessageDataPart> list, boolean lockAllow) throws IOException, StorageFileException {
		Storage.getDatabase().lockFile(lockAllow);
		try {
			// delete all previous message parts
			long indexFirstInStack = getIndexMessageParts();
			while (indexFirstInStack != 0) {
				MessageDataPart msgPart = MessageDataPart.getMessageDataPart(indexFirstInStack, false);
				indexFirstInStack = msgPart.getIndexNext();
				msgPart.delete(false);
			}

			// add new ones
			for (int i = 0; i < list.size(); ++i) {
				MessageDataPart msgPart = list.get(i);
				
				// parent
				msgPart.setIndexParent(this.mEntryIndex);
				
				// previous pointer
				if (i > 0) 
					msgPart.setIndexPrev(list.get(i - 1).getEntryIndex());
				else
					msgPart.setIndexPrev(0L);
				
				// next pointer
				if (i < list.size() - 1) 
					msgPart.setIndexNext(list.get(i + 1).getEntryIndex());
				else
					msgPart.setIndexNext(0L);
				
				msgPart.saveToFile(false);
			}
			
			// update pointer in the conversation 
			if (list.size() > 0)
				this.setIndexMessageParts(list.get(0).getEntryIndex());
			else
				this.setIndexMessageParts(0L);
			this.saveToFile(false);
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			Storage.getDatabase().unlockFile(lockAllow);	
		}
	}
	
	/**
	 * Delete Message and all the MessageParts it controls
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public void delete() throws StorageFileException, IOException {
		delete(true);
	}
	
	/**
	 * Delete Message and all the MessageParts it controls
	 * @param lockAllow 	Allow the file to be locked
	 * @throws StorageFileException
	 * @throws IOException
	 */
	public void delete(boolean lockAllow) throws StorageFileException, IOException {
		Storage db = Storage.getDatabase();
		
		db.lockFile(lockAllow);
		try {
			MessageData prev = this.getPreviousMessageData(false);
			MessageData next = this.getNextMessageData(false); 
	
			if (prev != null) {
				// this is not the first message in the list
				// update the previous one
				prev.setIndexNext(this.getIndexNext());
				prev.saveToFile(false);
			} else {
				// this IS the first message in the list
				// update parent
				Conversation parent = this.getParent(false);
				parent.setIndexMessages(this.getIndexNext());
				parent.saveToFile(false);
			}
			
			// update next one
			if (next != null) {
				next.setIndexPrev(this.getIndexPrev());
				next.saveToFile(false);
			}
			
			// delete all of the MessageParts
			MessageDataPart part = getFirstMessageDataPart(false);
			while (part != null) {
				part.delete(false);
				part = getFirstMessageDataPart(false);
			}
			
			// delete this message
			Empty.replaceWithEmpty(mEntryIndex, false);
			
			// remove from cache
			synchronized (cacheMessageData) {
				cacheMessageData.remove(this);
			}
			
			// make this instance invalid
			this.mEntryIndex = -1L;
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			db.unlockFile(lockAllow);
		}
	}
	
	// MESSAGE HIGH LEVEL
	
	public byte[] getAssignedData() throws StorageFileException, IOException {
		return getAssignedData(true);
	}
	
	public byte[] getAssignedData(boolean lockAllow) throws StorageFileException, IOException {
		Storage db = Storage.getDatabase();
		
		db.lockFile(lockAllow);
		try {
			// count the total length
			int length = getMessageBody().length;
			MessageDataPart part = getFirstMessageDataPart(false);
			while (part != null) {
				length += part.getMessageBody().length;
				part = part.getNextMessageDataPart(false);
			}
			
			byte[] result = new byte[length], data;
			int pos = 0, len;
			
			// copy all the data together
			data = getMessageBody();
			len = data.length;
			System.arraycopy(data, 0, result, pos, len);
			pos += len;
			
			part = getFirstMessageDataPart(false);
			while (part != null) {
				data = part.getMessageBody();
				len = data.length;
				System.arraycopy(data, 0, result, pos, len);
				pos += len;
				part = part.getNextMessageDataPart(false);
			}
			
			return result;
		} catch (StorageFileException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		} finally {
			db.unlockFile(lockAllow);
		}
	}

	// GETTERS / SETTERS
	
	long getEntryIndex() {
		return mEntryIndex;
	}
	
	public void setDeliveredPart(boolean deliveredPart) {
		this.mDeliveredPart = deliveredPart;
	}

	public boolean getDeliveredPart() {
		return mDeliveredPart;
	}

	public void setDeliveredAll(boolean deliveredAll) {
		this.mDeliveredAll = deliveredAll;
	}

	public boolean getDeliveredAll() {
		return mDeliveredAll;
	}

	public void setMessageType(MessageType messageType) {
		this.mMessageType = messageType;
	}

	public MessageType getMessageType() {
		return mMessageType;
	}

	public void setUnread(boolean unread) {
		this.mUnread = unread;
	}

	public boolean getUnread() {
		return mUnread;
	}

	public void setCompressed(boolean compressed) {
		this.mCompressed = compressed;
	}

	public boolean getCompressed() {
		return mCompressed;
	}

	public void setAscii(boolean ascii) {
		this.mAscii = ascii;
	}

	public boolean getAscii() {
		return mAscii;
	}

	void setMessageBody(byte[] messageBody) {
		this.mMessageBody = messageBody;
	}

	byte[] getMessageBody() {
		return mMessageBody;
	}

	public void setTimeStamp(Time timeStamp) {
		this.mTimeStamp = timeStamp;
	}

	public Time getTimeStamp() {
		return mTimeStamp;
	}

	long getIndexMessageParts() {
		return mIndexMessageParts;
	}

	void setIndexMessageParts(long indexMessageParts) {
		if (indexMessageParts > 0xFFFFFFFFL || indexMessageParts < 0L)
			throw new IndexOutOfBoundsException();
			
		this.mIndexMessageParts = indexMessageParts;
	}

	long getIndexPrev() {
		return mIndexPrev;
	}

	void setIndexPrev(long indexPrev) {
	    if (indexPrev > 0xFFFFFFFFL || indexPrev < 0L)
	    	throw new IndexOutOfBoundsException();
		
		this.mIndexPrev = indexPrev;
	}

	long getIndexNext() {
		return mIndexNext;
	}

	void setIndexNext(long indexNext) {
	    if (indexNext > 0xFFFFFFFFL || indexNext < 0L)
	    	throw new IndexOutOfBoundsException();
		
		this.mIndexNext = indexNext;
	}

	void setIndexParent(long indexParent) {
		this.mIndexParent = indexParent;
	}

	long getIndexParent() {
		return mIndexParent;
	}
}
