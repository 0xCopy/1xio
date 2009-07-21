
package org.jtorrent.tracker;

import java.io.*;
import java.util.List;
import java.util.Date;
import java.net.URL;
import java.net.Socket;

import org.jtorrent.ptop.IMonitor;
import org.jtorrent.ptop.ConnectionManager;
import org.jtorrent.ptop.BtTimerTask;
import org.jtorrent.msgdata.Utils;
import org.jtorrent.msgdata.URLEncoder;
import org.jtorrent.msgdata.BencodedMessage;

import org.jtorrent.Config;

/**
 * this class which interacts with the tracker by sending HTTP requests
 * and handling the bencoded messages returned by the tracker
 *
 * @author Hunter Payne
 */
public class TrackerClient implements ITrackerClient, Runnable {

    private ConnectionManager manager;
    private URL trackerUrl;
    private List peers;
    //    private long checkInterval;
    private long lastCheck;
    private int timeoutInterval;
    private int minPeers;

    /**
     * Creates a new tracker client
     *
     * @param manager -- object which manages network communication for this
     * peer
     * @param trackerUrl -- url of the tracker
     * @param monitor -- object which monitors the status of this download
     */
    public TrackerClient(ConnectionManager manager, String trackerUrl,
			 IMonitor monitor )
	throws IOException {
	
	this.manager = manager;
	this.trackerUrl = new URL(trackerUrl);
	//	this.checkInterval = checkInterval;
	// this.checkInterval = Integer.parseInt(Config.get("rerequest_interval"));
	this.minPeers = Integer.parseInt(Config.get("min_peers"));
	this.timeoutInterval = Integer.parseInt(Config.get("http_timeout"));
	lastCheck = 0;
	peers = null;
	started();
	Runtime runtime = Runtime.getRuntime();
	runtime.addShutdownHook(new Thread(new SendCompleted()));
    }

    /** sends a stopped message to the tracker */
    public class SendCompleted implements Runnable {

	public void run() {

	    try {

		stopped();

	    } catch (Exception e) {}
	}
    }

    /** gets the list of peers as bencoded messages */
    public List getPeerList() {

	if ((peers == null) && 
	    (lastCheck + timeoutInterval < (new Date()).getTime())) {

	    run();
	}

	return peers;
    }

    /** gets the refresh interval to recheck the tracker */
    public long getRefreshInterval() {

	return timeoutInterval;
    }

    /** sets the refresh interval to recheck the tracker */
    public void setRefreshInterval(long interval) {

	timeoutInterval = (int)interval;
    }

    /** sends the started message */
    public void started() throws IOException {

	sendRequest("&event=started");
    }

    /** sends the stopped message */
    public void stopped() throws IOException {

	sendRequest("&event=stopped");
    }

    /** sends the completed message */
    public void completed() throws IOException {

	sendRequest("&event=completed");
    }

    /** sends a generic request */
    public void run() {

	try {

	    sendRequest(null);

	} catch (IOException ioe) {

	    ioe.printStackTrace();
	}
    }

    // sends a tracker HTTP request
    private synchronized void sendRequest(String eventStr) throws IOException {

	lastCheck = (new Date()).getTime();

	try {

	    // open the socket and write the request to its output stream
	    int port = trackerUrl.getPort();
	    if (port == -1) port = 80;
	    Socket sock = new Socket(trackerUrl.getHost(), port);
	    OutputStream os = sock.getOutputStream();

	    os.write(Utils.getBytes("GET " + trackerUrl.getPath() +
				    "?info_hash="));
	    os.write(URLEncoder.encode(manager.getInfoHash(), Utils.encoding));
	    os.write(Utils.getBytes("&peer_id="));
	    os.write(URLEncoder.encode(manager.getPeerId(), Utils.encoding));
	    os.write(Utils.getBytes("&port="));
	    os.write(Utils.getBytes("" + manager.getPort()));
	    os.write(Utils.getBytes("&uploaded="));
	    os.write(Utils.getBytes("" + manager.getUploaded()));
	    os.write(Utils.getBytes("&downloaded="));
	    os.write(Utils.getBytes("" + manager.getDownloaded()));
	    os.write(Utils.getBytes("&left="));
	    os.write(Utils.getBytes("" + manager.getLeft()));

	    if (eventStr != null)
		os.write(Utils.getBytes(eventStr));

	    os.write(Utils.getBytes("\n\n"));

	    InputStream is = sock.getInputStream();
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte buf[] = new byte[1024];
	    int read = 0;

	    // read the message from the socket's input stream
	    while ((read = is.read(buf)) != -1) {

		baos.write(buf, 0, read);
	    }

	    byte messageBytes[] = baos.toByteArray();
	    byte delimiter[] = Utils.getBytes("\r\n");
	    boolean found = false;

	    // strip off headers!!!
	    for (int i = 0; !found && i < messageBytes.length - 2; i++) {

		if ((messageBytes[i] == delimiter[0]) &&
		    (messageBytes[i + 1] == delimiter[1])) {

		    found = true;
		    byte temp[] = new byte[messageBytes.length - i - 
					   delimiter.length];
		    System.arraycopy(messageBytes, i + 2, temp, 0, 
				     temp.length);
		    messageBytes = temp;
		}
	    }

	    BencodedMessage message = 
		BencodedMessage.readFromMessage(messageBytes);

	    if (!message.containsKey("failure reason")) {

		timeoutInterval = message.getIntValue("interval");
		List peers = message.getListValue("peers");

		if (this.peers != null) {
		    
		    // updates our own internal list of peers
		    for (int i = 0; i < peers.size(); i++) {

			BencodedMessage peerMessage =
			    (BencodedMessage)peers.get(i);
			byte peerId[] = peerMessage.getStringBytes("peer id");
			boolean contains = false;
			
			for (int j = 0; !contains && j < this.peers.size(); 
			     j++) {
			    
			    BencodedMessage checkMessage =
				(BencodedMessage)this.peers.get(j);
			    byte checkId[] = 
				checkMessage.getStringBytes("peer id");
			    
			    if ((peerId.length == checkId.length) &&
				(peerId.length == 20)) {

				boolean matches = true;

				for (int k = 0; matches && k < checkId.length;
				     k++) {

				    if (peerId[k] != checkId[k]) 
					matches = false;
				}

				if (matches) contains = true;
			    }			    
			}
			
			if (!contains) this.peers.add(peerMessage);
		    }
		} else this.peers = peers;
	    } else {

		System.err.println("failure message " + 
				   message.getStringValue("failure reason"));
	    }
	} catch (Exception e) {

	    e.printStackTrace();
	}
    }
}
