
package org.jtorrent.plugin;

import java.net.URL;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;

import org.jtorrent.Config;

import org.jtorrent.ptop.*;
import org.jtorrent.msgdata.BencodedMessage;
import org.jtorrent.msgdata.Utils;
import org.jtorrent.tracker.TrackerClient;
import org.jtorrent.tracker.TrackerServlet;

/**
 * This class starts a Bit Torrent download.  It starts the downloader and
 * connection manager object which actually carry out the download.
 *
 * @author Hunter Payne
 */
public class HeadlessPlugin implements IMonitor {

    protected Downloader downloader;

    /**
     * Starts the downloader
     *
     * @param url -- URL of the bittorrent file
     */
    public HeadlessPlugin(URL url) throws Exception {

	this(BencodedMessage.readFromStream(url.openStream()));
    }

    /**
     * Starts the downloader
     *
     * @param file -- the .torrent file
     */
    public HeadlessPlugin(File file) throws Exception {

	this(BencodedMessage.readFromStream(new FileInputStream(file)));
    }

    /**
     * Starts the downloader
     *
     * @param message -- the .torrent file
     */
    public HeadlessPlugin(BencodedMessage message) 
	throws Exception {
	
	int requestPieceSize = Integer.parseInt(Config.get("download_slice_size"));
	File file = getSaveFile(message);

	if (file != null) {

	    HashCheckStore store = getStore(message, file, requestPieceSize);

	    if (store != null) {

		try {

		    downloader = makeDownloader(store, message, this, file);
		    downloader.start();

		} catch (Exception e) {

		    e.printStackTrace();
		}
	    }
	}
    }

    protected Downloader makeDownloader(HashCheckStore store, 
					BencodedMessage message, 
					HeadlessPlugin plugin, 
					File selectedFile) 
	throws Exception {

	return (new Downloader(store, message, this));
    }

    /**
     * selected the location to store the downloaded file.  Just uses the
     * default location specified by the .torrent file
     *
     * @param message -- the bencoded .torrent file 
     */
    protected File getSaveFile(BencodedMessage message) {

	BencodedMessage info = (BencodedMessage)message.getDictValue("info");
	return (new File(info.getStringValue("name")));
    }

    /**
     * Creates a HashCheckStore object responsible for managing disk
     * access and checking the hashes of each piece.
     *
     * @param message -- the bencoded .torrent file
     * @param selectedFile -- the selected file or directory where the 
     * downloaded file is to be stored
     * @param requestPieceSize -- maximum size of a piece network request
     */
    protected HashCheckStore getStore(BencodedMessage message, 
				      File selectedFile, int requestPieceSize)
	throws IOException {

	BencodedMessage info = (BencodedMessage)message.getDictValue("info");
	int length = 0;
	HashCheckStore store = null;
	int pieceSize = info.getIntValue("piece length");
	byte hashs[] = info.getStringBytes("pieces");
	
	// if this is a single file downloader
	if (info.get("length") != null) {

	    length = ((Integer)info.get("length")).intValue();

	    // make the actual store object
	    SingleFileStore sfstore = 
		new SingleFileStore(selectedFile, pieceSize, length);

	    // make the hash checker store object
	    store = new HashCheckStore(sfstore, hashs, pieceSize, 
				       requestPieceSize);
	} else {

	    // if this is a multiple file downloader
	    ArrayList list = (ArrayList)info.get("files");
	    
	    for (int i = 0; i < list.size(); i++) {
		
		Properties fileInfo = (Properties)list.get(i);
		length += ((Integer)fileInfo.get("length")).intValue();
	    }

	    // make the actual store object
	    DirectoryStore dstore = 
		new DirectoryStore(selectedFile, pieceSize, list);

	    // make the hash checker store object
	    store = new HashCheckStore(dstore, hashs, pieceSize, 
				       requestPieceSize);
	}
	
	return store;
    }

    /**
     * called to notify the GUI of an error
     *
     * @param msg -- error message
     */
    public void error(String msg) {

	System.err.println(msg);
    }

    /**
     * called to notify the GUI of some new event
     *
     * @param msg -- notification message
     */
    public void updateStatus(String msg) {

	System.out.println(msg);
    }

    /**
     * called to update the progress of the download
     *
     * @param curr -- current progress
     * @param total -- total number of operations to complete
     */
    public void updateProgress(int curr, int total) {

	System.out.println("progress " + curr + " of " + total);
    }

    /** entry point */
    public static void main(String args[]) {

	if (args.length != 2) {

	    System.err.println("Usage: java org.jtorrent.plugin.HeadlessPlugin <bit torrent file> <config file>");
	    System.exit(1);
	}

	String torrentfile = args[0];
	String propsfile = args[1];

	try {

	    File torrentFile = new File(torrentfile);
	    File configFile = new File(propsfile);

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
	
	    Config.load( configFile );
	    new HeadlessPlugin(torrentFile);

	} catch (Exception e) {

	    e.printStackTrace();
	}
    }
}

