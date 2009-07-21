
package org.jtorrent.ptop;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;

import org.jtorrent.msgdata.BencodedMessage;

/**
 * This class manages access to a list of files and allows them to be read and
 * written as one array of bytes broken into pieces of a specified size.
 *
 * @author Hunter Payne
 */
public class DirectoryStore implements IStorage {

    private MappedByteBuffer buffer;
    private FileInputStream stream;
    private FileChannel channel;
    private RandomAccessFile writeFile;
    private int pieceSize;
    private long fileLength;
    private long[] sizes;
    private File[] paths;
    private int currentFile;
    private boolean readOnly;
    private long filePositionIndex;

    private boolean useMemoryMappedFile = false;

    /**
     * Creates a new store to manage a list of files
     *
     * @param storeDir -- directory where the downloaded files are stored
     * @param pieceSize -- size of each piece of the download
     * @param fileInfos -- list of files to manage
     */
    public DirectoryStore(File storeDir, int pieceSize, ArrayList fileInfos) 
	throws IOException {

	this(storeDir, pieceSize, fileInfos, false);
    }

    /**
     * Creates a new store to manage a list of files
     *
     * @param storeDir -- directory where the downloaded files are stored
     * @param pieceSize -- size of each piece of the download
     * @param fileInfos -- list of files to manage
     * @param readOnly -- if this store is read only
     */
    public DirectoryStore(File storeDir, int pieceSize, ArrayList fileInfos, 
			  boolean readOnly) throws IOException {

	filePositionIndex = 0;
	currentFile = -1;
	this.pieceSize = pieceSize;
	this.readOnly = readOnly;
	fileLength = 0;
	sizes = new long[fileInfos.size()];
	paths = new File[fileInfos.size()];
	buffer = null;
	channel = null;
	writeFile = null;
	stream = null;

	for (int i = 0; i < fileInfos.size(); i++) {

	    Object o = fileInfos.get(i);

	    if (o instanceof FileInfo) {

		FileInfo info = (FileInfo)o;
		sizes[i] = info.fileLength;
		paths[i] = new File(storeDir.getAbsolutePath(), info.filePath);
		fileLength += info.fileLength;

	    } else {

		BencodedMessage fileMess = (BencodedMessage)o;
		ArrayList filePathList = 
		    (ArrayList)fileMess.getListValue("path");

		paths[i] = new File(storeDir.getAbsolutePath(), 
				    (String)filePathList.get(0));

		for (int j = 1; j < filePathList.size(); j++)
		    paths[i] = new File(paths[i],
					(String)filePathList.get(j));

		sizes[i] = fileMess.getIntValue("length");
		fileLength += sizes[i];
	    }
	}

	loadNextFile(); // TODO is this needed???
    }

    // loads the next file in the list of files
    private void loadNextFile() throws IOException {

	if ((currentFile + 1 >= paths.length) || (currentFile == -1)) return;

	if (!readOnly) {
	    
	    if (writeFile != null) close();
	    File fileToLoad = paths[++currentFile];

	    if (!fileToLoad.exists()) {

		File parentDir = fileToLoad.getParentFile();

		if (((parentDir.exists()) || (parentDir.mkdirs())) &&
		    (fileToLoad.createNewFile())) {

		    System.out.println("made " + fileToLoad.getAbsolutePath());

		} else throw new IOException("no file " + 
					     fileToLoad.getAbsolutePath());
	    }

	    writeFile = new RandomAccessFile(fileToLoad, "rw");
	    writeFile.setLength(sizes[currentFile]);
	    channel = writeFile.getChannel();

	    if (useMemoryMappedFile)
		buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 
				     channel.size());
	    
	} else {

	    if (stream != null) close();
	    stream = new FileInputStream(paths[++currentFile]);
	    channel = stream.getChannel();
	    if (useMemoryMappedFile)
		buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, 
				     channel.size());
	}
    }

    /** accessor for the total length of all files in this store */
    public long getStoreLength() { return fileLength; }

    /**
     * local byte channel wrapped around a fixed length byte array
     */
    public static class ByteArrayByteChannel implements ByteChannel {

	private byte[] bytes;
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
 		src.get(bytes, curr, numToWrite);
 		curr += numToWrite;
 		return numToWrite;
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
    }

    /** sets the read only bit of this store */
    public void setReadOnly() { readOnly = true; }

    // opens a specific file to a specific offset
    // basically reinitializes the writeFile from the currentFile and 
    // specified file offset
    private void openFiles(int fileOffset) throws IOException {

	if (!readOnly) {
	    
	    if (writeFile != null) close();
	    File fileToLoad = paths[currentFile];

	    if (!fileToLoad.exists()) {

		File parentDir = fileToLoad.getParentFile();

		if (((parentDir.exists()) || (parentDir.mkdirs())) &&
		    (fileToLoad.createNewFile())) {

		    System.out.println("made " + fileToLoad.getAbsolutePath());

		} else throw new IOException("no file " + 
					     fileToLoad.getAbsolutePath());
	    }

	    writeFile = new RandomAccessFile(fileToLoad, "rw");
	    writeFile.setLength(sizes[currentFile]);
	    channel = writeFile.getChannel();

	    if (useMemoryMappedFile) {

		buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 
				     channel.size());
		buffer.position(fileOffset);

	    } else filePositionIndex = fileOffset;
	} else {

	    if (stream != null) close();
	    stream = new FileInputStream(paths[currentFile]);
	    channel = stream.getChannel();

	    if (useMemoryMappedFile) {

		buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, 
				     channel.size());
		buffer.position(fileOffset);

	    } else filePositionIndex = fileOffset;
	}
    }

    // loads the correct file given the specified absolute offset 
    private void loadCorrectFileAndIndex(int index) throws IOException {

	int curr = 0;

	// loop over the files into we find the open where this index occures
	for (int i = 0; i < sizes.length; i++) {

	    curr += sizes[i];

	    // if we found our file
	    if (curr >= index) {
		
		// if we need to reopen the file handles
		if (currentFile != i) {

		    currentFile = i;
		    openFiles(index - curr + (int)sizes[i]);

		    // if we have already opened file
		} else if (!useMemoryMappedFile) {

		    filePositionIndex = (index - curr + (int)sizes[i]);
		}

		return;
	    }
	}
    }

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

	if (readOnly) return;

	int index = pieceSize * pieceIndex + begin;
	loadCorrectFileAndIndex(index);

	if (useMemoryMappedFile) {
	    
	    int remaining = buffer.remaining();
	    
	    if (remaining > piece.length) {
		
		buffer.put(piece, 0, piece.length);
		
	    } else {
		
		buffer.put(piece, 0, remaining);
		
		int leftOver = piece.length - remaining;
		int curr = remaining;
		
		while (leftOver > 0) {
		    
		    loadNextFile();
		    remaining = buffer.remaining();
		    
		    if (leftOver > remaining) {
			
			buffer.put(piece, curr, remaining);
			curr += remaining;
			
		    } else buffer.put(piece, curr, leftOver);

		    leftOver -= remaining;
		}
	    }	    
	} else {
	    
	    // we don't need to open another file
	    if (channel.size() >= filePositionIndex + piece.length) {

		channel.transferFrom(new ByteArrayByteChannel(piece), 
				     filePositionIndex,
				     (long)piece.length);

	    } else {

		ByteArrayByteChannel babc = new ByteArrayByteChannel(piece);
		channel.transferFrom(babc, filePositionIndex,
				     channel.size() - filePositionIndex);
				     
		long leftOver = 
		    piece.length - channel.size() + filePositionIndex;

		// loop over subsequent files to get all our bytes
		while ((leftOver > 0) && (currentFile + 1 < paths.length)) {

		    loadNextFile();

		    if (leftOver > channel.size())
			channel.transferFrom(babc, 0, channel.size());
		    else channel.transferFrom(babc, 0, (long)leftOver);

		    leftOver -= channel.size();
		}
	    }
	}
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
	loadCorrectFileAndIndex(index);

	if (useMemoryMappedFile) {
	    
	    int remaining = buffer.remaining();

	    if (remaining > length) {

		int oldLimit = buffer.limit();

		if (length + index < buffer.capacity())
		    buffer.limit(index + length);
		else buffer.limit(buffer.capacity());

		writable.write(buffer);
		buffer.limit(oldLimit);
		
	    } else if (remaining < length) {

		writable.write(buffer);
		int leftOver = length - remaining;
		
		while (leftOver > 0) {

		    loadNextFile();
		    remaining = buffer.remaining();

		    if (leftOver >= remaining) {

			writable.write(buffer);

		    } else {

			int oldLimit = buffer.limit();

			if (leftOver < buffer.capacity())
			    buffer.limit(leftOver);
			else buffer.limit(buffer.capacity());

			buffer.position(0);
			writable.write(buffer);
			buffer.limit(oldLimit);
		    }

		    leftOver -= remaining;
		}
	    } else {

		writable.write(buffer);
	    }
	} else {

	    // we don't need to open another file
	    if (channel.size() > filePositionIndex + length) {

		channel.transferTo(filePositionIndex,(long)length, 
				   writable);
	    } else {

		channel.transferTo(filePositionIndex, channel.size() - 
				   filePositionIndex, writable);
		long leftOver = length - channel.size() + filePositionIndex;
		
		// loop over subsequent files to get all our bytes
		while ((leftOver > 0) && (currentFile + 1 < paths.length)) {

		    loadNextFile();
		    
		    if (leftOver > channel.size()) 
			channel.transferTo(0, channel.size(), writable);
		    else channel.transferTo(0, leftOver, writable);

		    leftOver -= channel.size();
		}
	    }
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
	    ArrayList list = new ArrayList();
	    list.add(new FileInfo("Testfile", 4315));
	    list.add(new FileInfo("Testfile2", 7186));

	    DirectoryStore store = new DirectoryStore(file, 1024, list, true);
	    NBStore nbstore = new NBStore(store, 1024);
	    byte read[] = new byte[1024];
 	    nbstore.loadPiece(new ByteArrayByteChannel(read), 4, 0, 1024);
 	    System.out.println("data " + new String(read));
	    store = new DirectoryStore(file, 1024, list);
	    nbstore = new NBStore(store, 1024);
 	    nbstore.storePiece(read, 4, 0);
 	    nbstore.flush();

	} catch (Exception e) {

	    e.printStackTrace();
	}
	}*/
}
