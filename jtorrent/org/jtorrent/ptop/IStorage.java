
package org.jtorrent.ptop;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * This interface is implementated by classes which load and store chunks
 * of data from the disk.
 *
 * @author Hunter Payne
 */
public interface IStorage {

    /**
     * Stores a piece of data to the disk
     * 
     * @param piece -- bytes to store
     * @param pieceIndex -- index of piece in which to store this data
     * @param begin -- relative offset inside of the piece where this data will
     * be stored
     */
    public void storePiece(byte[] piece, int pieceIndex, int begin) 
	throws IOException;

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
			  int begin, int length) throws IOException;

    /** returns the total number of bytes in this store */
    public long getStoreLength();

    /** sets the readOnly bit of this store */
    public void setReadOnly();

    /** flushes data to disk */
    public void flush() throws IOException;

    /** closes this store */
    public void close() throws IOException;
}
