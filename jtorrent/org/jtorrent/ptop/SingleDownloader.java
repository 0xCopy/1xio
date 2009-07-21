
package org.jtorrent.ptop;

import java.util.ArrayList;
import java.util.Random;
import java.util.Date;
import java.io.IOException;
import java.net.InetAddress;
//
import org.jtorrent.Config;

/**
 * This is a Java port of the SingleDownloader object of the Downloader.py
 * module of BitTorrent 3.0.2
 */
public class SingleDownloader {

    private boolean choked;
    private boolean interested;
    private Downloader downloader;
    private PriorityBitField priorities;
    private Connection connection;
    private ArrayList requests;
    private boolean bitfield[];
    private boolean endgame;

    public SingleDownloader(Downloader downloader, Connection con) {

	endgame = downloader.getStore().allPending();
	bitfield = new boolean[downloader.getStore().getNumPieces()];
	for (int i = 0; i < bitfield.length; i++) bitfield[i] = false;
	connection = con;
	this.downloader = downloader;
	choked = true;
	interested = false;
	requests = new ArrayList();
	priorities = new PriorityBitField(downloader.getNumPieces());
    }

    public void disconnected() {

	downloader.removeDownloader(this);
	letgo();
    }

    public void sendKeepalive() {

	connection.sendKeepalive();
    }

    private void letgo() {

	for (int i = 0; i < requests.size(); i++) {

	    int index = ((Integer)requests.get(i)).intValue();
	    int pieceIndex = 
		downloader.getPriorityToIndex(priorities.getFirst());
	    boolean before = 
		!downloader.getStore().getPiece(pieceIndex).isFinished();
	    downloader.getStore().getPiece(pieceIndex).lostDownload(index);

	    if (downloader.changeInterest(pieceIndex, index, before))
		downloader.adjustAll();
	}
    }

    Connection getConnection() { return connection; }

    public void gotChoke() {

	if (choked) return;
	choked = true;
	if (!endgame) letgo(); // TODO is this correct???, It seems to be what
	// the EndgameDownloader is doing
    }

    public void gotUnchoke() {

	if (!choked) return;
	choked = false;

	if (!endgame) {
	    
	    adjust();
	    endgame = downloader.getStore().allPending();

	} else {

	    int random[] = getRandomList(downloader.getStore().getNumPieces());

	    for (int i = 0; i < downloader.getStore().getNumPieces(); i++) {

		sendRandomRequests(random[i], false);
	    }
	}
    }

    private void sendRandomRequests(int index, boolean makeInterested) {

	PartialPiece piece = downloader.getStore().getPiece(index);

	if (!piece.isFinished() && bitfield[index]) {

	    double numberChunks = (double)piece.getActualPieceSize() / 
		(double)piece.getRequestPieceSize();
	    int numChunks = -1;

	    if (numberChunks == Math.floor(numberChunks))
		numChunks = (int)numberChunks;
	    else numChunks = (int)numberChunks + 1;
	    
	    int random2[] = getRandomList(numChunks);
	    
	    for (int j = 0; j < numChunks; j++) {
		
		if (!piece.contains(random2[j]) && 
		    piece.isActive(random2[j])) {
		    
		    int chunkIndex = random2[j] * 
			piece.getRequestPieceSize();
		    int len = piece.getRequestPieceSize();
		    if (chunkIndex + len > piece.getActualPieceSize())
			len = piece.getActualPieceSize() - chunkIndex;

		    if (!interested && makeInterested) {

			interested = true;
			connection.sendInterested();
			connection.sendRequest(index, chunkIndex, len);

		    } else if (interested) 
			connection.sendRequest(index, chunkIndex, len);
		}
	    }
	}
    } 

    private int[] getRandomList(int size) {

	int random[] = new int[size];
	ArrayList randomList = new ArrayList();
	
	for (int i = 0; i < size; i++)	    
	    randomList.add(new Integer(i));
	
	Random rand = new Random((new Date()).getTime());
	
	for (int i = 0; i < size; i++) {
	    
	    int index = rand.nextInt(randomList.size());
	    random[i] = ((Integer)randomList.remove(index)).intValue();
	}	    

	return random;
    }

    public boolean getChoked() {

	return choked;
    }

    public boolean getInterested() {

	return interested;
    }

    private static String formatAddress(InetAddress address) {

	String str = address.toString();
	if (str.startsWith("/")) str = str.substring(1);
	return str;
    }
    
    public boolean gotPiece(int index, int begin, byte[] piece) {

	int requestIndex = begin / downloader.getStore().getRequestPieceSize();
	InetAddress address = connection.getSocket().getInetAddress();
	downloader.getMonitor().updateStatus("received data from " + 
					     formatAddress(address) + 
					     " on port " + 
					     connection.getSocket().getPort());

	if (requests.remove(new Integer(begin))) {

	    PartialPiece ppiece = downloader.getStore().getPiece(index);
	    boolean before = !ppiece.isFinished();
	    
	    try {
	
		if (downloader.getStore() != null) 
		    downloader.getStore().storePiece(piece, index, begin);
		
	    } catch (InvalidHashException ihe) {

		System.err.println("invalid hash...");
		downloader.getMonitor().error("Piece number " + index + 
					      " had an invalid hash");
		downloader.getStore().getPiece(index).invalidatePiece();

	    } catch (IOException ioe) {
		
		downloader.getMonitor().error(ioe.getMessage());
		ioe.printStackTrace();
		downloader.getStore().getPiece(index).invalidatePiece();
	    }

	    if (((downloader.getStore().getPieceSize() == piece.length) ||
		 (downloader.getStore().getStoreLength() % 
		  downloader.getStore().getPieceSize() == piece.length)) &&
		(begin == 0)) {

		connection.sendHave(index);
		downloader.getMonitor().updateStatus("finished downloading piece number " + index);
		boolean bitfield[] = downloader.getStore().getBitfield();
		int completed = 0;
		for (int i = 0; i < bitfield.length; i++)
		    if (bitfield[i]) completed++;
		System.out.println("finished # " + completed);
		downloader.getMonitor().updateProgress(completed, 
						       bitfield.length);
		if (completed == bitfield.length) downloader.stopped();

	    } else if (before && ppiece.isFinished()) {

		connection.sendHave(index);
		downloader.getMonitor().updateStatus("finished downloading piece number " + index);
		boolean bitfield[] = downloader.getStore().getBitfield();
		int completed = 0;
		for (int i = 0; i < bitfield.length; i++)
		    if (bitfield[i]) completed++;
		System.out.println("finished # " + completed);
		downloader.getMonitor().updateProgress(completed, 
						       bitfield.length);
		if (completed == bitfield.length) downloader.stopped();
	    }
	    
	    int pieceIndex = 
		downloader.getPriorityToIndex(priorities.getFirst());
	    if (pieceIndex == -1) pieceIndex = index;

	    if (!endgame) {
		
		if (downloader.changeInterest(pieceIndex, requestIndex, 
					      before))
		    downloader.adjustAll();
		else adjust();

		endgame = downloader.getStore().allPending();

	    } else if (!ppiece.isFinished()) {

		// send requests for all chunks of this piece in random order
		sendRandomRequests(index, true);
		return false;
		
	    } else {

		// cancels other requests for this chunk
		downloader.sendCancelsFor(this, index, begin, piece.length);
		
		// if we are still downloading this piece
		if (ppiece.isActive()) return true;

		downloader.updateInterest(index);

		if (downloader.getStore().isComplete()) {

		    downloader.stopped();
		    downloader.closeConnections();
		}
	    }

	    // TODO do we need to check the endgame here???
	    //	endgame = downloader.getStore().allPending();
	    return ppiece.isFinished();
	}

	return false;
    }

    public boolean adjust() {

	if ((priorities.isEmpty()) && (requests.size() == 0)) {

	    if (interested) {

		interested = false;
		connection.sendNotInterested();
	    }
	} else if (!interested) {

	    interested = true;
	    connection.sendInterested();
	}

	if (choked) return false;

	boolean hit = false;
	boolean allPiecesRequested = false;
	int pieceSize = downloader.getStore().getPieceSize();

	int backlog = Integer.parseInt(Config.get("request_backlog"));
	while (!priorities.isEmpty() && !allPiecesRequested &&
	       (requests.size() < backlog)) {
	    
	    int i = downloader.getPriorityToIndex(priorities.getFirst());
	    int newRequestIndex = 
		downloader.getStore().getPiece(i).getNextBegin();

	    if (newRequestIndex != -1) {

		requests.add(new Integer(newRequestIndex));
		if (downloader.changeInterest(i, newRequestIndex))
		    hit = true;
		int length = downloader.getStore().getRequestPieceSize();
		int requestPieceSize = 
		    downloader.getStore().getRequestPieceSize();
		
		if (i * pieceSize + newRequestIndex + requestPieceSize > 
		    downloader.getStore().getStoreLength())
		    length = (int)downloader.getStore().getStoreLength() - 
			(i * pieceSize + newRequestIndex);
		connection.sendRequest(i, newRequestIndex, length); 

		// this case is when we are
		// almost done downloading a piece but aren't ready to start
		// downloading the next one on this connection yet
	    } else allPiecesRequested = true;
	}

	return hit;
    }

    public int getCurrentPieceIndex() {

	return downloader.getPriorityToIndex(priorities.getFirst());
    }

    public void gotHave(int index) {

	if (bitfield[index]) return;
	bitfield[index] = true;

	if (!endgame) {

	    if ((!downloader.getStore().isComplete()) &&
		(!downloader.getStore().getPiece(index).isFinished())) {
		
		int toInsert = downloader.getIndexToPriority(index);
		priorities.insert(toInsert);
		adjust();
	    }
	    
	    endgame = downloader.getStore().allPending();

	} else {

	    if (downloader.getStore().getBitfield()[index]) return;

	    // issue all current request for parts of this piece on
	    // this connection in a random order and if necessary make this
	    // connection interested
	    sendRandomRequests(index, true);

	    if (downloader.getStore().isComplete()) connection.close();
	}
    }

    public void gotBitfield(boolean[] bitfield) {

	if (!endgame) {

	    for (int i = 0; i < bitfield.length; i++) {
		
		this.bitfield[i] = bitfield[i];
		
		if (bitfield[i] && (!downloader.getStore().isComplete()) &&
		    (!downloader.getStore().getPiece(i).isFinished())) {
		    
		    int toInsert = downloader.getIndexToPriority(i);
		    priorities.insert(toInsert);
		    adjust();
		}
	    }
	    
	    endgame = downloader.getStore().allPending();

	} else {

	    for (int i = 0; i < bitfield.length; i++) {
		
		this.bitfield[i] = bitfield[i];
	    }

	    // issue all current request for parts of pieces contained
	    // on the other size of this connection in a random order and if 
	    // necessary make this connection interested
	    int random[] = getRandomList(downloader.getStore().getNumPieces());

	    for (int i = 0; i < downloader.getStore().getNumPieces(); i++) {

		sendRandomRequests(random[i], true);
	    }
	    
	    if (downloader.getStore().isComplete()) connection.close();
	}
    }

    public PriorityBitField getPriorities() { return priorities; }

    public boolean[] getBitfield() { return bitfield; }
}
