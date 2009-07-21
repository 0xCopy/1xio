
package org.jtorrent.ptop;

import java.net.*;
import java.io.InputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.HashMap;
import java.util.Date;

import org.jtorrent.msgdata.*;
//
import org.jtorrent.Config;
import org.jtorrent.Stats;

/**
	* This class wraps a socket object and handles the reading from and writing 
	* to the socket.  It buffers the writes in an internal buffer so they can be
	* canceled at any time. It is also the class which is manipulated by 
	* the rest of the code to control the connection to a peer.  It uses the
	* code in the org.jtorrent.msgdata to read from and write to the socket.
	*
	* @author Hunter Payne, Dave Blau
	* @version 0.2
	*/
public class Connection implements IMessageReceiver
{
	// Types of connection
	public static final int   UPLOADER = 0;
	public static final int DOWNLOADER = 1;

	private ConnectionRate ulRate;
	private ConnectionRate dlRate;

	private SingleDownloader downloader;
	private Uploader uploader;
	private Socket socket;
	private IProtocolParser parser;
	private IProtocolFormatter formatter;
	private ConnectionManager manager;
    	private Vector writeQueue;
    	private byte writingPiece[];
    	private int currentPieceOffset;
	private HashCheckStore store;
	private boolean bitfield[];
	private byte peerId[];
	private int timeout;
	private long lastMessage;
	private boolean handshakeSent;
	//
	private int statsNo;
	private Stats stats;

	/**
	* creates a new connection object.  Only called by the ConnectionManager
	* <br/><em>Use ConnectionManager.getConnection() instead</em>
	*
	* @param socket -- socket to read from and write to
	* @param manager -- object which manages this peer's connection
	* @param downloader -- object which controls the download
	* @param choker -- object which chokes connections
	* @param store -- object which controls access to the downloaded file
	* message
	*/
	public Connection(Socket socket, ConnectionManager manager, 
		      Downloader downloader, Choker choker, 
		      HashCheckStore store )
	{
	handshakeSent = false;
	if (downloader != null) 
		this.downloader = downloader.makeDownloader(this);
	else this.downloader = null;

	lastMessage = (new Date()).getTime();
	this.timeout = 1000 * Integer.parseInt(Config.get("timeout"));
	this.manager = manager;
	this.store = store;

	if (downloader != null) {

		bitfield = new boolean[downloader.getNumPieces()];
		for (int i = 0; i < bitfield.length; i++) bitfield[i] = false;

	} else {

		bitfield = new boolean[store.getNumPieces()];
		for (int i = 0; i < bitfield.length; i++) bitfield[i] = true;
	}

	currentPieceOffset = -1;
	this.socket = socket;
	parser = new ProtocolParser(socket, this, store.getNumPieces());
	formatter = new ProtocolMessageFormatter(socket);
	writeQueue = new Vector();
	uploader = new Uploader(store, this, choker);
	choker.connectionMade(this);
	System.out.println("made connection w/ socket " + socket);
//
	statsNo = Stats.newSub( Stats.CONN );
	stats   = Stats.getSub( Stats.CONN, statsNo );

	ulRate = new ConnectionRate( statsNo, UPLOADER );
	dlRate = new ConnectionRate( statsNo, DOWNLOADER );

	}

	/**
	* accessor for the socket
	*/
	public Socket getSocket() {

	return socket;
	}

	/**
	* accessor for the object controlling the download on this connection
	*/
	public SingleDownloader getDownloader() { return downloader; }

	/**
	* accessor for the uploader object
	*/
	public Uploader getUploader() { return uploader; }

	/**
	* closes this socket
	*/
	public void close() {

	try {

		uploader.close();
		if (downloader != null) downloader.disconnected();
		manager.unregisterConnection(peerId);
		socket.close();

	} catch (Exception e) { e.printStackTrace(); }
	}

	/**
	* check if this connection has timedout yet
	*
	* @return true if this connection hasn't yet timedout
	*/
	public boolean isAlive() {

	return ((new Date()).getTime() < timeout + lastMessage);
	}

	/**
	* called when this socket has bytes to read
	*/
	public void handleRead() throws IOException {

	lastMessage = (new Date()).getTime();
	parser.handleMessage();

	//	System.out.println("pinging last message " + socket.getPort());

	// for very long reads, reset the message timer
	lastMessage = (new Date()).getTime();
	}

	/**
	* called when this socket is ready to write
	*/
    // TODO move the manager.incUpload/incDownload calls to the ProtocolDelegate
	public void writeNext(int numBytes) throws IOException {

	int bytesWritten = 0;
	if (!handshakeSent) {

		formatter.sendHandshake(manager.getInfoHash(), 
				    manager.getPeerId());
		handshakeSent = true;

		bytesWritten += 63;

		if ((writeQueue.size() > 0) && 
		(writeQueue.get(0) instanceof HashMap))
		writeQueue.remove(0);
	}

	//	bytesWritten = buffer.write(numBytes - bytesWritten);

	// write as much of the rest of the piece as we can
	if (currentPieceOffset != -1) {

		int left = writingPiece.length - currentPieceOffset;

		if (left > numBytes) {

		formatter.writeBytes(writingPiece, currentPieceOffset, 
				     numBytes);
		manager.incUploaded(numBytes);
		currentPieceOffset += numBytes;
		bytesWritten = numBytes;

		} else {

		formatter.writeBytes(writingPiece, currentPieceOffset, left);
		manager.incUploaded(left);
		writingPiece = null;
		currentPieceOffset = -1;
		bytesWritten = left;
		}
	}

	Object curr = null;

	// while we have messages to write and we haven't yet written enough
	// bytes
	while ((bytesWritten < numBytes) && (writeQueue.size() > 0) &&
		((curr = writeQueue.remove(0)) != null)) {

		if (curr instanceof Integer) {

		int value = ((Integer)curr).intValue();

		if (value == IProtocolParser.CHOKE) {

		    formatter.sendChoke();
		    bytesWritten += 2;

		} else if (value == IProtocolParser.UNCHOKE) {

		    formatter.sendUnchoke();
		    bytesWritten += 2;

		} else if (value == IProtocolParser.INTERESTED) {

		    formatter.sendInterested();
		    bytesWritten += 2;

		} else if (value == IProtocolParser.NOT_INTERESTED) {

		    formatter.sendNotInterested();
		    bytesWritten += 2;

		} else if (value == 9) {

		    formatter.sendKeepalive();
		    bytesWritten++;
		}
		} else if (curr instanceof boolean[]) {

		bytesWritten += formatter.sendBitfield((boolean[])curr);

		} else if (curr instanceof int[]) {

		int data[] = (int[])curr;

		if (data.length == 1) {

		    formatter.sendHave(data[0]);
		    bytesWritten += 5;

		} else if (data.length == 4) {

		    if (data[0] == IProtocolParser.REQUEST) {

			formatter.sendRequest(data[1], data[2], data[3]);
			bytesWritten += 13;

		    } else if (data[0] == IProtocolParser.CANCEL) {

			formatter.sendCancel(data[1], data[2], data[3]);
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
		    int maxWrite = numBytes - bytesWritten;

		    if (maxWrite < piece.length) {

			writingPiece = piece;
			currentPieceOffset = maxWrite;
			formatter.startSendPiece(index, begin, piece, 
						 maxWrite);
			manager.incUploaded(maxWrite);
			
		    } else {

			formatter.startSendPiece(index, begin, piece, 
						 piece.length);
			manager.incUploaded(piece.length);
		    }

		    bytesWritten = numBytes;

		} else {

		    byte infoHash[] = (byte[])map.get("info hash");
		    byte peerId[] = (byte[])map.get("peer id");
		    System.out.println("sending handshake...");
		    formatter.sendHandshake(infoHash, peerId);
		    handshakeSent = true;
		    bytesWritten += 63;
		}
		}
	}

	}

	/**
	* the cached bitfield returned by the peer at the other end of this socket
	*/
	public boolean[] getBitfield() {

	return bitfield;
	}

	/**
	* schedules a handshake message on this socket
	*/
	public void sendHandshake(byte infoHash[], byte[] peerId) 
	throws IOException {

	HashMap map = new HashMap();
	map.put("info hash", infoHash);
	map.put("peer id", peerId);
	writeQueue.add(0, map);
	}

	/**
	* schedules this connection to send a choke message
	*/
	public void sendChoke() {

	writeQueue.add(new Integer(IProtocolParser.CHOKE));
	}

	/**
	* schedules this connection to send an unchoke message
	*/
	public void sendUnchoke() {

	writeQueue.add(new Integer(IProtocolParser.UNCHOKE));
	}

	/**
	* schedules this connection to send an interested message
	*/
	public void sendInterested() {

	writeQueue.add(new Integer(IProtocolParser.INTERESTED));
	}

	/**
	* schedules this connection to send a not interested message
	*/
	public void sendNotInterested() {

	writeQueue.add(new Integer(IProtocolParser.NOT_INTERESTED));
	}

	/**
	* schedules this connection to send a keep alive message
	*/
	public void sendKeepalive() {

	writeQueue.add(new Integer(9));
	}

	/**
	* schedules this connection to send a bitfield message
	*
	* @param bitfield -- the boolean array which specifies which pieces this
	* peer has successfully downloaded
	*/
	public void sendBitfield(boolean[] bitfield) {

	writeQueue.add(bitfield);
	}

	/**
	* schedules this connection to send a have message
	*
	* @param index -- piece index which we have just finished downloading
	*/
	public void sendHave(int index) {

	int arr[] = new int[1];
	arr[0] = index;
	writeQueue.add(arr);
	}

	/**
	* schedules this connection to send a choke message
	*
	* @param index -- index of the piece this message is requesting
	* @param begin -- where in the piece to start copying information
	* @param length -- number of bytes to download
	*/
	public void sendRequest(int index, int begin, int length) {

	int arr[] = new int[4];
	arr[0] = IProtocolParser.REQUEST;
	arr[1] = index;
	arr[2] = begin;
	arr[3] = length;
	writeQueue.add(arr);
	}

	/**
	* schedules this connection to send a choke message
	*
	* @param index -- value of the piece index of the request to cancel
	* @param begin -- value of the offset index of the request to cancel
	* @param length -- length of the request to cancel
	*/
	public void sendCancel(int index, int begin, int length) {

	int arr[] = new int[4];
	arr[0] = IProtocolParser.CANCEL;
	arr[1] = index;
	arr[2] = begin;
	arr[3] = length;
	writeQueue.add(arr);
	}

	/**
	* schedules this connection to send a choke message
	*
	* @param index -- index of the piece this message is carrying
	* @param begin -- offset index of the data in this message
	* @param piece -- data we are uploading
	*/
	public void sendPiece(byte[] piece, int index, int begin) {

	HashMap map = new HashMap();
	map.put("piece", piece);
	map.put("index", new Integer(index));
	map.put("begin", new Integer(begin));
	writeQueue.add(map);
	}

	/**
	* called when a handshake message is received from the socket,
	* this is used to authorize a connection
	* 
	* @param infoHash -- hash of the bencoded message in which the peer is 
	* interested
	* @param peerId -- id of the peer on the other side of the connection
	* @return true if this info hash was valid
	*/
	public boolean gotHandshake(byte[] infoHash, byte[] peerId) {

	byte myInfoHash[] = manager.getInfoHash();
	System.out.println("got handshake from " + peerId);

	if ((infoHash.length == 20) && (peerId.length == 20) && 
		(myInfoHash.length == 20)) {

		for (int i = 0; i < infoHash.length; i++) 
		if (myInfoHash[i] != infoHash[i]) return false;
		
		this.peerId = peerId;
		manager.registerConnection(peerId, this);
		return true;
	}

	return false;
	}

	/**
	* called when a keep alive message is received
	*/
	public void gotKeepalive() {
	}

	/**
	* called when a choke message is received
	*/
	public void gotChoke() {

	writeQueue.clear();
	if (downloader != null) downloader.gotChoke();
	}

	/**
	* called when an unchoke message is received
	*/
	public void gotUnchoke() {

	if (downloader != null) downloader.gotUnchoke();
	}

	/**
	* called when an interested message is received
	*/
	public void gotInterested() {

	uploader.gotInterested();
	}

	/**
	* called when a not interested message is received
	*/
	public void gotNotInterested() {

	uploader.gotNotInterested();
	}

	/**
	* called when a have message is received
	*
	* @param index -- which piece the peer at the other side of the connection
	* has just finished downloading
	*/
	public void gotHave(int index) {

	bitfield[index] = true;
	if (downloader != null) downloader.gotHave(index);
	}

	/**
	* called when a bitfield message is received
	*
	* @param bitfield -- the boolean array which specifies which pieces the
	* peer at the other end of this connection has successfully downloaded
	*/
	public void gotBitfield(boolean[] bitfield) {

	this.bitfield = bitfield;
	if (downloader != null) downloader.gotBitfield(bitfield);
	}

	/**
	* called when a request message is received
	*
	* @param index -- index of the piece this message is requesting
	* @param begin -- where in the piece to start copying information
	* @param length -- number of bytes to download
	*/
	public void gotRequest(int index, int begin, int length) {

	uploader.gotRequest(index, begin, length);
	}

	/**
	* called when a cancel message is received
	*
	* @param index -- value of the piece index of the request to cancel
	* @param begin -- value of the offset index of the request to cancel
	* @param length -- length of the request to cancel
	*/
	public void gotCancel(int index, int begin, int length) {

	    // buffer.cancelMessage(index, begin, length);

	for (int i = 0; i < writeQueue.size(); i++) {

		Object o = writeQueue.get(i);
		
		if (o instanceof HashMap) {

		HashMap map = (HashMap)o;
		int mapIndex = ((Integer)map.get("index")).intValue();
		int mapBegin = ((Integer)map.get("begin")).intValue();
		byte piece[] = (byte[])map.get("piece");

		// remove write requests from the application write buffer
		if ((mapIndex == index) && (mapBegin == begin) && 
		    (piece.length == length)) writeQueue.remove(o);
		else if ((mapIndex == index) && (mapBegin == begin)) {

		    System.err.println("WARNING: unable to cancel because the piece length didn't match " + index + ":" + begin + ":" + length + ":" + piece.length);
		}
		}
	}
	}

	/**
	* called when a piece message is received
	*
	* @param index -- index of the piece this message is carrying
	* @param begin -- offset index of the data in this message
	* @param piece -- data we are downloading
	*/
	public void gotPiece(int index, int begin, byte piece[])
	{
		manager.incDownloaded(piece.length);
		dlRate.updateRate( piece.length );
		if (downloader != null)
			downloader.gotPiece(index, begin, piece);
	}

	/**
	* returns the download rate of this connection
	*/
	public double getRate()
	{
		return dlRate.getRate();
	}

	public double getUploadRate()
	{
		return ulRate.getRate();
	}
}
