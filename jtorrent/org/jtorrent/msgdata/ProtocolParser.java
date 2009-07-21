
package org.jtorrent.msgdata;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SocketChannel;
//
import org.jtorrent.Config;

/**
 * This classes parses bit torrrent protocol messages from a network socket
 *
 * @author Hunter Payne
 */
public class ProtocolParser implements IProtocolParser {

    private Socket sock;
    private SocketChannel channel;
    private ByteBuffer partialMessage;
    private IMessageReceiver receiver;
    private boolean handshakeReceived;
    private int maxMessageLength;
    private int numPieces;
    
    /**
     * Constructs a new parser object for this socket
     *
     * @param sock -- socket from which to read bit torrent messages
     * @param receiver -- object to notify when a protocol message is received
     * @param numPieces -- number of pieces in this download
     */
    public ProtocolParser(Socket sock, IMessageReceiver receiver, 
			  int numPieces) {

	channel = null;
	maxMessageLength = Integer.parseInt(Config.get("max_message_length"));
	this.sock = sock;
	this.receiver = receiver;
	this.numPieces = numPieces;
	partialMessage = null;
	handshakeReceived = false;
    }

    /**
     * Called when it is time to read messages from this socket
     */
    public void handleMessage() throws IOException {

	if (channel == null) channel = sock.getChannel();
	handleMessage(channel);
    }

    /**
     * called when it is time to read from the specified socket channel
     */
    protected void handleMessage(SocketChannel sc) throws IOException {

	if (handshakeReceived) {

	    ByteBuffer message = null;

	    // if we don't have any bytes left over from the last read
	    if (partialMessage == null) {
		
		ByteBuffer buf = ByteBuffer.allocate(4);
		int read = sc.read(buf);

		if (read != -1) {
		    
		    buf.flip();
		    int remaining = buf.remaining();

		    if (remaining < 4) {
			
			// can't really do anything???, wish i could push the 
			// bits back on the channel
			if (remaining > 0) System.err.println("push...push");
			return;
		    }
		    
		    // read length of the message
		    int length = buf.getInt();
		    
		    if (maxMessageLength < length) {
			
			(new IOException("message length " + length + 
					 " is too long")).printStackTrace();
			sock.close();
			return;
		    }
		    
		    if (length > 0) message = ByteBuffer.allocate(length);
		    
		} else return;
	    } else {
		
		message = partialMessage;
		partialMessage = null;
	    }
	    
	    if (message != null) {

		// read bytes of message
		int read = sc.read(message);
		//		System.out.println("read " + read + " bytes");
		
		// if we have read the entire message
		if (message.hasRemaining()) {
		
		    partialMessage = message;

		} else {
		    
		    partialMessage = null;
		    message.flip();

		    // parse message
		    handleMessage(message.array());

		    // handle any remaining bytes
		    handleMessage(sc);
		}
	    }

	    // read the handshake
	} else {

	    ByteBuffer message = null;

	    if (partialMessage == null)
		message = ByteBuffer.allocate(68);
	    else message = partialMessage;

	    int read = sc.read(message);
	    System.out.println("read handshake " + read);

	    // if we haven't finished reading the handshake
	    if (message.hasRemaining()) {

		partialMessage = message;

	    } else {

		partialMessage = null;
		message.flip();
		handleMessage(message.array());
		handleMessage(sc);
	    }
	}
    }

    /**
     * parses messages from the socket's raw bits 
     */
    protected void handleMessage(byte[] message) {

	if (handshakeReceived) {

	    try {

		// get type of message and notify the receiver object
		int type = (int)message[0];
		//		System.out.println("read message type " + type);

		if (type == CHOKE) {
		    
		    receiver.gotChoke();

		} else if (type == UNCHOKE) {

		    receiver.gotUnchoke();

		} else if (type == INTERESTED) {

		    receiver.gotInterested();

		} else if (type == NOT_INTERESTED) {

		    receiver.gotNotInterested();

		} else if (type == HAVE) {

		    ByteArrayInputStream bais = 
			new ByteArrayInputStream(message);
		    DataInputStream dis = new DataInputStream(bais);
		    dis.skipBytes(1);
		    receiver.gotHave(dis.readInt());

		} else if (type == BITFIELD) {
		    
		    boolean arr[] = new boolean[numPieces];
		    byte mask = (byte)-128;

		    for (int i = 1; i < message.length; i++) {

			byte b = message[i];

			for (int j = 0; j < 8 && (arr.length > 
						  8 * (i - 1) + j); j++) {
			
			    arr[(i - 1) * 8 + j] = ((b & mask) != 0);
			    b = (byte)(b << 1);
			}
		    }

		    System.out.println("got bitfield..." + message[1]);
		    //    System.out.println("from " + sock.getPort());
		    for (int i = 0; i < arr.length; i++)
			if (arr[i]) System.out.print("+");
			else System.out.print("-");
		    System.out.println("");
		    receiver.gotBitfield(arr);

		} else if (type == REQUEST) {

		    ByteArrayInputStream bais = 
			new ByteArrayInputStream(message);
		    DataInputStream dis = new DataInputStream(bais);
		    dis.skipBytes(1);
		    int index = dis.readInt();
		    int begin = dis.readInt();
		    int length = dis.readInt();
		    receiver.gotRequest(index, begin, length);

		} else if (type == PIECE) {

		    ByteArrayInputStream bais = 
			new ByteArrayInputStream(message);
		    DataInputStream dis = new DataInputStream(bais);
		    dis.skipBytes(1);
		    int index = dis.readInt();
		    int begin = dis.readInt();
		    byte piece[] = new byte[message.length - 9];
		    dis.read(piece);
		    receiver.gotPiece(index, begin, piece);

		} else if (type == CANCEL) {

		    ByteArrayInputStream bais = 
			new ByteArrayInputStream(message);
		    DataInputStream dis = new DataInputStream(bais);
		    dis.skipBytes(1);
		    int index = dis.readInt();
		    int begin = dis.readInt();
		    int length = dis.readInt();
		    receiver.gotCancel(index, begin, length);

		} else {

		    System.err.println("illegal message type " + type);
		}
	    } catch (UnsupportedEncodingException uee) {

		uee.printStackTrace();

	    } catch (NumberFormatException nfe) {

		nfe.printStackTrace();

	    } catch (IOException ioe) {

		// TODO handle this error
		ioe.printStackTrace();
	    }

	    // read the handshake
	} else {

	    try {

		int length = (int)message[0];

		// skip protocol name and eight reserved bytes and length byte
		int curr = length + 9;
		byte infoHash[] = new byte[20];

		// copy the info hash
		for (int i = 0; i < 20; i++)
		    infoHash[i] = message[i + curr];

		curr += 20;
		byte peerId[] = new byte[20];
		
		// copy the peer hash
		for (int i = 0; i < 20; i++)
		    peerId[i] = message[i + curr];

		// check the hashes and close the socket if either doesn't 
		// match
		if (!receiver.gotHandshake(infoHash, peerId)) {

		    System.err.println("didn't get handshake...");
		    sock.close();
		}

		handshakeReceived = true;
		System.out.println("got handshake message...");

	    } catch (IOException ioe) {

		ioe.printStackTrace();

		try {

		    System.err.println("closing socket!!!");
		    sock.close();

		} catch (IOException ioe2) { ioe2.printStackTrace(); }

	    } catch (NumberFormatException nfe) {

		try {

		    // TODO bad protocol
		    System.err.println("too bad " + nfe.getMessage());
		    nfe.printStackTrace();
		    sock.close();

		} catch (IOException ioe) { ioe.printStackTrace(); }
	    }
	}
    }
}
