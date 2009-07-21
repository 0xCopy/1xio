
package org.jtorrent.ptop;

import java.util.*;
import java.nio.channels.*;
import java.net.*;
import java.io.IOException;

import org.jtorrent.msgdata.*;
//
import org.jtorrent.Config;
import org.jtorrent.Stats;

/**
This object manages the bit torrent network connections this peer has.
Since the networking code is using non-blocking semantics,
this object's run method loops forever to call the read and write methods
of any connection objects that are ready.

@author Hunter Payne, Dave Blau
@version 0.2
*/
public class ConnectionManager implements Runnable {

	private Downloader downloader;
	private Hashtable connections;
	private Selector selector;
	private byte[] peerId;
	private BencodedMessage infoMessage;
	private HashCheckStore store;
	private InetAddress localhost;
	private int port;
	private long uploaded;
	private long downloaded;
	private long totalLength;
	private int maxConnections;
	private Choker choker;
	private ServerSocketChannel ssc;
	private Vector connectionsToRegister;
	//
	private int statNo;
	private Stats stats;

	/**
	* constructs an object that manages all network communications on behalf
	* of a bit torrent peer
	*
	* @param infoMessage -- the bencoded message stored in the info key in the
	* .torrent file
	* @param downloader -- the object manageing the download operations
	* @param store -- the object managing access to the local downloaded files
	* @param peerId -- id of this peer
	*/
	public ConnectionManager(BencodedMessage infoMessage, 
			     Downloader downloader, HashCheckStore store, 
			     byte[] peerId) 
	throws IOException {

	connectionsToRegister = new Vector();
	maxConnections = Integer.parseInt(Config.get("max_initiate"));

	this.downloader = downloader;
	this.store = store;
	this.peerId = peerId;
	this.infoMessage = infoMessage;
	selector = Selector.open();
	uploaded = 0;
	downloaded = 0;// TODO set to the pad computed by the Plugin
	this.totalLength = store.getStoreLength();
	connections = new Hashtable();

	choker = new Choker();

	// Create a new server socket and set to non blocking mode
	ssc = ServerSocketChannel.open();
	ssc.configureBlocking(false);
	ServerSocket ss = ssc.socket();

	localhost = null;

	try {

	    String bind = Config.get("bind");

	    if (bind != null) {

		System.out.println("setting localhost to " + bind);
		localhost = InetAddress.getByName(bind);
	    }

	    if (localhost == null) 
		localhost = InetAddress.getLocalHost();

	    if (localhost == null) {

		System.err.println("couldn't find a localhost, set the "+ 
				   "bind parameter");
		System.exit(1);
	    }
	} catch(Exception e) {

	    e.printStackTrace();
	}

	boolean bound = false;
	int minport = Integer.parseInt(Config.get("minport"));
	int maxport = Integer.parseInt(Config.get("maxport"));

	// bind to a local port between 6881 and 6889
	for (int i = minport; i <= maxport && !bound; i++) {

	    InetSocketAddress address = new InetSocketAddress(localhost, i);

	    try {

		ss.bind(address);
		System.out.println("bound to " + address);
		port = i;
		bound = true;

	    } catch (IOException ioe) {

		if (i > minport) ioe.printStackTrace();
	    }
	}

	if (!bound) {

	    System.err.println("unable to bind to port...");
	    System.exit(1);
	}

	// create the accept key
	try {

	    SelectionKey acceptKey = 
		ssc.register(selector, SelectionKey.OP_ACCEPT);

	} catch (Exception e) { e.printStackTrace(); }

	// start the management thread
	Thread thread = new Thread(this);
	thread.setDaemon(true);
	thread.start();
	int timeoutCheckInterval = 
	    Integer.parseInt(Config.get("timeout_check_interval"));

	// start the timeout checker
	BtTimerTask task = new BtTimerTask(new ConnectionTimeouter(), 
					   timeoutCheckInterval);

	statNo = Stats.newSub( Stats.CONN );
	stats  = Stats.getSub( Stats.CONN, statNo );
	}

	/**
	* returns true if this object manages a connection which the specified
	* peer id
	*
	* @param peerId -- id of the desired peer
	*/
	public boolean hasConnection(byte[] peerId) {

	return connections.containsKey(new String(peerId));
	}

	/**
	* gets a connection object with the specified peer id, if necessary
	* it opens a new network connection to the specified ip and port
	* 
	* @param peerId -- id of the desired peer
	* @param peerIp -- ip of the desired peer
	* @param port -- port where the peer is running
	* connection
	*/
	public Connection getConnection(byte[] peerId, byte[] peerIp, int port )
				    throws IOException {
	
	Connection con = (Connection)connections.get(new String(peerId));

	// if we need to create a new connection
	if ((con == null) && (connections.size() < maxConnections)) {

	    SocketChannel channel = SocketChannel.open();
	    System.out.println("opened channel...");
	    channel.configureBlocking(false);
	    InetAddress address = InetAddress.getByName(new String(peerIp));
	    channel.connect(new InetSocketAddress(address, port));
	    downloader.getMonitor().updateStatus("made connection to " + 
						 address + ":" + port);

	    con = new Connection(channel.socket(), this, downloader, choker, 
				 store);
	    connections.put(new String(peerId), con);
	    connectionsToRegister.add(con);
	    selector.wakeup();
	}

	return con;
	}

	/**
	* registers a connection with the specified peer id
	*/
	void registerConnection(byte[] peerId, Connection conn) {

	connections.put(new String(peerId), conn);
	}
	
	/**
	* unregisters a connection with the specified peer id
	*/
	void unregisterConnection(byte[] peerId) {

	connections.remove(new String(peerId));
	}

	/**
	* runs the thread responsible for network management
	*/
	public void run() {

	while(true) {

	    try {

		// register any connections if necessary
		while (connectionsToRegister.size() > 0) {

		    Connection con = 
			(Connection)connectionsToRegister.remove(0);
		    Socket sock = con.getSocket();
		    SocketChannel channel = sock.getChannel();
		    channel.register(selector, SelectionKey.OP_CONNECT, con);
		}

		if (selector.select() > 0) {

		    Set keys = selector.selectedKeys();
		    Iterator it = keys.iterator();

		    // loop over the connections which are ready
		    while (it.hasNext()) {

			SelectionKey key = (SelectionKey)it.next();
			it.remove();

			// if an accept request is ready
			if ((key.readyOps() & SelectionKey.OP_ACCEPT) > 0) {
			    
			    if (connections.size() < maxConnections) {

				ServerSocketChannel channel = 
				    (ServerSocketChannel)key.channel();
				SocketChannel sc = channel.accept();

				if (sc != null) {

				    System.out.println("accepted channel...");
				    sc.configureBlocking(false);
				    Socket s = sc.socket();
				    Connection con = 
					new Connection(s, this, downloader, 
						       choker, store );
				    SelectionKey connectKey = 
					sc.register(selector, 
						    SelectionKey.OP_READ | 
						    SelectionKey.OP_WRITE,
						    con);
				    con.sendBitfield(store.getBitfield());
				}
			    }
			}

			// if a connection operation is ready
			if (key.isValid() && 
			    (key.readyOps() & SelectionKey.OP_CONNECT) > 0) {

			    SocketChannel channel = 
				(SocketChannel)key.channel();

			    if (channel.finishConnect())
				key = key.interestOps(SelectionKey.OP_READ | 
						      SelectionKey.OP_WRITE);
			    else key.cancel();			    

			    System.out.println("opened channel...");
			}

			// if this connection is ready to read
			if ((key.readyOps() & SelectionKey.OP_READ) > 0) {
			    
			    SocketChannel channel = 
				(SocketChannel)key.channel();

			    if (key.attachment() == null) {

				channel.configureBlocking(false);
				SelectionKey connectKey = 
				    channel.register(selector, 
						     SelectionKey.OP_READ | 
						     SelectionKey.OP_WRITE);
				Connection con = 
				    new Connection(channel.socket(), this, 
						   downloader, choker, store );
				connectKey.attach(con);
			    }

			    Connection con = (Connection)key.attachment();
			    con.handleRead();
			} 

			// if we are ready to write
			if (key.isValid() && 
			    (key.readyOps() & SelectionKey.OP_WRITE) > 0) {

			    SocketChannel channel = 
				(SocketChannel)key.channel();

			    if (key.attachment() == null) {

				channel.configureBlocking(false);
				SelectionKey connectKey = 
				    channel.register(selector, 
						     SelectionKey.OP_READ | 
						     SelectionKey.OP_WRITE);
				Connection con = 
				    new Connection(channel.socket(), this, 
						   downloader, choker, store );
				connectKey.attach(con);
			    }

			    Connection con = (Connection)key.attachment();

			    try {

				con.writeNext(1024);

			    } catch (IOException ioe2) {

				con.close();
			    }
			}
		    }
		} else {
		    
		    Thread.sleep(10);
		}
	    } catch (IOException ioe) {

		System.err.println(ioe.getMessage());

	    } catch (Throwable t) {

		t.printStackTrace();
	    }
	}
	}

	class ConnectionTimeouter implements Runnable {

	public void run() {

	    Enumeration e = connections.keys();

	    while (e.hasMoreElements()) {

		String peerId = (String)e.nextElement();
		Connection con = (Connection)connections.get(peerId);
		if (!con.isAlive()) con.close();
	    }
	}
	}

	/**
	* send keep alives on all threads
	*/
	public void sendKeepalives() {

	Iterator it = connections.values().iterator();

	while (it.hasNext()) {

	    Connection con = (Connection)it.next();
	    con.sendKeepalive();
	}
	}

	/**
	* computs the hash of the bencoded message in the info slot of the 
	* .torrent file
	*/
	public byte[] getInfoHash() {
	
	return SHAUtils.computeDigest(infoMessage.getMessageBytes());
	}

	/**
	* accessor for the local peer id
	*/
	public byte[] getPeerId() {

	return peerId;
	}

	/**
	* returns our local ip address
	*/
	public InetAddress getIp() {

	return localhost;
	}

	/**
	* returns the local port to which we this object is bound
	*/
	public int getPort() {

	return port;
	}

	/**
	* gets the total number of bytes which have been uploaded from this peer
	*/
	public long getUploaded() {

	return uploaded;
	}

	/**
	* gets the total number of bytes which have been downloaded to this peer
	*/
	public long getDownloaded() {

	return downloaded;
	}

	/** increment the toal number of bytes uploaded */
	void incUploaded(long inc)
	{
		uploaded += inc;
	}

	/** increment the toal number of bytes downloaded */
	void incDownloaded(long inc)
	{
		downloaded += inc;
	}

	/**
	* total number of bytes left to download
	*/
	public long getLeft() {

	return totalLength - downloaded;
	}

	/**
	* returns true if this peer id is our own peer id
	*/
	public boolean isLocalhost(byte[] peerId) {

	if (peerId.length == this.peerId.length) {
	    
	    for (int i = 0; i < this.peerId.length; i++) 
		if (this.peerId[i] != peerId[i]) return false;
	    return true;
	}

	return false;
	}

	public double getRate()
	{
		double totalRate = 0;
		Iterator it = connectionsToRegister.iterator();
		while( it.hasNext() )
		{
			Connection con = (Connection)it.next();
			totalRate += con.getRate();
		}
		return totalRate;
	}

	public double getUploadRate()
	{
		double totalRate = 0;
		Iterator it = connectionsToRegister.iterator();
		while( it.hasNext() )
		{
			Connection con = (Connection)it.next();
			totalRate += con.getUploadRate();
		}
		return totalRate;
	}
}
