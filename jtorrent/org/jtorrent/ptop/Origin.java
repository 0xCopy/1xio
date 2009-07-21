
package org.jtorrent.ptop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import org.jtorrent.Config;

import org.jtorrent.ptop.*;
import org.jtorrent.msgdata.BencodedMessage;
import org.jtorrent.msgdata.Utils;
import org.jtorrent.tracker.TrackerClient;
import org.jtorrent.tracker.TrackerServlet;

/**
 * This class starts an 'origin' peer.  The 'origin' peer never downloads 
 * data, only uploads it.  It is usually run in the same place as the tracker.
 *
 * @author Hunter Payne
 */
public class Origin implements IMonitor {

    private ConnectionManager manager;
    private TrackerClient client;

    /**
     * Starts a new origin peer
     *
     * @param file -- .torrent file
     * @param origin -- location of the actual file to upload
     */
    public Origin(File file, File origin) throws Exception {

	this(BencodedMessage.readFromStream(new FileInputStream(file)),
	     origin);
    }

    /**
     * Starts a new origin peer
     *
     * @param message -- parsed .torrent file 
     * @param origin -- location of the actual file to upload
     */
    public Origin(BencodedMessage message, File origin) 
	throws Exception {
	
	int requestPieceSize =
	    Integer.parseInt(Config.get("download_slice_size"));

	HashCheckStore store = getStore(message, origin, requestPieceSize);
	BencodedMessage infoMessage = 
	    (BencodedMessage)message.getDictValue("info");

	byte hashs[] = store.getHashs();
	byte messHashs[] = infoMessage.getStringBytes("pieces");

	manager = new ConnectionManager(infoMessage, null, store, 
					constructId());
	client = new TrackerClient(manager, message.getStringValue("announce"),
				   this);
	int keepaliveInterval = 
	    Integer.parseInt(Config.get("keepalive_interval"));
	new BtTimerTask(new Pinger(), keepaliveInterval);

	// TODO is this necessary???
	while(true) {

	    Thread.yield();
	}
    }

    class Pinger implements Runnable {

	public void run() {

	    manager.sendKeepalives();
	}
    }

    /**
     * creates a new peer id
     */
    public static byte[] constructId() {

	String name = "origin.bittorrent.org";
	String msg = (new Date()).toString() + name;
	return SHAUtils.computeStringDigest(msg);
    }

    // constructs a read only store object from which controls access to the
    // original files to upload
    private HashCheckStore getStore(BencodedMessage message, 
				    File selectedFile, int requestPieceSize)
	throws IOException {

	BencodedMessage info = (BencodedMessage)message.getDictValue("info");
	int length = 0;
	HashCheckStore store = null;
	int pieceSize = info.getIntValue("piece length");
	byte hashs[] = info.getStringBytes("pieces");
	
	if (info.get("length") != null) {

	    length = ((Integer)info.get("length")).intValue();
	    SingleFileStore sfstore = 
		new SingleFileStore(selectedFile, pieceSize, length, true);
	    store = new HashCheckStore(sfstore, hashs.length / 20, pieceSize, 
				       requestPieceSize);
	} else {
	    
	    ArrayList list = (ArrayList)info.get("files");
	    
	    for (int i = 0; i < list.size(); i++) {
		
		Properties fileInfo = (Properties)list.get(i);
		length += ((Integer)fileInfo.get("length")).intValue();
	    }

	    DirectoryStore dstore = 
		new DirectoryStore(selectedFile, pieceSize, list, true);
	    store = new HashCheckStore(dstore, hashs.length / 20, pieceSize, 
				       requestPieceSize);
	}

	// TOOD check hashs!!!
	return store;
    }

    /**
     * called to notify this class of an error
     *
     * @param msg -- error message
     */
    public void error(String msg) {

	System.err.println(msg);
    }

    /**
     * called to notify this class of some new event
     *
     * @param msg -- notification message
     */
    public void updateStatus(String msg) {

    }

    /**
     * called to update the progress of the download
     *
     * @param curr -- current progress
     * @param total -- total number of operations to complete
     */
    public void updateProgress(int curr, int total) {

    }

    /** entry point for the Origin */
    public static void main(String args[]) {

	if (args.length != 3) {

	    System.err.println("Usage: java org.jtorrent.ptop.Origin <config file> <origin file> <bit torrent file>");
	    System.exit(1);
	}

	String torrentfile = args[2];
	String propsfile = args[0];
	String origin = args[1];

	try {

	    File torrentFile = new File(torrentfile);
	    File configFile = new File(propsfile);
	    File originFile = new File(origin);

	    if (!torrentFile.exists()) {

		System.err.println("bit torrent file " + 
				   torrentFile.getAbsolutePath() + 
				   " doesn't exist");
		System.exit(1);
	    }

	    if (!configFile.exists()) {

		System.err.println("configuration file " + 
				   configFile.getAbsolutePath() + 
				   " doesn't exist");
		System.exit(1);
	    }

	    if (!originFile.exists()) {

		System.err.println("download file " + 
				   originFile.getAbsolutePath() + 
				   " doesn't exist");
		System.exit(1);
	    }

	    Config.load( configFile );
	    new Origin(torrentFile, originFile);

	} catch (Exception e) {

	    e.printStackTrace();
	}
    }
}
