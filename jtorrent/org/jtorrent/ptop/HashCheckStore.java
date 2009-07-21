
package org.jtorrent.ptop;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Hashtable;

/**
 * This class roughly represents the StorageWrapper.py module in BitTorrent 
 * 3.0.2<br>
 * However, it is responsible for wrapping an IStorage object and checking
 * the hashs, throws any InvalidHashExceptions if necessary.  It also handles
 * some small bookkeeping tasks.
 *
 * @author Hunter Payne
 */
public class HashCheckStore implements IStorage {

    private byte hashs[];
    private PartialPiece pieces[];
    private int pieceSize;
    private IStorage store;
    private boolean completed;
    private int requestPieceSize;

    private boolean storeIsThreadsafe = false;

    /**
     * Creates a new store object which can check the hashs as the data is
     * stored.
     *
     * @param store -- storage object which actually transfers bytes to and 
     * from the disk
     * @param numPieces -- number of pieces in this download
     * @param pieceSize -- size of each piece of the download
     * @param requestPieceSize -- size of each download request
     */
    // download_slice_size == requestPieceSize
    public HashCheckStore(IStorage store, int numPieces, int pieceSize,
			  int requestPieceSize) throws IOException {

	this.store = store;
	this.pieceSize = pieceSize;
	this.requestPieceSize = requestPieceSize;
	pieces = new PartialPiece[numPieces];
	for (int i = 0; i < pieces.length; i++) pieces[i] = null;
	generateHashs();
	completed = true;
    }

    /**
     * Creates a new store object which can check the hashs as the data is
     * stored.
     *
     * @param store -- storage object which actually transfers bytes to and 
     * from the disk
     * @param hashs -- the hashs of each piece to download
     * @param pieceSize -- size of each piece of the download
     * @param requestPieceSize -- size of each download request
     */
    public HashCheckStore(IStorage store, byte[] hashs, int pieceSize,
			  int requestPieceSize) {

	completed = false;
	this.requestPieceSize = requestPieceSize;
	this.store = store;
	this.hashs = hashs;
	this.pieceSize = pieceSize;
	pieces = new PartialPiece[hashs.length / 20];

	// loop over each piece and check if it has already been downloaded
	for (int i = 0; i < pieces.length; i++) {

	    try {

		// initialize to null
		pieces[i] = null;

		// compute size of and construct the partial piece
		pieces[i] = getPiece(i);
		byte buf[] = new byte[requestPieceSize];
		SingleFileStore.ByteArrayByteChannel channel = 
		    new SingleFileStore.ByteArrayByteChannel(buf);
		int curr = 0;
		int index = 0;

		// load the piece a few bytes at a time
		while (curr < pieces[i].getActualPieceSize()) {
		    
		    int chunkSize = requestPieceSize;

		    if (pieces[i].getActualPieceSize() < curr + chunkSize) {

			chunkSize = pieces[i].getActualPieceSize() - curr;
			buf = new byte[chunkSize];
			channel = 
			    new SingleFileStore.ByteArrayByteChannel(buf);
		    }

		    store.loadPiece(channel, i, curr, chunkSize);
		    pieces[i].addBytes(buf, curr);
		    channel.reset();
		    curr += chunkSize;
		    index++;
		}

		// if the hash doesn't match, we don't have that piece yet
		if (!checkHash(pieces[i].getHash(), i)) pieces[i] = null;

	    } catch (InvalidHashException ihe) {

		pieces[i] = null;

	    } catch (Exception e) {

		e.printStackTrace();
		pieces[i] = null;
	    }
	}
    }

    /**
     * returns true if all requests have been issued, ie time for endgame mode
     */
    public boolean allPending() {

	if (!completed) {

	    for (int i = 0; i < pieces.length; i++) 
		if ((pieces[i] == null) || (!pieces[i].isPending())) 
		    return false;
	    return true;
	}

	return false;
    }

    /**
     * returns the size of each network request
     */
    public int getRequestPieceSize() {

	return requestPieceSize;
    }

    /**
     * max size of a piece of the download
     */
    public int getPieceSize() {

	return pieceSize;
    }

    /**
     * returns the number of pieces in this download
     */
    public int getNumPieces() {

	return pieces.length;
    }

    /**
     * gets the partial piece object representing one piece of the download
     *
     * @param index -- index of the piece
     */
    public PartialPiece getPiece(int index) {

	if (!completed && (pieces[index] == null)) {

	    int actualPieceSize = pieceSize;

	    if (index == pieces.length - 1) {

		long length = store.getStoreLength();
		actualPieceSize = (int)(length - ((long)index * 
						  (long)pieceSize));
	    }

	    pieces[index] = new PartialPiece(pieceSize, requestPieceSize,
					     actualPieceSize);
	}

	return pieces[index];
    }

    /**
     * gets a bitfield representing the pieces which have been successfully 
     * downloaded
     */
    public boolean[] getBitfield() {

	boolean ret[] = new boolean[pieces.length];

	if (!completed) { // && !readOnly) {

	    boolean allDone = true;

	    for (int i = 0; i < ret.length; i++) {

		ret[i] = ((pieces[i] != null) && (pieces[i].isFinished()));
		if (!ret[i]) allDone = false;
	    }

	    if (allDone) {

		completed = true;
		setReadOnly();
	    }

	    return ret;
	}

	for (int i = 0; i < ret.length; i++) ret[i] = true;
	return ret;
    }

    /** sets the readOnly bit of this store */
    public void setReadOnly() { store.setReadOnly(); }

    // generates the hashs for the file or files represented by this store
    private void generateHashs() throws IOException {

	hashs = new byte[20 * pieces.length];
	long length = store.getStoreLength();
	long curr = 0;

	// loop over each piece
	for (int i = 0; i < pieces.length; i++) {

	    byte piece[] = null;

	    if (length >= curr + pieceSize) 
		piece = new byte[pieceSize];
	    else piece = new byte[(int)(length - curr)];

	    // TODO don't load the entire piece at once, load a few bytes 
	    // at a time
	    curr += piece.length;

	    // load the piece
	    SingleFileStore.ByteArrayByteChannel channel = 
		new SingleFileStore.ByteArrayByteChannel(piece);
	    store.loadPiece(channel, i, 0, piece.length);

	    // and compute its digest
	    byte digest[] = SHAUtils.computeDigest(piece);

	    for (int j = 0; j < 20; j++) hashs[i * 20 + j] = digest[j];
	}
    }

    /**
     * gets the hashs used by this object to check the downloaded file or files
     */
    public byte[] getHashs() {

	return hashs;
    }

    // checks a hash of a specific piece
    private boolean checkHash(byte hash[], int pieceIndex) {

 	for (int i = 0; i < 20; i++)
 	    if (hash[i] != hashs[pieceIndex * 20 + i]) {

		System.out.println("didn't match " + i + ":" + pieceIndex);
		return false;
	    }

	System.out.println("hash matched " + pieceIndex);
	return true;
    }

    /**
     * Stores a piece of data to the disk, if this data completes the download
     * of a piece and the piece's hash doesn't match, an InvalidHashException
     * is thrown
     * 
     * @param piece -- bytes to store
     * @param pieceIndex -- index of piece in which to store this data
     * @param begin -- relative offset inside of the piece where this data will
     * be stored
     * @throws InvalidHashException
     */
    public void storePiece(byte[] piece, int pieceIndex, int begin) 
	throws IOException {

	PartialPiece partialPiece = getPiece(pieceIndex);
	partialPiece.addBytes(piece, begin);
	boolean finished = partialPiece.isFinished();

	// if necessary, check the hash
	if (!finished || checkHash(partialPiece.getHash(), pieceIndex)) {

	    if (storeIsThreadsafe) {
		
		store.storePiece(piece, pieceIndex, begin);

	    } else {

		synchronized (store) {
			
		    store.storePiece(piece, pieceIndex, begin);
		}
	    }

	    
	} else throw new InvalidHashException(pieceIndex);
    }

    /**
     * Loads a piece of data from the disk
     * 
     * @param channel -- channel where the bytes from disk will be copied
     * @param pieceIndex -- index of piece from which to load this data
     * @param begin -- relative offset inside of the piece from where this
     * data will be loaded
     * @param length -- number of bytes to transfer
     */
    public void loadPiece(WritableByteChannel channel, int pieceIndex, 
			  int begin, int length) throws IOException {

	if (storeIsThreadsafe) {

	    store.loadPiece(channel, pieceIndex, begin, length);

	} else {

	    synchronized (store) {

		store.loadPiece(channel, pieceIndex, begin, length);
	    }
	}
    }

    /** returns the total number of bytes in this store */
    public long getStoreLength() {

	return store.getStoreLength();
    }

    /** returns true if this download is complete */
    public boolean isComplete() {

	return completed;
    }

    /** flushes data to disk */
    public void flush() throws IOException {

	if (storeIsThreadsafe) {

	    store.flush();

	} else {

	    synchronized (store) {

		store.flush();
	    }
	}
    }

    /** closes this store */
    public void close() throws IOException {

	store.close();
    }
}
