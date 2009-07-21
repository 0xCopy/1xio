
package org.jtorrent.ptop;

import java.util.*;
import java.io.IOException;
import java.net.InetAddress;

import org.jtorrent.Config;

import org.jtorrent.tracker.TrackerClient;
import org.jtorrent.msgdata.BencodedMessage;
import org.jtorrent.msgdata.Utils;

/**
 * Java port of the Downloader object in the Downloader.py module in Bit 
 * Torrent 3.0.2
 */
public class Downloader implements Runnable {

    private boolean running;
    private Thread thread;
    private ArrayList downloaders;
    private HashCheckStore store;
    private HashMap indexToPriority;
    private HashMap priorityToIndex;
    private IMonitor monitor;
    private int numPieces;

    private ConnectionManager manager;
    private TrackerClient client;

    public Downloader(HashCheckStore store, BencodedMessage message, 
		      IMonitor monitor) 
	throws Exception {

	numPieces = store.getHashs().length;
	this.monitor = monitor;
	this.store = store;
	running = false;
	downloaders = new ArrayList();
	thread = new Thread(this);
	indexToPriority = new HashMap();
	priorityToIndex = new HashMap();
	int numPieces = store.getBitfield().length;
	Random random = new Random((new Date()).getTime());
	ArrayList randoms = new ArrayList();

	for (int i = 0; i < numPieces; i++) {

	    randoms.add(new Double(random.nextDouble()));
	    indexToPriority.put(new Integer(i), new Integer(i));
	}

	Iterator it = indexToPriority.keySet().iterator();

	while (it.hasNext()) {

	    Integer keyi = (Integer)it.next();
	    Double priorityi = (Double)randoms.get(keyi.intValue());
	    Iterator it2 = indexToPriority.keySet().iterator();

	    while (it2.hasNext()) {

		Integer keyj = (Integer)it2.next();

		if (keyi != keyj) {

		    Double priorityj = (Double)randoms.get(keyj.intValue());
		    
		    if (priorityi.doubleValue() < priorityj.doubleValue()) {

			randoms.set(keyi.intValue(), priorityj);
			randoms.set(keyj.intValue(), priorityi);
			Integer temp = (Integer)indexToPriority.get(keyi);
			indexToPriority.put(keyi, indexToPriority.get(keyj));
			indexToPriority.put(keyj, temp);
		    }
		}
	    }
	}

	it = indexToPriority.keySet().iterator();

	while (it.hasNext()) {

	    Integer index = (Integer)it.next();
	    Integer priority = (Integer)indexToPriority.get(index);
	    priorityToIndex.put(priority, index);
	}

	BencodedMessage infoMessage = 
	    (BencodedMessage)message.getDictValue("info");
	manager = new ConnectionManager(infoMessage, this, store, 
					constructId());
	client = 
	    new TrackerClient(manager, message.getStringValue("announce"),
			      monitor);
	int keepaliveInterval = 
	    Integer.parseInt(Config.get("keepalive_interval"));

	new BtTimerTask(new Pinger(), keepaliveInterval);
    }

    public double getUploadRate() {
	return manager.getUploadRate();
    }

    public long getDownloaded() {

	return manager.getDownloaded();
    }

    class Pinger implements Runnable {

	public void run() {

	    sendKeepalives();
	}
    }

    protected void stopped() { 

	try {

	    client.stopped(); 

	} catch (Exception e) {

	    e.printStackTrace();
	}
    }

    private void sendKeepalives() {

	for (int i = 0; i < downloaders.size(); i++) {

	    SingleDownloader sd = (SingleDownloader)downloaders.get(i);
	    sd.sendKeepalive();
	}
    }
    
    public static byte[] constructId() {

	String hostname = "localhost";

	try {

	    InetAddress address = InetAddress.getLocalHost();
	    hostname = address.getHostName();

	} catch (Exception e) {

	    e.printStackTrace();
	}

	String msg = (new Date()).toString() + hostname;
	return SHAUtils.computeStringDigest(msg);
    }

    public void run() {

	while (running) {

	    try {

		// get a list of peers from the tracker client
		List peers = client.getPeerList();

		for (int i = 0; i < peers.size(); i++) {

		    BencodedMessage peer = (BencodedMessage)peers.get(i);
		    byte peerId[] = peer.getStringBytes("peer id");
		    byte ip[] = peer.getStringBytes("ip");
		    int port = peer.getIntValue("port");

		    if (!manager.isLocalhost(peerId)) {

			if ((peerId != null) && (ip != null)) {

			    // make connections for all of them
			    if (!manager.hasConnection(peerId)) {

				InetAddress address = 
				    InetAddress.getByName(new String(ip));
				System.out.println("got peer address " + address);
				manager.getConnection(peerId, ip, port);
				monitor.updateStatus("made connection to " + 
						     new String(ip) + " on port " + port);
			    }
			}
		    }
		}

		if (store.isComplete()) {

		    running = false;
		    client.completed();
		}
	    } catch (Exception e) {

		e.printStackTrace();
	    }

	    try {

		Thread.sleep(1000 * client.getRefreshInterval());

	    } catch (Throwable t) {

		
	    }
	}

	// choke all the downloading connections
	for (int i = 0; i < downloaders.size(); i++) {
	    
	    SingleDownloader sd = (SingleDownloader)downloaders.get(i);
	    sd.disconnected();
	}
    }

    public void start() {

	running = true;
	thread.start();

	try {

	    client.started();

	} catch (IOException ioe) { ioe.printStackTrace(); }
    }

    public void stop() {

	running = false;

	try {

	    client.stopped();

	} catch (IOException ioe) { ioe.printStackTrace(); }
    }

    public IMonitor getMonitor() { return monitor; }

    public HashCheckStore getStore() { return store; }

    public boolean changeInterest(int index, int requestIndex) {

	return changeInterest(index, requestIndex, true);
    }

    public boolean changeInterest(int index, int requestIndex, 
				  boolean before) {

	PartialPiece piece = store.getPiece(index);
	boolean after = !piece.isFinished();
	Integer indexObj = new Integer(index);
	int priority = ((Integer)indexToPriority.get(indexObj)).intValue();
	boolean r = false;

	    for (int i = 0; i < downloaders.size(); i++) {

		SingleDownloader sd = (SingleDownloader)downloaders.get(i);
		
		if (sd.getBitfield()[index]) {

		    r = true;
		if( ! before && after )
		    sd.getPriorities().insert(priority);
	 else if (before && !after)
		    sd.getPriorities().remove(priority);
	    }
	}

	return r;
    }

    public void adjustAll() {

	for (int i = 0; i < downloaders.size(); i++) {

	    SingleDownloader sd = (SingleDownloader)downloaders.get(i);
	    sd.adjust();
	}
    }

    public SingleDownloader makeDownloader(Connection con) {

	SingleDownloader sd = new SingleDownloader(this, con);
	downloaders.add(sd);
	return sd;
    }

    public void removeDownloader(SingleDownloader sd) {

	downloaders.remove(sd);
    }

    public int getIndexToPriority(int index) {

	return ((Integer)indexToPriority.get(new Integer(index))).intValue();
    }

    public int getPriorityToIndex(int priority) {

	Integer intObj = new Integer(priority);

	if (priorityToIndex.containsKey(intObj))
	    return ((Integer)priorityToIndex.get(intObj)).intValue();
	System.out.println("unable to find index for priority " + priority);
	System.out.println("" + priorityToIndex);
	//	(new Exception()).printStackTrace();
	return -1;
    }

    public int getNumPieces() {

	return numPieces;
    }

    // loops over downloaders and cancels similar requests on the other 
    // downloaders
    protected void sendCancelsFor(SingleDownloader downloader, int index, 
				  int begin, int len) {
	
	for (int i = 0; i < downloaders.size(); i++) {

	    SingleDownloader sd = (SingleDownloader)downloaders.get(i);
	    
	    if ((downloader != sd) && !sd.getChoked() && 
		sd.getBitfield()[index]) 
		sd.getConnection().sendCancel(index, begin, len);
	}
    }

    protected void updateInterest(int index) {

	// loop over the downloaders 
	for (int i = 0; i < downloaders.size(); i++) {

	    SingleDownloader sd = (SingleDownloader)downloaders.get(i);
	    boolean bitfield[] = sd.getBitfield();

	    // and if the other end of the connection has the piece
	    if (bitfield[index]) {

		// check for pieces other other we can download
		for (int j = 0; j < bitfield.length; j++) {

		    if (bitfield[j] && store.getPiece(j).isActive()) return;
		}

		// if none exist, send not_interested and return true
		sd.getConnection().sendNotInterested();
	    }
	}
    }

    protected void closeConnections() {

	// loop over the downloaders 
	for (int i = 0; i < downloaders.size(); i++) {

	    SingleDownloader sd = (SingleDownloader)downloaders.get(i);
	    sd.getConnection().close();
	}	
    }

	public double getRate() { return manager.getRate(); }
}
