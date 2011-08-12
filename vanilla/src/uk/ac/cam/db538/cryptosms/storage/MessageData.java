package uk.ac.cam.db538.cryptosms.storage;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.utils.Charset;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;

/**
 * 
 * Class representing a message entry in the secure storage file.
 * 
 * @author David Brazdil
 *
 */
public class MessageData {
	// FILE FORMAT
	public static final int LENGTH_MESSAGE = 133;

	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_TIMESTAMP = 29;
	private static final int LENGTH_MESSAGEBODYLEN = 2;
	private static final int LENGTH_MESSAGEBODY = LENGTH_MESSAGE;

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
	 */
	public static MessageData createMessageData(Conversation parent) throws StorageFileException {
		// create a new one
		MessageData msg = new MessageData(Empty.getEmptyIndex(), false);
		parent.attachMessageData(msg);
		return msg;
	}

	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 */
	static MessageData getMessageData(long index) throws StorageFileException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		synchronized (cacheMessageData) {
			for (MessageData empty: cacheMessageData)
				if (empty.getEntryIndex() == index)
					return empty; 
		}
		
		// create a new one
		return new MessageData(index, true);
	}
	
	// INTERNAL FIELDS
	private long mEntryIndex; // READ ONLY
	private boolean mDeliveredPart;
	private boolean mDeliveredAll;
	private MessageType mMessageType;
	private boolean mUnread;
	private boolean mCompressed;
	private boolean mAscii;
	
	private DateTime mTimeStamp;
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
	 */
	private MessageData(long index, boolean readFromFile) throws StorageFileException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Storage.getStorage().getEntry(index);
			byte[] dataPlain;
			try {
				dataPlain = Encryption.getEncryption().decryptSymmetricWithMasterKey(dataEncrypted);
			} catch (EncryptionException e) {
				throw new StorageFileException(e);
			}
			
			byte flags = dataPlain[OFFSET_FLAGS];
			boolean deliveredPart = ((flags & (1 << 7)) == 0) ? false : true;
			boolean deliveredAll = ((flags & (1 << 6)) == 0) ? false : true;
			boolean messageOutgoing = ((flags & (1 << 5)) == 0) ? false : true;
			boolean unread = ((flags & (1 << 4)) == 0) ? false : true;
			boolean compressed = ((flags & (1 << 3)) == 0) ? false : true;
			boolean ascii = ((flags & (1 << 2)) == 0) ? false : true;

			String timeStamp = Charset.fromAscii8(dataPlain, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP);
			
			setDeliveredPart(deliveredPart);
			setDeliveredAll(deliveredAll);
			setMessageType((messageOutgoing) ? MessageType.OUTGOING : MessageType.INCOMING);
			setUnread(unread);
			setCompressed(compressed);
			setAscii(ascii);
			setTimeStamp(ISODateTimeFormat.dateTimeParser().parseDateTime(timeStamp));
			int messageBodyLength = Math.min(LENGTH_MESSAGEBODY, LowLevel.getUnsignedShort(dataPlain, OFFSET_MESSAGEBODYLEN));
			setMessageBody(LowLevel.cutData(dataPlain, OFFSET_MESSAGEBODY, messageBodyLength));
			setIndexParent(LowLevel.getUnsignedInt(dataPlain, OFFSET_PARENTINDEX));
			setIndexMessageParts(LowLevel.getUnsignedInt(dataPlain, OFFSET_MSGSINDEX));
			setIndexPrev(LowLevel.getUnsignedInt(dataPlain, OFFSET_PREVINDEX));
			setIndexNext(LowLevel.getUnsignedInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			setDeliveredPart(false);
			setDeliveredAll(false);
			setMessageType(MessageType.OUTGOING);
			setUnread(false);
			setCompressed(false);
			setAscii(true);
			setTimeStamp(new DateTime());
			setMessageBody(new byte[0]);
			setIndexParent(0L);
			setIndexMessageParts(0L);
			setIndexPrev(0L);
			setIndexNext(0L);
			
			saveToFile();
		}
		
		synchronized (cacheMessageData) {
			cacheMessageData.add(this);
		}
	}

	// FUNCTIONS

	/**
	 * Save the contents of this class to its place in the storage file.
	 * @throws StorageFileException
	 */
	public void saveToFile() throws StorageFileException {
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
		String timeStamp = ISODateTimeFormat.dateTime().print(this.mTimeStamp);
		msgBuffer.put(Charset.toAscii8(timeStamp, LENGTH_TIMESTAMP));

		// message body
		msgBuffer.put(LowLevel.getBytesUnsignedShort(this.mMessageBody.length));
		msgBuffer.put(LowLevel.wrapData(mMessageBody, LENGTH_MESSAGEBODY));

		// random data
		msgBuffer.put(Encryption.getEncryption().generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		msgBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexParent)); 
		msgBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexMessageParts)); 
		msgBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexPrev));
		msgBuffer.put(LowLevel.getBytesUnsignedInt(this.mIndexNext));
		
		byte[] dataEncrypted = null;
		try {
			dataEncrypted = Encryption.getEncryption().encryptSymmetricWithMasterKey(msgBuffer.array());
		} catch (EncryptionException e) {
			throw new StorageFileException(e);
		}
		Storage.getStorage().setEntry(mEntryIndex, dataEncrypted);
	}

	/**
	 * Returns an instance of the Conversation class that is the parent of this Message in the data structure
	 * @return
	 * @throws StorageFileException
	 */
	public Conversation getParent() throws StorageFileException {
		if (mIndexParent == 0)
			return null;
		return Conversation.getConversation(mIndexParent);
	}
	
	/**
	 * Returns an instance of the Message that's predecessor of this one in the linked list of Messages of this Conversation
	 * @return
	 * @throws StorageFileException
	 */
	public MessageData getPreviousMessageData() throws StorageFileException {
		if (mIndexPrev == 0)
			return null;
		return MessageData.getMessageData(mIndexPrev);
	}

	/**
	 * Returns an instance of the Message that's successor of this one in the linked list of Messages of this Conversation
	 * @return
	 * @throws StorageFileException
	 */
	public MessageData getNextMessageData() throws StorageFileException {
		if (mIndexNext == 0)
			return null;
		return MessageData.getMessageData(mIndexNext);
	}
	
	/**
	 * Returns first message part in the linked list of MessageParts.
	 * Should not be public - Message has API for making this seamlessly
	 * @return
	 * @throws StorageFileException
	 */
	MessageDataPart getFirstMessageDataPart() throws StorageFileException {
		if (mIndexMessageParts == 0)
			return null;
		return MessageDataPart.getMessageDataPart(mIndexMessageParts);
	}
	
	/**
	 * Replaces assigned message parts with given list
	 * @param list
	 * @throws StorageFileException
	 */
	void assignMessageDataParts(ArrayList<MessageDataPart> list) throws StorageFileException {
		// delete all previous message parts
		long indexFirstInStack = getIndexMessageParts();
		while (indexFirstInStack != 0) {
			MessageDataPart msgPart = MessageDataPart.getMessageDataPart(indexFirstInStack);
			indexFirstInStack = msgPart.getIndexNext();
			msgPart.delete();
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
			
			msgPart.saveToFile();
		}
		
		// update pointer in the conversation 
		if (list.size() > 0)
			this.setIndexMessageParts(list.get(0).getEntryIndex());
		else
			this.setIndexMessageParts(0L);
		this.saveToFile();
	}
	
	/**
	 * Delete Message and all the MessageParts it controls
	 * @throws StorageFileException
	 */
	public void delete() throws StorageFileException {
		MessageData prev = this.getPreviousMessageData();
		MessageData next = this.getNextMessageData(); 

		if (prev != null) {
			// this is not the first message in the list
			// update the previous one
			prev.setIndexNext(this.getIndexNext());
			prev.saveToFile();
		} else {
			// this IS the first message in the list
			// update parent
			Conversation parent = this.getParent();
			parent.setIndexMessages(this.getIndexNext());
			parent.saveToFile();
		}
		
		// update next one
		if (next != null) {
			next.setIndexPrev(this.getIndexPrev());
			next.saveToFile();
		}
		
		// delete all of the MessageParts
		MessageDataPart part = getFirstMessageDataPart();
		while (part != null) {
			part.delete();
			part = getFirstMessageDataPart();
		}
		
		// delete this message
		Empty.replaceWithEmpty(mEntryIndex);
		
		// remove from cache
		synchronized (cacheMessageData) {
			cacheMessageData.remove(this);
		}
		
		// make this instance invalid
		this.mEntryIndex = -1L;
	}
	
	// MESSAGE HIGH LEVEL
	
	/**
	 * Returns the data assigned to message part at given index
	 */
	public byte[] getPartData(int index) throws StorageFileException {
		if (index == 0) 
			return this.getMessageBody();
		else {
			--index;
			MessageDataPart part = getFirstMessageDataPart();
			while (part != null) {
				if (index-- == 0)
					return part.getMessageBody();
				part = part.getNextMessageDataPart();
			}
		}
		throw new IndexOutOfBoundsException();
	}
	
	/**
	 * Adds/removes message parts so that there is exactly given number of them
	 * (There is always at least one)
	 * @param count
	 * @throws StorageFileException
	 */
	public void setNumberOfParts(int count) throws StorageFileException {
		--count; // count the first part
		
		MessageDataPart temp = null, part = getFirstMessageDataPart();
		while (count > 0 && part != null) {
			part.setMessageBody(new byte[0]);
			part.setDeliveredPart(false);
			part.saveToFile();
			
			--count;
			temp = part;
			part = part.getNextMessageDataPart();
		}
		
		if (count > 0 && part == null) {
			// we need to add more
			while (count-- > 0) {
				part = MessageDataPart.createMessageDataPart();
				// parent
				part.setIndexParent(this.mEntryIndex);
				// pointers
				if (temp == null) {
					// this is the first one in list
					part.setIndexPrev(0L);
					this.setIndexMessageParts(part.getEntryIndex());
					this.saveToFile();
				}
				else {
					part.setIndexPrev(temp.getEntryIndex());
					temp.setIndexNext(part.getEntryIndex());
					temp.saveToFile();
				}
				part.setIndexNext(0);
				// save and move to next
				if (count <= 0) // otherwise will be saved in the next run
					part.saveToFile();
				temp = part;
			}
			
		} else if (count <= 0 && part != null) {
			// we need to remove some
			while (part != null) {
				temp = part.getNextMessageDataPart();
				part.delete();
				part = temp;
			}
		}
	}
	
	/**
	 * Returns message part of given index (only for indices > 0)
	 * @param index
	 * @return
	 * @throws StorageFileException
	 */
	private MessageDataPart getMessageDataPart(int index) throws StorageFileException {
		if (index <= 0)
			throw new IndexOutOfBoundsException();
		else {
			--index; // for the first part
			MessageDataPart part = this.getFirstMessageDataPart();
			while (part != null && index > 0) {
				part = part.getNextMessageDataPart();
				index--;
			}
			
			if (part != null)
				return part;
			else
				throw new IndexOutOfBoundsException();
		}
	}
	
	/**
	 * Sets data to given message part
	 * @param index
	 * @param data
	 * @throws StorageFileException
	 */
	public void setPartData(int index, byte[] data) throws StorageFileException {
		// if it's too long, just cut it
		if (data.length > LENGTH_MESSAGEBODY)
			data = LowLevel.cutData(data, 0, LENGTH_MESSAGEBODY);

		if (index == 0) {
			this.setMessageBody(data);
			this.saveToFile();
		} else {
			MessageDataPart part = getMessageDataPart(index);
			part.setMessageBody(data);
			part.saveToFile();
		}
	}

	/**
	 * Returns whether given message part was delivered
	 * @param index
	 * @return
	 * @throws StorageFileException
	 */
	public boolean getPartDelivered(int index) throws StorageFileException {
		if (index == 0)
			return this.getDeliveredPart();
		else
			return getMessageDataPart(index).getDeliveredPart();
	}

	/**
	 * Sets whether given message part was delivered
	 * @param index
	 * @return
	 * @throws StorageFileException
	 */
	public void setPartDelivered(int index, boolean delivered) throws StorageFileException {
		if (index == 0) {
			this.setDeliveredPart(delivered);
			this.saveToFile();
		} else {
			MessageDataPart part = getMessageDataPart(index);
			part.setDeliveredPart(delivered);
			part.saveToFile();
		}
	}

	// GETTERS / SETTERS
	
	long getEntryIndex() {
		return mEntryIndex;
	}
	
	void setDeliveredPart(boolean deliveredPart) {
		this.mDeliveredPart = deliveredPart;
	}

	boolean getDeliveredPart() {
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

	public void setTimeStamp(DateTime timeStamp) {
		this.mTimeStamp = timeStamp;
	}

	public DateTime getTimeStamp() {
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
