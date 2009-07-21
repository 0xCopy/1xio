
package org.jtorrent.msgdata;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * This class formats and sends bit torrent protocol messages on a socket 
 * channel
 */
public class ProtocolMessageFormatter implements IProtocolFormatter {

    private Socket sock;
    private SocketChannel channel;

    /**
     * Makes a new message formatter
     *
     * @param sock -- send the bit torrent protocol messages on this socket
     */
    public ProtocolMessageFormatter(Socket sock) {

	this.sock = sock;
	channel = sock.getChannel();
    }

    /**
     * sets the socket used by this formatter object
     */
    public void setSocket(Socket s) {

	sock = s;
    }

    /**
     * sends a handshake message
     *
     * @param infoHash -- hash of the info part of the bencoded message
     * which configures this download, used for authorization
     * @param peerId -- id of this peer
     */
    public void sendHandshake(byte infoHash[], byte[] peerId) 
	throws IOException {

	ByteBuffer buf = ByteBuffer.allocate(28 + infoHash.length + 
					     peerId.length);
	buf.put((byte)19);
	buf.put(Utils.getBytes("BitTorrent protocol"));
	for (int i = 0; i < 8; i++) buf.put((byte)0);
	buf.put(infoHash);
	buf.put(peerId);
	channel.write((ByteBuffer)buf.flip());
    }

    /**
     * sends a keep alive message
     */
    public void sendKeepalive() throws IOException {

	ByteBuffer buf = ByteBuffer.allocate(4);
	buf.putInt(0);
	channel.write((ByteBuffer)buf.flip());
    }

    /**
     * sends a choke message
     */
    public void sendChoke() throws IOException {

	ByteBuffer buf = ByteBuffer.allocate(5);
	buf.putInt(1);
	buf.put((byte)IProtocolParser.CHOKE);
	channel.write((ByteBuffer)buf.flip());
    }

    /**
     * sends an unchoke message
     */
    public void sendUnchoke() throws IOException {

	ByteBuffer buf = ByteBuffer.allocate(5);
	buf.putInt(1);
	buf.put((byte)IProtocolParser.UNCHOKE);
	channel.write((ByteBuffer)buf.flip());
    }

    /**
     * sends an interested message
     */
    public void sendInterested() throws IOException {

	ByteBuffer buf = ByteBuffer.allocate(5);
	buf.putInt(1);
	buf.put((byte)IProtocolParser.INTERESTED);
	channel.write((ByteBuffer)buf.flip());
    }

    /**
     * sends a not interested message
     */
    public void sendNotInterested() throws IOException {

	ByteBuffer buf = ByteBuffer.allocate(5);
	buf.putInt(1);
	buf.put((byte)IProtocolParser.NOT_INTERESTED);
	channel.write((ByteBuffer)buf.flip());
    }

    /**
     * sends a have message
     *
     * @param index -- piece index which we have just finished downloading
     */
    public void sendHave(int index) throws IOException {

	ByteBuffer buf = ByteBuffer.allocate(9);
	buf.putInt(5);
	buf.put((byte)IProtocolParser.HAVE);
	buf.putInt(index);
	channel.write((ByteBuffer)buf.flip());
    }

    /**
     * sends a bitfield message
     *
     * @param bitfield -- the boolean array which specifies which pieces this
     * peer has successfully downloaded
     */
    public int sendBitfield(boolean[] bitfield) throws IOException {

	int length = bitfield.length / 8;
	if (bitfield.length % 8 != 0) length++;

	byte arr[] = new byte[length];

	for (int i = 0; i < length; i++) arr[i] = 0;

	// construct the bytes of the message payload
	for (int i = bitfield.length - 1; i >= 0; i--) 
	    arr[i / 8] = shiftRight(arr[i / 8], bitfield[i]);

	ByteBuffer buf = ByteBuffer.allocate(arr.length + 5);
	buf.putInt(arr.length + 1);
	buf.put((byte)IProtocolParser.BITFIELD);
	buf.put(arr);
	channel.write((ByteBuffer)buf.flip());
	System.out.println("sent bitfield " + arr[0]);
	//	System.out.println("to " + sock.getPort());
	for (int i = 0; i < bitfield.length; i++)
	    if (bitfield[i]) System.out.print("+");
	    else System.out.print("-");
	System.out.println("");
	return arr.length + 1;
    }

    /**
     * sends a request message
     *
     * @param index -- index of the piece this message is requesting
     * @param begin -- where in the piece to start copying information
     * @param length -- number of bytes to download
     */
    public void sendRequest(int index, int begin, int length) 
	throws IOException {

	ByteBuffer buf = ByteBuffer.allocate(17);
	buf.putInt(13);
	buf.put((byte)IProtocolParser.REQUEST);
	buf.putInt(index);
	buf.putInt(begin);
	buf.putInt(length);
	channel.write((ByteBuffer)buf.flip());
    }

    /**
     * sends a cancel message
     *
     * @param index -- value of the piece index of the request to cancel
     * @param begin -- value of the offset index of the request to cancel
     * @param length -- length of the request to cancel
     */
    public void sendCancel(int index, int begin, int length) 
	throws IOException {

	ByteBuffer buf = ByteBuffer.allocate(17);
	buf.putInt(13);
	buf.put((byte)IProtocolParser.CANCEL);
	buf.putInt(index);
	buf.putInt(begin);
	buf.putInt(length);
	channel.write((ByteBuffer)buf.flip());
    }

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
    public void startSendPiece(int index, int begin, byte[] piece, 
			       int writeLength) throws IOException {

	ByteBuffer buf = ByteBuffer.allocate(13 + piece.length);
	buf.putInt(9 + piece.length);
	buf.put((byte)IProtocolParser.PIECE);
	buf.putInt(index);
	buf.putInt(begin);
	//	buf.position(13);
	buf.put(piece, 0, writeLength);
	channel.write((ByteBuffer)buf.flip());
    }

    /**
     * writes some bytes into the socket
     *
     * @param piece -- data to write
     * @param offset -- where to start reading the data from the byte array
     * @param len -- amount of data to write
     */
    public void writeBytes(byte piece[], int offset, int len) 
	throws IOException {

	ByteBuffer buf = ByteBuffer.allocate(len);
	buf.put(piece, offset, len);
	channel.write((ByteBuffer)buf.flip());
    }

    // because of how Java does it's bit shift operators(ie, convert to int, 
    // perform op, convert back to byte) this method will handle the
    // operation by clumsly shifting the bytes to the right and setting
    // the 2 left most bits by hand if necessary 
    static byte shiftRight(byte b, boolean addOne) {

	boolean topBit = (b < 0);
	b = (byte)(b >> 1);

	if (!topBit && addOne) {

	    b = (byte)(b | -128);
	    if (b < 0) return b;
	    //	    System.err.println("correcting " + b);
	    return (byte)(b | -128);

	} else if (topBit && addOne) {

	    b = (byte)(b | -64);
	    if (b < 0) return b;
	    //	    System.err.println("correcting " + b);
	    return (byte)(b | -128);

	} else if (topBit && !addOne) {

	    b = (byte)(b | 64);
	    if (b > 0) return b;
	    //	    System.err.println("correcting " + b);
	    return (byte)(b & 127);

	} else { // if (!topBit && !addOne) {

	    b = (byte)(b & 63);
	    if (b > 0) return b;
	    //	    System.err.println("correcting " + b);
	    return (byte)(b & 127);
	}
    }
    /*
    public static void main(String args[]) {

	boolean bitfield[] = new boolean[6];
	for (int i = 0; i < 6; i++) bitfield[i] = false;
	bitfield[1] = true;

	int length = bitfield.length / 8;
	if (bitfield.length % 8 != 0) length++;

	byte arr[] = new byte[length];

	for (int i = 0; i < length; i++) arr[i] = 0;

	for (int i = bitfield.length - 1; i >= 0; i--) {

	    arr[i / 8] = shiftRight(arr[i / 8], bitfield[i]);
	    System.out.println("after shift " + bitfield[i] + ":" + 
			       arr[i / 8]);
	}

	System.out.println("bitfield: ");

	for (int i = 0; i < arr.length; i++) {

	    System.out.print(arr[i]);
	    System.out.print(" ");
	}

	System.out.println("");
	}*/
}
