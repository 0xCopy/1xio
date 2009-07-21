
package org.jtorrent.ptop;

import java.security.MessageDigest;
import java.util.Hashtable;

/**
 * The class checks the hash of a piece of a download.  It is used by the
 * HashCheckStore to manage accounting information about the state of a piece
 * of a download.  It uses the MessageDigest class from the java.security
 * package to compute the SHA-1 digest of this piece which is checked by
 * the HashCheckStore class.
 *
 * @author Hunter Payne
 * @version 0.1
 */
public class PartialPiece {

    private MessageDigest digest;
    private int bytesWritten;
    private int pieceSize;
    private int requestPieceSize;
    private int actualPieceSize;
    private boolean[] loaded;
    private boolean[] active;
    private int activeCount;
    private Hashtable undigestedBytes;

    /**
     * creates a partial piece to represent one piece of the download
     *
     * @param pieceSize -- theortical size of this piece
     * @param requestPieceSize -- size of each downloaded chunk of this piece
     * @param actualPieceSize -- the real size of this piece
     */
    public PartialPiece(int pieceSize, int requestPieceSize, 
			int actualPieceSize) {

	undigestedBytes = new Hashtable();

	try {

	    digest = MessageDigest.getInstance("SHA-1");

	} catch (java.security.NoSuchAlgorithmException nsae) {

	    nsae.printStackTrace();
	    System.exit(1);
	}

	this.actualPieceSize = actualPieceSize;
	this.pieceSize = pieceSize;
	this.requestPieceSize = requestPieceSize;
	bytesWritten = 0;
	activeCount = 0;

	// compute the number of requests to download
	double numRequests = 
	    (double)actualPieceSize / (double)requestPieceSize;
	if (numRequests == Math.floor(numRequests))
	    loaded = new boolean[(int)numRequests];
	else loaded = new boolean[(int)numRequests + 1];

	// setup accounting info
	active = new boolean[loaded.length];

	for (int i = 0; i < loaded.length; i++) {

	    loaded[i] = false;
	    active[i] = false;
	}
    }

    /**
     * returns true if all requests have been issued, ie time for endgame mode
     */
    public boolean isPending() {

	for (int i = 0; i < loaded.length; i++) {

	    if (!loaded[i] && !active[i]) return false;
	}

	return true;
    }

    /**
     * loop over the active requests for this peice and if there is one 
     * return true
     */
    protected boolean isActive() {

	for (int i = 0; i < loaded.length; i++) {

	    if (!loaded[i] && active[i]) return true;
	}

	return false;
    }

    /** returns the theorical size of this piece */
    public int getLength() { return pieceSize; }

    /** returns the index of the next chunk to download */
    public int getNextBegin() {

	synchronized(this) {

	    for (int i = 0; i < active.length; i++) {

		if (!active[i]) {

		    active[i] = true;
		    activeCount++;
		    return i * requestPieceSize;
		}
	    }

	    //	    (new Exception("out of beings")).printStackTrace();
	    return -1;
	}
    }

    /**
     * gets the number of network requested required to download this piece
     */
    public int getNumRequests() {

	return loaded.length;
    }

    /** returns true if this chunk index has been downloaded */
    public boolean contains(int index) {

	return loaded[index];
    }

    /** 
     * returns true if this chunk index has been downloaded or is currently 
     * being loaded 
     */
    public boolean isActive(int index) {

	return active[index];
    }

    /**
     * called to notify this object that a chunk's download has been stopped
     * or its connection has been lost
     */
    public void lostDownload(int index) {

	synchronized(this) {

	    activeCount--;
	    active[index / requestPieceSize] = false;
	}
    }

    /**
     * called to notify this object that the piece's hash didn't match,
     * so it has to be downloaded again
     */
    public void invalidatePiece() {

	for (int i = 0; i < active.length; i++) {

	    active[i] = false;
	    loaded[i] = false;
	}

	digest.reset();
    }
    
    /** returns true if this piece has been downloaded */
    public boolean isFinished() {

	if (bytesWritten == actualPieceSize) {

	    for (int i = 0; i < loaded.length; i++)
		if (!loaded[i]) return false;

	    return true;
	}

	return false;
    }

    /** returns the actual number of bytes in this piece */
    public int getActualPieceSize() {

	return actualPieceSize;
    }

    /** returns the number of bytes already written */
    public int getBytesWritten() {

	return bytesWritten;
    }

    /** 
     * called to notify this object that some new bytes have been downloaded
     */
    public void addBytes(byte[] data, int begin) {

	int requestIndex = begin / requestPieceSize;
	boolean allLoaded = true;

	if (!loaded[requestIndex]) 
	    updateIndicies(begin, begin + data.length);

	for (int i = 0; allLoaded && i < requestIndex; i++) 
	    if (!loaded[i]) allLoaded = false;

	// if all data before this request have already been loaded
	if (allLoaded) {

	    // add this data to the digest
	    digest.update(data);

	    // get all the digests after this piece
	    for (int i = requestIndex + 1; i < loaded.length && loaded[i]; 
		 i++) {

		Integer index = new Integer(i);
		byte undigestedData[] = (byte[])undigestedBytes.get(index);
		
		if (undigestedData != null) {
		    
		    undigestedBytes.remove(index);
		    digest.update(undigestedData);
		}
	    }
	} else {

	    // otherwise store the piece until all data before this request
	    // has been loaded
	    undigestedBytes.put(new Integer(requestIndex), data);
	}
    }

    /**
     * return the hash of this downloaded piece
     */
    public byte[] getHash() {

	return digest.digest();
    }

    private synchronized void updateIndicies(int begin, int end) {

	int requestIndex = begin / requestPieceSize;
	bytesWritten += (end - begin);
	loaded[requestIndex] = true;
	activeCount--;
    }

    /**
     * returns the size of the chunk to be downloaded
     */
    public int getRequestPieceSize() {

	return requestPieceSize;
    }
}
