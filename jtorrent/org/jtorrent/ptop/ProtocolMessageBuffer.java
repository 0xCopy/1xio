
package org.jtorrent.ptop;

import java.util.Vector;
import java.util.HashMap;
import java.io.IOException;

import org.jtorrent.msgdata.IProtocolFormatter;
import org.jtorrent.msgdata.IMessageReceiver;
import org.jtorrent.msgdata.IProtocolParser;

/**
 * This class buffers Bittorrent Protocol messages in a thread-safe manor.  This class is used
 * by the Connection object to buffer Protocol messages to and from the main IO thread.  As long
 * a no more than one thread calls write() and/or clear() at once on the same instance, this class 
 * is thread-safe.
 *
 * @author Hunter Payne
 */
public class ProtocolMessageBuffer implements IMessageReceiver {

    private Vector queue;
    private byte writingPiece[];
    private IProtocolFormatter receiver;
    private int currentPieceOffset;

    /**
     * Creates an empty message buffer
     * 
     * @param receiver -- where to send the protocol messages when flush or write is called
     */
    public ProtocolMessageBuffer(IProtocolFormatter receiver) {

	queue = new Vector();
	this.receiver = receiver;
	currentPieceOffset = -1;
	writingPiece = null;
    }

    /**
     * Sends some protocol messages in the buffer to the formatter object receiving messages from
     * this buffer<p />
     * won't send more than numBytes + 12 or numBytes + (numPieces / 8) - 1 bytes but usually will 
     * only send numBytes or less
     *
     * @param numBytes -- maximum number of protocol message bytes to write to the receiver object
     */
    // called by Connection.writeBytes()
    public int write(int numBytes) throws IOException {

	int bytesWritten = 0;

	// write as much of the rest of the piece as we can
	if (currentPieceOffset != -1) {

	    int left = writingPiece.length - currentPieceOffset;
	    
	    if (left > numBytes) {

		receiver.writeBytes(writingPiece, currentPieceOffset, numBytes);
		currentPieceOffset += numBytes;
		bytesWritten = numBytes;

	    } else {
		
		receiver.writeBytes(writingPiece, currentPieceOffset, left);
		writingPiece = null;
		currentPieceOffset = -1;
		bytesWritten = left;
	    }
	}

	Object curr = null;

	// while we have messages to write and we haven't yet written enough
	// bytes
	while ((bytesWritten < numBytes) && (queue.size() > 0) &&
	       ((curr = queue.remove(0)) != null)) {

	    if (curr instanceof Integer) {
		
		int value = ((Integer)curr).intValue();
		
		if (value == IProtocolParser.CHOKE) {
		    
		    receiver.sendChoke();
		    bytesWritten += 2;
		    
		} else if (value == IProtocolParser.UNCHOKE) {
		    
		    receiver.sendUnchoke();
		    bytesWritten += 2;

		} else if (value == IProtocolParser.INTERESTED) {

		    receiver.sendInterested();
		    bytesWritten += 2;

		} else if (value == IProtocolParser.NOT_INTERESTED) {

		    receiver.sendNotInterested();
		    bytesWritten += 2;

		} else if (value == 9) {

		    receiver.sendKeepalive();
		    bytesWritten++;
		}
	    } else if (curr instanceof boolean[]) {
		
		bytesWritten += receiver.sendBitfield((boolean[])curr);
		
	    } else if (curr instanceof int[]) {
		    
		int data[] = (int[])curr;
		
		if (data.length == 1) {
		    
		    receiver.sendHave(data[0]);
		    bytesWritten += 5;

		} else if (data.length == 4) {

		    if (data[0] == IProtocolParser.REQUEST) {

			receiver.sendRequest(data[1], data[2], data[3]);
			bytesWritten += 13;

		    } else if (data[0] == IProtocolParser.CANCEL) {

			receiver.sendCancel(data[1], data[2], data[3]);
			bytesWritten += 13;
		    }
		}
	    } else if (curr instanceof HashMap) {
		    
		HashMap map = (HashMap)curr;
		byte piece[] = (byte[])map.get("piece");

		if (piece != null) {

		    int index = ((Integer)map.get("index")).intValue();
		    int begin = ((Integer)map.get("begin")).intValue();
		    
		    // we can't write the entire
		    // piece out to the TCP buffer, because we might choke this
		    // connection and need to cancel this connection
		    int maxWrite = numBytes - bytesWritten - 13;

		    if ((maxWrite < piece.length) && (maxWrite > 0)) {

			writingPiece = piece;
			currentPieceOffset = maxWrite;
			receiver.startSendPiece(index, begin, piece, maxWrite);
			bytesWritten = numBytes;
			
		    } else {

			receiver.startSendPiece(index, begin, piece, piece.length);
			bytesWritten += piece.length;
		    }
		} else {

		    throw new RuntimeException("huh, what is this handshake message doing here");
		}
	    }
	}

	return bytesWritten;
    }

    /**
     * Sends all protocol messages in the buffer to the formatter object receiving messages from
     * this buffer
     */
    // called by Downloader on each SingleDownloader's message buffer
    public void flush() throws IOException {

	Object curr = null;

	// while we have messages to write and we haven't yet written enough
	// bytes
	while ((queue.size() > 0) && ((curr = queue.remove(0)) != null)) {

	    if (curr instanceof Integer) {
		
		int value = ((Integer)curr).intValue();
		
		if (value == IProtocolParser.CHOKE) {
		    
		    receiver.sendChoke();
		    
		} else if (value == IProtocolParser.UNCHOKE) {
		    
		    receiver.sendUnchoke();

		} else if (value == IProtocolParser.INTERESTED) {

		    receiver.sendInterested();

		} else if (value == IProtocolParser.NOT_INTERESTED) {

		    receiver.sendNotInterested();

		} else if (value == 9) {

		    receiver.sendKeepalive();
		}
	    } else if (curr instanceof boolean[]) {
		
		receiver.sendBitfield((boolean[])curr);
		
	    } else if (curr instanceof int[]) {
		    
		int data[] = (int[])curr;
		
		if (data.length == 1) {
		    
		    receiver.sendHave(data[0]);

		} else if (data.length == 4) {

		    if (data[0] == IProtocolParser.REQUEST) {

			receiver.sendRequest(data[1], data[2], data[3]);

		    } else if (data[0] == IProtocolParser.CANCEL) {

			receiver.sendCancel(data[1], data[2], data[3]);
		    }
		}
	    } else if (curr instanceof HashMap) {
		    
		HashMap map = (HashMap)curr;
		byte piece[] = (byte[])map.get("piece");

		if (piece != null) {

		    int index = ((Integer)map.get("index")).intValue();
		    int begin = ((Integer)map.get("begin")).intValue();
		    receiver.startSendPiece(index, 0, piece, piece.length);

		} else {

		    throw new RuntimeException("huh, what is this handshake message doing here");
		}
	    }
	}
    }

    /**
     * removes all messages from the buffer.  Call on the same thread as write().  No problems 
     * for calls to flush().
     */
    public void clear() {

	queue.clear();
	currentPieceOffset = -1;
	writingPiece = null;
    }
    
    /**
     * clears appropiate message(s) from this buffer when a cancel message is received
     *
     * @param index -- value of the piece index of the request to cancel
     * @param begin -- value of the offset index of the request to cancel
     * @param length -- length of the request to cancel
     */
    public void cancelMessage(int index, int begin, int length) {
	
	for (int i = 0; i < queue.size(); i++) {
	    
	    Object o = queue.get(i);
	    
	    if (o instanceof HashMap) {
		
		HashMap map = (HashMap)o;
		int mapIndex = ((Integer)map.get("index")).intValue();
		int mapBegin = ((Integer)map.get("begin")).intValue();
		byte piece[] = (byte[])map.get("piece");
		
		// remove write requests from the application write buffer
		if ((mapIndex == index) && (mapBegin == begin) && 
		    (piece.length == length)) queue.remove(o);
		else if ((mapIndex == index) && (mapBegin == begin)) {
		    
		    System.err.println("WARNING: unable to cancel because the piece length " + 
				       "didn't match " + 
				       index + ":" + begin + ":" + length + ":" + piece.length);
		}
	    }
	}
    }

    /**
     * called when a handshake message is received by the buffer
     * 
     * @param infoHash -- hash of the bencoded message in which the peer is 
     * interested
     * @param peerId -- id of the peer on the other side of the connection
     * @return true if this info hash was valid
     */
    public boolean gotHandshake(byte infoHash[], byte[] peerId) {
	
	throw new RuntimeException("second gotHandshake");
    }
    
    /**
     * schedules the buffer to send a choke message
     */
    public void gotChoke() {
	
	queue.add(new Integer(IProtocolParser.CHOKE));
    }
    
    /**
     * schedules the buffer to send an unchoke message 
     */
    public void gotUnchoke() {
	
	queue.add(new Integer(IProtocolParser.UNCHOKE));
    }
    
    /**
     * schedules the buffer to send an interest message 
     */
    public void gotInterested() {
	
	queue.add(new Integer(IProtocolParser.INTERESTED));
    }

    /**
     * schedules the buffer to send a not interested message 
     */
    public void gotNotInterested() {

	queue.add(new Integer(IProtocolParser.NOT_INTERESTED));
    }

    /**
     * schedules the buffer to send a keep alive message 
     */
    public void gotKeepalive() {

	queue.add(new Integer(9));
    }

    /**
     * schedules the buffer to send a bitfield message 
     *
     * @param bitfield -- the boolean array which specifies which pieces this
     * peer has successfully downloaded
     */
    public void gotBitfield(boolean[] bitfield) {

	queue.add(bitfield);
    }

    /**
     * schedules this buffer to send a have message
     *
     * @param index -- piece index which we have just finished downloading
     */
    public void gotHave(int index) {

	int arr[] = new int[1];
	arr[0] = index;
	queue.add(arr);
    }

    /**
     * schedules this buffer to send a request message
     *
     * @param index -- index of the piece this message is requesting
     * @param begin -- where in the piece to start copying information
     * @param length -- number of bytes to download
     */
    public void gotRequest(int index, int begin, int length) {

	int arr[] = new int[4];
	arr[0] = IProtocolParser.REQUEST;
	arr[1] = index;
	arr[2] = begin;
	arr[3] = length;
	queue.add(arr);
    }

    /**
     * schedules this buffer to send a cancel message
     *
     * @param index -- value of the piece index of the request to cancel
     * @param begin -- value of the offset index of the request to cancel
     * @param length -- length of the request to cancel
     */
    public void gotCancel(int index, int begin, int length) {

	int arr[] = new int[4];
	arr[0] = IProtocolParser.CANCEL;
	arr[1] = index;
	arr[2] = begin;
	arr[3] = length;
	queue.add(arr);
    }

    /**
     * schedules this buffer to send a piece message
     *
     * @param index -- index of the piece this message is carrying
     * @param begin -- offset index of the data in this message
     * @param piece -- data we are uploading
     */
    public void gotPiece(int index, int begin, byte piece[]) {

	HashMap map = new HashMap();
	map.put("piece", piece);
	map.put("index", new Integer(index));
	map.put("begin", new Integer(begin));
	queue.add(map);
    }
}
