
package org.jtorrent.tracker;

import java.io.*;
import java.util.*;
import java.net.InetAddress;

import javax.servlet.*;
import javax.servlet.http.*;

import org.jtorrent.Config;

import org.jtorrent.msgdata.BencodedMessage;
import org.jtorrent.ptop.SHAUtils;
import org.jtorrent.ptop.BtTimerTask;

/**
 * This is the servlet which handles requests from the tracker client, manages
 * the list of peers and sends those peer lists to the peers randomly.
 *
 * @author Hunter Payne
 */
public class TrackerServlet extends HttpServlet implements Runnable {

    private IPeerInfoManager manager;
    private int responseSize;
    private long reannounceInterval;
    private long peerTimeout;
    private long timeoutCheckInterval;
    private File logFile;
    private long logFileInterval; 
    private boolean natCheck;

    private File allowedDir;
    private long parseAllowedInterval;
    private File originDir;
    private String command;

    private Hashtable messages;
    private Hashtable files;

    /**
     * initializes the servlet
     */
    public void init(ServletConfig config) throws ServletException {

	super.init(config);

	messages = new Hashtable();
	files = new Hashtable();
	String resourceName = 
	    config.getInitParameter("database_resource_name");
	manager = new SQLPeerInfoManager(this, resourceName);

	try {

	    responseSize = 
		Integer.parseInt(config.getInitParameter("response_size"));
	    reannounceInterval = 
		Long.parseLong(config.getInitParameter("reannounce_interval"));
	    peerTimeout = 
		Long.parseLong(config.getInitParameter("timeout_downloaders_interval")) * 1000;
	    timeoutCheckInterval = 
		Long.parseLong(config.getInitParameter("timeout_check_interval"));
	    logFile = new File(config.getInitParameter("logfile"));
	    natCheck = false;
	    String natCheckParam = config.getInitParameter("nat_check");
	    
	    if ((natCheckParam != null) && (natCheckParam.equals("true")))
		natCheck = true;

	    command = config.getInitParameter("command");
	    logFileInterval = Long.parseLong(config.getInitParameter("min_time_between_log_flushes"));
	    allowedDir = new File(config.getInitParameter("allowed_dir"));
	    parseAllowedInterval = Long.parseLong(config.getInitParameter("parse_allowed_interval"));
	    originDir = new File(config.getInitParameter("origin_dir"));

	    run();

	    // start the scheduled tasks
	    new BtTimerTask(this, parseAllowedInterval);
	    new BtTimerTask(new ConnectionTimeouter(), timeoutCheckInterval);
	    new BtTimerTask(new LogFileSaver(), logFileInterval);

	} catch (Exception e) {

	    e.printStackTrace();
	}	
    }

    /**
     * called when the servlet is unloaded
     */
    public void destroy() {

	files.clear();
	messages.clear();
	manager.close();
	super.destroy();
    }

    /**
     * checks the allowed directory and add them to the list of .torrent
     * files to manage
     */
    public void run() {

	File files[] = allowedDir.listFiles();

	for (int i = 0; i < files.length; i++) {

	    if ((files[i].getName().endsWith(".torrent")) &&
		(!messages.containsKey(files[i]))) {

		try {

		    FileInputStream fis = new FileInputStream(files[i]);
		    BencodedMessage message = 
			BencodedMessage.readFromStream(fis);
		    
		    BencodedMessage infoMessage = 
			(BencodedMessage)message.getDictValue("info");
		    byte infoBytes[] = infoMessage.getMessageBytes();
		    byte digest[] = SHAUtils.computeDigest(infoBytes);
		    
		    messages.put(files[i], digest);
		    String name = infoMessage.getStringValue("name");
		    this.files.put(files[i], new File(originDir, name));

		    if (command != null) {

			// TODO start Origin
		    }
		} catch (Exception e) {

		    e.printStackTrace();
		}
	    }
	}
    }

    // checks a info hash against our list of info hashs
    private boolean checkInfoHash(byte[] checkHash) {

	if (checkHash.length == 20) {

	    Enumeration e = messages.keys();
	    
	    while (e.hasMoreElements()) {		
		File key = (File)e.nextElement();
		byte digest[] = (byte[])messages.get(key);
		boolean matches = true;

		for (int i = 0; matches && i < 20; i++) 
		    if (digest[i] != checkHash[i]) matches = false;

		if (matches) return true;
	    }
	}

	return false;
    }

    // gets a random list of peers
    private ArrayList generateRandomList(byte infoHash[]) {

	Random rand = new Random((new Date()).getTime());
	ArrayList peers = manager.getAllPeers(infoHash);
	double size = (double)peers.size();
	ArrayList ret = new ArrayList();

	if (responseSize < size) {

	    for (int i = 0; i < responseSize; i++) {
		
		double random = rand.nextDouble();
		int index = (int)(size * random);
		IPeerInfo peer = (IPeerInfo)peers.get(index);
		ret.add(makePeerMessage(peer));
	    }
	} else {

	    for (int i = 0; i < size; i++) {
		
		IPeerInfo peer = (IPeerInfo)peers.get(i);
		ret.add(makePeerMessage(peer));
	    }	    
	}

	return ret;
    }

    // makes a bencoded message from this peers 
    private BencodedMessage makePeerMessage(IPeerInfo peer) {

	BencodedMessage peerMessage = new BencodedMessage();
	peerMessage.setStringValue("peer id", peer.getPeerId());

	try {

	    InetAddress address = InetAddress.getByAddress(peer.getPeerIp());
	    peerMessage.setStringValue("ip", address.getHostAddress());

	} catch (java.net.UnknownHostException uhe) {

	    uhe.printStackTrace();
	}

	peerMessage.setIntValue("port", peer.getPort());
	return peerMessage;
    }
    
    // makes the actual bencoded message to send back to the peer
    private BencodedMessage generateMessage(IPeerInfo peer) {

	BencodedMessage message = new BencodedMessage();
	message.setIntValue("interval", (int)reannounceInterval);
	ArrayList peers = peer.getPeerCache();

	if ((peers == null) || (peers.size() < 10)) {

	    peers = generateRandomList(peer.getInfoHash());
	    peer.setPeerCache(peers);
	}

	message.setListValue("peers", peers);
	return message;
    }

    /** called when we receive a GET request */
    public void doGet(HttpServletRequest req, HttpServletResponse resp) 
	throws ServletException, IOException {

	/*	doPost(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) 
	throws ServletException, IOException {
	*/
	byte infoHash[] = null;
	byte peerId[] = null;
	InetAddress ip = null;
	int port = -1;
	long uploaded = 0;
	long downloaded = 0;
	long left = 0;
	String event = null;
	
	Enumeration e = req.getParameterNames();

	// tomcat has some problems
	while (e.hasMoreElements()) {

	    String name = (String)e.nextElement();
	    String value = req.getParameter(name);

	    if (name.equals("info_hash")) {

		infoHash = value.getBytes();

	    } else if (name.equals("peer_id")) {

		peerId = value.getBytes();

	    } else if (name.equals("port")) {
		
		port = Integer.parseInt(value);

	    } else if (name.equals("uploaded")) {

		uploaded = Long.parseLong(value);

	    } else if (name.equals("downloaded")) {

		downloaded = Long.parseLong(value);

	    } else if (name.equals("left")) {

		left = Long.parseLong(value);

	    } else if (name.equals("ip")) {

		ip = InetAddress.getByAddress(value.getBytes());

	    } else if (name.equals("event")) {

		event = value;
	    }
	}

	if (!checkInfoHash(infoHash)) {

	    sendFailMessage("Not authorized", resp);
	    return;
	}

	if (ip == null) {

	    String host = req.getRemoteHost();
	    ip = InetAddress.getByName(host);
	}

	IPeerInfo peer = null;

	// get the peer and check the NAT if necessary
	if (!natCheck)
	    peer = manager.getPeerInfo(infoHash, peerId);
	else {

	    NATCheck checker = new NATCheck(ip, port, infoHash);
	    if (checker.isOkay()) peer = manager.getPeerInfo(infoHash, peerId);
	}

	// if this is a started message make the new peer
	if ((peer == null) && (event.equals("started"))) {

	    peer = manager.makeNewPeerInfo(peerId, ip.getAddress(), port, 
					   infoHash);

	} else if (peer != null) {

	    // otherwise just update the peer
	    peer.setUploaded(uploaded);
	    peer.setDownloaded(downloaded);
	    manager.updateAccess(peer);

	} else if (!natCheck) {

	    sendFailMessage("no peer and not started event", resp);
	    return;

	} else {

	    sendFailMessage("'You are behind NAT. Please open port 6881 or " +
			    "download from elsewhere", resp);
	    return;
	}
	
	sendMessage(generateMessage(peer), resp);
    }

    // sends a bencoded message to the peer
    private void sendMessage(BencodedMessage message, 
			     HttpServletResponse resp) {

	try {

	    ServletOutputStream sos = resp.getOutputStream();
	    sos.write(message.getMessageBytes());
	    sos.flush();
	    sos.close();

	} catch (Exception e) {

	    e.printStackTrace();
	}
    }

    // sends a failure message
    private void sendFailMessage(String message, HttpServletResponse resp) {

	BencodedMessage failMessage = new BencodedMessage();
	failMessage.setStringValue("failure reason", message);
	sendMessage(failMessage, resp);
    }

    // called to check the peers for stale/timedout entries and removes them
    class ConnectionTimeouter implements Runnable {

	public void run() {

	    manager.timeoutPeers(peerTimeout);
	}
    }

    // makes a log file
    private void makeLogFile() {

	StringBuffer buf = new StringBuffer();
	buf.append("<HTML><HEAD><TITLE>Bit Torrent Tracker Log</TITLE></HEAD><BODY bgcolor=\"white\"><br /><CENTER><H1>Bit Torrent Tracker Log</H1></CENTER>");
	buf.append("<H2>Serving " + messages.size() + " files</H2><br />");
	Enumeration files = messages.keys();

	while (files.hasMoreElements()) {

	    File file = (File)files.nextElement();
	    byte infoHash[] = (byte[])messages.get(file);
	    File originFile = (File)this.files.get(file);
	    ArrayList peers = manager.getAllPeers(infoHash);
	    buf.append("<H1>Peers report for ");
	    buf.append(originFile.getAbsolutePath());
	    buf.append("</H1><br />");

	    buf.append("<TABLE><TR><TH>Peer Host</TH><TH>Download Rate</TH><TH>Upload Rate</TH></TR>");

	    if (peers != null) {

		for (int i = 0; i < peers.size(); i++) {
		    
		    IPeerInfo peer = (IPeerInfo)peers.get(i);
		    buf.append("<TR>");
		    buf.append("<TD>");
		    
		    try { 

			buf.append(InetAddress.getByAddress(peer.getPeerIp()).getHostAddress());
		    } catch (java.net.UnknownHostException uhe) {}
		    
		    buf.append("</TD>");
		    buf.append("<TD>");
		    buf.append(peer.getDownloaded());
		    buf.append("</TD>");
		    buf.append("<TD>");
		    buf.append(peer.getUploaded());
		    buf.append("</TD>");
		    buf.append("</TR>");
		}
	    } else {

		buf.append("<TR><TD>No peers found</TD></TR>");
	    }

	    buf.append("</TABLE>");
	}

	buf.append("</BODY></HTML>");

	try {

	    FileWriter fw = new FileWriter(logFile);
	    fw.write(buf.toString());
	    fw.flush();
	    fw.close();

	} catch (Exception e) {

	    e.printStackTrace();
	}
    }

    // called to regenerate the log file at a specified interval
    class LogFileSaver implements Runnable {

        public void run() {

	    makeLogFile();
	}
    }
}
