
package org.jtorrent.plugin;

import java.net.URL;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.JFileChooser;

import org.jtorrent.Config;

import org.jtorrent.msgdata.BencodedMessage;
import org.jtorrent.ptop.Downloader;
import org.jtorrent.ptop.Origin;
import org.jtorrent.ptop.HashCheckStore;

/**
 * This class starts the Bit Torrent downloader GUI.  It uses the MonitorFrame
 * class to show the user the status of the download.  It extends the 
 * HeadlessPlugin class which actually starts the downloader and connection
 * manager.
 *
 * @author Hunter Payne
 */
public class Plugin extends HeadlessPlugin {

    private MonitorFrame monitor;

    /**
     * Starts the downloader GUI client
     *
     * @param url -- URL of the bittorrent file
     */
    public Plugin(URL url) throws Exception {

	super(url);
    }

    /**
     * Starts the downloader GUI client
     *
     * @param file -- the .torrent file
     */
    public Plugin(File file) throws Exception {

	super(file);
    }

    /**
     * Starts the downloader GUI client
     *
     * @param message -- the .torrent file
     */
    public Plugin(BencodedMessage message) 
	throws Exception {

	super(message);
    }

    protected Downloader makeDownloader(HashCheckStore store, 
					BencodedMessage message, 
					HeadlessPlugin plugin, 
					File selectedFile) 
    throws Exception {

	Downloader downloader = 
	    super.makeDownloader(store, message, plugin, selectedFile);
	BencodedMessage info = (BencodedMessage)message.getDictValue("info");
	byte hashs[] = info.getStringBytes("pieces");
	int pieceSize = info.getIntValue("piece length");
	boolean bitfield[] = store.getBitfield();

	long pad = 0;

	for (int i = 0; i < bitfield.length - 1; i++)
	    if (bitfield[i]) pad += pieceSize;

	if (bitfield[bitfield.length - 1]) {

	    long storeLength = store.getStoreLength();
	    pad += (storeLength % pieceSize);
	}

	// create the GUI
	monitor = new MonitorFrame("Downloading " + selectedFile.getName(),
				   selectedFile.getAbsolutePath(), 
				   store.getStoreLength(), pad, downloader);

	downloader.getMonitor().updateStatus("Starting to download " + 
					     selectedFile.getName());

	System.out.println("starting " + monitor);
	return downloader;
    }

    // currently unused
    class TorrentFilter extends javax.swing.filechooser.FileFilter {

	public boolean accept(File f) {

	    return f.getName().endsWith(".torrent");
	}

	public String getDescription() {

	    return "Bit Torrent Files";
	}
    }

    /**
     * allows the user to select where to store the downloaded file
     *
     * @param message -- the bencoded .torrent file 
     */
    protected File getSaveFile(BencodedMessage message) {

	File defaultFile = super.getSaveFile(message);
	JFileChooser chooser = new JFileChooser();
	//	chooser.setFileFilter(new TorrentFilter());
	BencodedMessage info = (BencodedMessage)message.getDictValue("info");
	
	if (info.get("length") == null) 
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

	chooser.setSelectedFile(defaultFile);

	// show the chooser dialog
	int selected = chooser.showSaveDialog(null);

	// if the user selected a file
	if (selected == JFileChooser.APPROVE_OPTION) 
	    return chooser.getSelectedFile();

	// if they pressed cancel or closed the dialog
	return null;
    }

    /**
     * called to notify the GUI of an error
     *
     * @param msg -- error message
     */
    public void error(String msg) {

	System.err.println("Error: " + msg);
	if (monitor != null) monitor.error(msg);
    }

    /**
     * called to notify the GUI of some new event
     *
     * @param msg -- notification message
     */
    public void updateStatus(String msg) {

	System.out.println(msg);
	if (monitor != null) monitor.updateStatus(msg);
    }

    /**
     * called to update the progress of the download
     *
     * @param curr -- current progress
     * @param total -- total number of operations to complete
     */
    public void updateProgress(int curr, int total) {

	System.out.println("" + curr + " of " + total + " complete");
	if (monitor != null) monitor.updateProgress(curr, total);
    }

    public static void main(String args[]) {

	if (args.length != 2) {

	    System.err.println("Usage: java org.jtorrent.plugin.Plugin <bit torrent file> <config file>");
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
	    new Plugin(torrentFile);

	} catch (Exception e) {

	    e.printStackTrace();
	}
    }
}
