
package org.jtorrent.ptop;

import java.io.IOException;

/**
 * This exception is thrown when a piece is corrupted and its hash doesn't
 * match the value in the .torrent file
 *
 * @author Hunter Payne
 */
public class InvalidHashException extends IOException {

    /**
     * Creates a new exception
     *
     * @param pieceIndex -- index of the corrupted piece
     */
    public InvalidHashException(int pieceIndex) {

	super("Piece with index " + pieceIndex + " has an invalid hash");
    }
}
