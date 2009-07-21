
package org.jtorrent.msgdata;

import java.io.IOException;

/**
 * Interface implemented by a class which writes bit torrent protocol messages
 * to a socket
 * 
 * @author Hunter Payne
 */
public interface IProtocolFormatter {

    /**
     * sends a handshake message
     *
     * @param infoHash -- hash of the info part of the bencoded message
     * which configures this download, used for authorization
     * @param peerId -- id of this peer
     */
    public void sendHandshake(byte infoHash[], byte[] peerId) 
	throws IOException;

    /**
     * sends a keep alive message
     */
    public void sendKeepalive() throws IOException;

    /**
     * sends a choke message
     */
    public void sendChoke() throws IOException;

    /**
     * sends an unchoke message
     */
    public void sendUnchoke() throws IOException;

    /**
     * sends an interested message
     */
    public void sendInterested() throws IOException;

    /**
     * sends a not interested message
     */
    public void sendNotInterested() throws IOException;

    /**
     * sends a have message
     *
     * @param index -- piece index which we have just finished downloading
     */
    public void sendHave(int index) throws IOException;

    /**
     * sends a bitfield message
     *
     * @param bitfield -- the boolean array which specifies which pieces this
     * peer has successfully downloaded
     */
    public int sendBitfield(boolean bitfield[]) throws IOException;

    /**
     * sends a request message
     *
     * @param index -- index of the piece this message is requesting
     * @param begin -- where in the piece to start copying information
     * @param length -- number of bytes to download
     */
    public void sendRequest(int index, int begin, int length) 
	throws IOException;

    /**
     * sends a cancel message
     *
     * @param index -- value of the piece index of the request to cancel
     * @param begin -- value of the offset index of the request to cancel
     * @param length -- length of the request to cancel
     */
    public void sendCancel(int index, int begin, int length) 
	throws IOException;

    /**
     * sends a piece message, since this message is likely much longer than
     * we should write to a socket at once, this functionality is split into
     * two methods, startSendPiece and writeBytes which will be called 
     * multiple times until this message has been completely written
     *
     * @param index -- index of the piece this message is carrying
     * @param begin -- offset index of the data in this message
     * @param piece -- data we are uploading
     * @param writeLength -- amount of the message to write
     */
    public void startSendPiece(int index, int begin, byte piece[], 
			       int writeLength) throws IOException;

    /**
     * writes some bytes into the socket
     *
     * @param piece -- data to write
     * @param offset -- where to start reading the data from the byte array
     * @param len -- amount of data to write
     */
    public void writeBytes(byte piece[], int offset, int len) 
	throws IOException;
}
