
package org.jtorrent.msgdata;

/**
 * This event listener interface provides the ability to monitor and authorize
 * messages received from a socket using the bit torrent network protocol.
 * 
 * @author Hunter Payne
 */
public interface IMessageReceiver {

    /**
     * called when a handshake message is received from the socket,
     * this is used to authorize a connection
     * 
     * @param infoHash -- hash of the bencoded message in which the peer is 
     * interested
     * @param peerId -- id of the peer on the other side of the connection
     * @return true if this info hash was valid
     */
    public boolean gotHandshake(byte infoHash[], byte[] peerId);

    /**
     * called when a keep alive message is received
     */
    public void gotKeepalive();

    /**
     * called when a choke message is received
     */
    public void gotChoke();

    /**
     * called when an unchoke message is received
     */
    public void gotUnchoke();

    /**
     * called when an interested message is received
     */
    public void gotInterested();

    /**
     * called when a not interested message is received
     */
    public void gotNotInterested();

    /**
     * called when a have message is received
     *
     * @param index -- which piece the peer at the other side of the connection
     * has just finished downloading
     */
    public void gotHave(int index);

    /**
     * called when a bitfield message is received
     *
     * @param bitfield -- the boolean array which specifies which pieces the
     * peer at the other end of this connection has successfully downloaded
     */
    public void gotBitfield(boolean bitfield[]);

    /**
     * called when a request message is received
     *
     * @param index -- index of the piece this message is requesting
     * @param begin -- where in the piece to start copying information
     * @param length -- number of bytes to download
     */
    public void gotRequest(int index, int begin, int length);

    /**
     * called when a cancel message is received
     *
     * @param index -- value of the piece index of the request to cancel
     * @param begin -- value of the offset index of the request to cancel
     * @param length -- length of the request to cancel
     */
    public void gotCancel(int index, int begin, int length);

    /**
     * called when a piece message is received
     *
     * @param index -- index of the piece this message is carrying
     * @param begin -- offset index of the data in this message
     * @param piece -- data we are downloading
     */
    public void gotPiece(int index, int begin, byte piece[]);
}
