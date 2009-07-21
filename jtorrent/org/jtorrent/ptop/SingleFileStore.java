
package org.jtorrent.ptop;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * This class manages access to a single file and allows it to be read and
 * written as one array of bytes broken into pieces of a specified size.
 *
 * @author Hunter Payne
 */
public class SingleFileStore implements IStorage {

    private MappedByteBuffer buffer;
    private FileInputStream stream;
    private FileChannel channel;
    private RandomAccessFile writeFile;
    private int pieceSize;
    private long fileLength;
    private long filePositionIndex;
    private long bufferLength = 131072; // 2 to the 17th, or two requests
    private boolean readOnly;

    private boolean useMemoryMappedFile = false;
    
    /**
     * Creates a new store to manage one file
     *
     * @param storeDir -- where the download we be stored
     * @param pieceSize -- size of each piece of the download
     * @param fileLength -- length of the file
     */
    public SingleFileStore(File storeFile, int pieceSize, long fileLength) 
	throws IOException {

	this(storeFile, pieceSize, fileLength, false);
    }

    /**
     * Creates a new store to manage one file
     *
     * @param storeDir -- directory where the downloaded files are stored
     * @param pieceSize -- size of each piece of the download
     * @param fileLength -- length of this file
     * @param readOnly -- if this store is read only
     */
    public SingleFileStore(File storeFile, int pieceSize, long fileLength, 
			   boolean readOnly) throws IOException {

	this.readOnly = readOnly;
	this.pieceSize = pieceSize;
	this.fileLength = fileLength;
	buffer = null;
	writeFile = null;
	stream = null;
	channel = null;
	filePositionIndex = 0;

	if (!readOnly) {
	    
	    writeFile = new RandomAccessFile(storeFile, "rw");
	    writeFile.setLength(fileLength);
	    channel = writeFile.getChannel();

	    if (useMemoryMappedFile)
		buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 
				     bufferLength);
	    else buffer = null;

	} else {

	    stream = new FileInputStream(storeFile);
	    channel = stream.getChannel();
	    if (useMemoryMappedFile)
		buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, 
				     bufferLength);
	    else buffer = null;
	}
    }

    /** accessor for the length of the target file */
    public long getStoreLength() { return fileLength; }

    /**
     * local byte channel wrapped around a fixed length byte array
     */
    public static class ByteArrayByteChannel implements ByteChannel {

	byte[] bytes;
	private boolean closed;
	private int curr;

	/**
	 * @param arr -- byte array to read or write byte from
	 */
	public ByteArrayByteChannel(byte arr[]) {

	    bytes = arr;
	    closed = false;
	    curr = 0;
	}

	/** reads bytes from our internal byte array into specified buffer */
	public int read(ByteBuffer buf) throws IOException {

	    if (!closed) {

		int numToRead = buf.remaining();
		buf.put(bytes, curr, numToRead);
		curr += numToRead;
		return numToRead;
	    }

	    return -1;
	}

	/** writes bytes from our internal byte array into specified buffer */
	public int write(ByteBuffer src) throws IOException {

	    if (!closed) {

		int numToWrite = src.remaining();

		if (numToWrite + curr <= bytes.length) {

		    src.get(bytes, curr, numToWrite);
		    curr += numToWrite;
		    return numToWrite;
		}

		numToWrite = bytes.length - curr;
		src.get(bytes, curr, numToWrite);
		curr = bytes.length;
	    }

	    return -1;
	}
	    
	/** close this channel */
	public void close() {

	    closed = true;
	}

	/** returns true if this channel is still open */
	public boolean isOpen() {

	    return !closed;
	}

	/** resets this channel so it can be reused */
	public void reset() {

	    closed = false;
	    curr = 0;
	}
    }

    /** sets the read only bit of this store */
    public void setReadOnly() { readOnly = true; }

    /**
     * stores the specified byte array to disk.
     *
     * @param piece -- bytes to store
     * @param pieceIndex -- index of the piece into which this data will be 
     * stored
     * @param begin -- offset in the piece where to begin storing these bytes
     */
    public void storePiece(byte[] piece, int pieceIndex, int begin) 
	throws IOException {

	if (!readOnly) {

	    int index = pieceSize * pieceIndex + begin;

	    if (useMemoryMappedFile) {

		if ((filePositionIndex > index) ||
		    (filePositionIndex + bufferLength < index + piece.length)){

		    filePositionIndex = index;
		    buffer = channel.map(FileChannel.MapMode.READ_WRITE, 
					 filePositionIndex, bufferLength);
		}

		buffer.position((int)(index - filePositionIndex));
		buffer.put(piece, 0, piece.length);
	    
	    } else {
	    
		channel.transferFrom(new ByteArrayByteChannel(piece), 
				     (long)index, (long)piece.length);
	    }
	} else System.err.println("store is readonly!!!!!!");
    }

    /**
     * loads bytes from disk into the specified byte channel
     *
     * @param writable -- byte channel where to write the newly loaded bytes
     * @param pieceIndex -- index of the piece to load the data from
     * @param begin -- offset in the piece where to start loading bytes
     * @param length -- number of bytes to load from disk
     */
    public void loadPiece(WritableByteChannel writable, int pieceIndex, 
			  int begin, int length) throws IOException {

	int index = pieceSize * pieceIndex + begin;

	if (useMemoryMappedFile) {
	    
	    if ((filePositionIndex <= index) &&
		(filePositionIndex + bufferLength >= index + length)) {

		int oldLimit = buffer.limit();
		buffer.position((int)(index - filePositionIndex));
		
		if (length + index < buffer.capacity())
		    buffer.limit(index + length);
		else buffer.limit(buffer.capacity());
		
		writable.write(buffer);
		buffer.limit(oldLimit);

	    } else {
		
		filePositionIndex = index;
		buffer = channel.map(FileChannel.MapMode.READ_WRITE, 
				     filePositionIndex, bufferLength);
		buffer.position((int)(index - filePositionIndex));

		if (length + index < buffer.capacity())
		    buffer.limit(index + length);
		else buffer.limit(buffer.capacity());
		writable.write(buffer);
	    } 
	} else {

	    channel.transferTo((long)index, (long)length, writable);
	}
    }

    /** flushes data to disk */
    public void flush() throws IOException {

	if (buffer != null) buffer = buffer.force();
	else if (channel != null) channel.force(true);
    }

    /** flushes data to disk, and closes the file handles */
    public void close() throws IOException {

	flush();
	if (writeFile != null) writeFile.close();
	else if (stream != null) stream.close();
    }
    /*
    public static void main(String args[]) {

	try {

	    File file = new File(args[0]);
	    SingleFileStore store = 
		new SingleFileStore(file, (int)file.length() / 2, 
				    file.length(), true);
	    NBStore nbstore = new NBStore(store, (int)file.length() / 2);
	    byte read[] = new byte[(int)file.length() / 2];
	    nbstore.loadPiece(new ByteArrayByteChannel(read), 1, 0, 
			      read.length);
	    System.out.println("data " + new String(read));
	    store = new SingleFileStore(file, (int)file.length() / 2, 
					file.length());
	    nbstore.close();
	    nbstore = new NBStore(store, (int)file.length() / 2);
	    nbstore.storePiece(read, 1, 0);
	    nbstore.close();

	} catch (Exception e) {

	    e.printStackTrace();
	}
	}*/
}
