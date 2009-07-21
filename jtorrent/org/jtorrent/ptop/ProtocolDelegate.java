
package org.jtorrent.ptop;

import java.io.IOException;

import org.jtorrent.msgdata.IProtocolFormatter;

/**
 * This class routes protocol messages from the ProtocolParser to the SingleDownloader and Uploader
 * classes.  It is executed on the Disk IO/Downloader thread.  There is one protocol delegate
 * per connection.
 *
 * @author Hunter Payne
 */
public class ProtocolDelegate implements IProtocolFormatter {

    private SingleDownloader downloader;
    private Uploader uploader;
    private boolean bitfield[]; // download state of peer at the other end of the connection
    // this object receives messages for

    /**
     * Creates a new protocol delegate.
     * 
     * @param downloader -- object responsible for managing the download of data on one connection
     * @param uploader -- object responsible for managing the upload of data on one connection
     * @param numPieces -- number of pieces in the torrent file(s)
     */
    public ProtocolDelegate(SingleDownloader downloader, Uploader uploader, int numPieces) {

	this.downloader = downloader;
	this.uploader = uploader;
	bitfield = new boolean[numPieces];

	if (downloader != null) for (int i = 0; i < bitfield.length; i++) bitfield[i] = false;
	else for (int i = 0; i < bitfield.length; i++) bitfield[i] = true;
    }

    /**
     * throws a Runtime exception because the handshake should be handled by the ConnectionFactory
     *
     * @param infoHash -- hash of the info part of the bencoded message
     * which configures this download, used for authorization
     * @param peerId -- id of this peer
     */
    public void sendHandshake(byte infoHash[], byte[] peerId) throws IOException {

	throw new RuntimeException("why am I getting a handshake now???");
    }

    /**
     * does nothing
     */
    public void sendKeepalive() throws IOException {
    }

    /**
     * sends a choke message to the downloader
     */
    public void sendChoke() throws IOException {

	// TODO clear connection send buffer
	if (downloader != null) downloader.gotChoke();
    }

    /**
     * sends an unchoke message to the downloader
     */
    public void sendUnchoke() throws IOException {

	if (downloader != null) downloader.gotChoke();
    }

    /**
     * sends an interested message to the uploader
     */
    public void sendInterested() throws IOException {

	uploader.gotInterested();
    }

    /**
     * sends a not interested message to the uploader
     */
    public void sendNotInterested() throws IOException {

	uploader.gotNotInterested();
    }

    /**
     * sends a have message to the downloader
     *
     * @param index -- piece index which we have just finished downloading
     */
    public void sendHave(int index) throws IOException {

	bitfield[index] = true;
	if (downloader != null) downloader.gotHave(index);
    }

    /**
     * sends a bitfield message to the downloader
     *
     * @param bitfield -- the boolean array which specifies which pieces this
     * peer has successfully downloaded
     */
    public int sendBitfield(boolean bitfield[]) throws IOException {
	
	this.bitfield = bitfield;
	if (downloader != null) downloader.gotBitfield(bitfield);
	return -1; // return value doesn't matter here since it will get called during a flush
	// not during a write
    }

    /**
     * sends a request message to the uploader
     *
     * @param index -- index of the piece this message is requesting
     * @param begin -- where in the piece to start copying information
     * @param length -- number of bytes to download
     */
    public void sendRequest(int index, int begin, int length) throws IOException {

	uploader.gotRequest(index, begin, length);
    }	

    /**
     * throws a runtime exception because cancel messages are handled by removing a message from
     * a ProtocolMessageBuffer
     *
     * @param index -- value of the piece index of the request to cancel
     * @param begin -- value of the offset index of the request to cancel
     * @param length -- length of the request to cancel
     */
    public void sendCancel(int index, int begin, int length) throws IOException {
	
	throw new RuntimeException("why am I getting a sendCancel() in ProtocolDelegate");
    }

    /**
     * passes an entire chunk to the downloader so it can be stored to disk
     *
     * @param index -- index of the piece this message is carrying
     * @param begin -- offset index of the data in this message
     * @param piece -- data we are uploading
     * @param writeLength -- amount of the message to write
     */
    public void startSendPiece(int index, int begin, byte piece[], int writeLength) 
	throws IOException {

	if (downloader != null) downloader.gotPiece(index, begin, piece);
    }   

    /**
     * throws a runtime exception because entire chunks are passed one at a time, instead of 
     * a set number of bytes at a time
     *
     * @param piece -- data to write
     * @param offset -- where to start reading the data from the byte array
     * @param len -- amount of data to write
     */
    public void writeBytes(byte piece[], int offset, int len) throws IOException {
	
	throw new RuntimeException("why am I getting a writeBytes() in ProtocolDelegate");
    }
}
