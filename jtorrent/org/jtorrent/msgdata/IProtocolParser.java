
package org.jtorrent.msgdata;

import java.io.IOException;

/**
 * This interface is implemented by classes which can parse bit torrent 
 * protocol messages from somewhere, usually a socket
 */
public interface IProtocolParser {

    /** Constant representing a choke message */
    public static final int CHOKE = 0;

    /** Constant representing an unchoke message */
    public static final int UNCHOKE = 1;

    /** Constant representing an interested message */
    public static final int INTERESTED = 2;

    /** Constant representing a not interested message */
    public static final int NOT_INTERESTED = 3;

    /** Constant representing a have message */
    public static final int HAVE = 4;

    /** Constant representing a bitfield message */
    public static final int BITFIELD = 5;

    /** Constant representing a request message */
    public static final int REQUEST = 6;

    /** Constant representing a piece message */
    public static final int PIECE = 7;

    /** Constant representing a cancel message */
    public static final int CANCEL = 8;

    /** called when we are ready to read data */
    public void handleMessage() throws IOException;
}
