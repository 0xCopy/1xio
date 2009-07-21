
package org.jtorrent.msgdata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.jtorrent.msgdata.BencodedMessage;
import org.jtorrent.msgdata.Utils;
import org.jtorrent.ptop.*;

/**
 * This class makes the .torrent files used by bit torrent to tell peers and
 * the tracker about a download.
 *
 * @author Hunter Payne
 */
public class MakeBitTorrentFile {

    /**
     * Makes a .torrent file for a single file download
     *
     * @param torrentFile -- the location of the .torrent file
     * @param origin -- file to be download
     * @param trackerUrl -- url of the tracker
     */
    public MakeBitTorrentFile(File torrentFile, File origin, 
			      String trackerUrl) {
	
	long length = origin.length();
	int pieceSize = (int)Math.pow(2, 20);
	int numPieces = (int)Math.ceil((double)length / (double)pieceSize);

	try {

	    // make a store which allows access to the downloadable file
	    SingleFileStore sfstore = 
		new SingleFileStore(origin, pieceSize, length, true);
	    HashCheckStore store = 
		new HashCheckStore(sfstore, numPieces, pieceSize, 0);

	    // make the .torrent file
	    saveFile(torrentFile, origin, store, pieceSize, trackerUrl, null);

	} catch (IOException ioe) {

	    ioe.printStackTrace();
	}
    }

    /**
     * Makes a .torrent file for a multiple file download
     *
     * @param torrentFile -- the location of the .torrent file
     * @param originDir -- directory where the files to be downloaded are 
     * stored
     * @param originFiles -- a list of file objects which represent the
     * files in the download
     * @param trackerUrl -- url of the tracker
     */
    public MakeBitTorrentFile(File torrentFile, File originDir, 
			      ArrayList originFiles, String trackerUrl) {

	long totalLength = 0;
	ArrayList files = new ArrayList();
	int pathLength = originDir.getAbsolutePath().length();

	// construct the files list for the directory store
	for (int i = 0; i < originFiles.size(); i++) {

	    File file = (File)originFiles.get(i);
	    totalLength += file.length();
	    String path = file.getAbsolutePath().substring(pathLength);
	    files.add(new FileInfo(path, file.length()));
	}

	int pieceSize = (int)Math.pow(2, 20);
	int numPieces = 
	    (int)Math.ceil((double)totalLength / (double)pieceSize);

	try {

	    // make a store which controls access to the downloadable files
	    DirectoryStore dstore = new DirectoryStore(originDir, pieceSize, 
						       files);
	    HashCheckStore store = 
		new HashCheckStore(dstore, numPieces, pieceSize, 0);

	    // make the .torrent file
	    saveFile(torrentFile, originDir, store, pieceSize, trackerUrl, 
		     files);
	
	} catch (IOException ioe) {

	    ioe.printStackTrace();
	}
    }

    // actually creates the .torrent file
    private void saveFile(File torrentFile, File origin, 
			  HashCheckStore store, int pieceSize, 
			  String trackerUrl, ArrayList files) {

	// make the message object
	BencodedMessage message = new BencodedMessage();

	// and add its values
	message.setStringValue("announce", trackerUrl);
	BencodedMessage infoMessage = new BencodedMessage();

	if (files == null) {

	    infoMessage.setIntValue("length", (int)store.getStoreLength());

	} else {

	    ArrayList filesList = new ArrayList();

	    for (int i = 0; i < files.size(); i++) {

		FileInfo info = (FileInfo)files.get(i);
		BencodedMessage fileMessage = new BencodedMessage();
		fileMessage.setIntValue("length", (int)info.fileLength);
		ArrayList filePathList = new ArrayList();
		StringTokenizer st = new StringTokenizer(info.filePath);

		while (st.hasMoreElements()) 
		    filePathList.add((String)st.nextElement());
		
		fileMessage.setListValue("path", filePathList);
		filesList.add(fileMessage);
	    }

	    infoMessage.setListValue("files", filesList);
	}

	infoMessage.setStringValue("name", origin.getName());
	infoMessage.setIntValue("piece length", pieceSize);

	try {
	
	    infoMessage.setStringValue("pieces", store.getHashs());

	} catch (Exception e) { e.printStackTrace(); }

	// add the info message to the main message
	message.setDictValue("info", infoMessage);

	try {
	
	    // write the message object to the specified file
	    FileOutputStream fos = new FileOutputStream(torrentFile);
	    fos.write(message.getMessageBytes());
	    fos.flush();
	    fos.close();

	} catch (IOException ioe) {

	    ioe.printStackTrace();
	}
    }

    /**
     * allows command line calling of this functionality
     */
    public static void main(String args[]) {

	if (args.length < 3) {

	    System.err.println("Usage: java org.jtorrent.msgdata.MakeBitTorrentFile <bit torrent file to create> <tracker url> <origin file list>");
	    System.exit(1);
	}

	String torrentfile = args[0];
	String trackerUrl = args[1];
	int numFiles = args.length - 2;

	try {

	    File torrentFile = new File(torrentfile);

	    if (numFiles == 1) {

		File origin = new File(args[2]);

		if (origin.exists()) {
		    
		    if (!origin.isDirectory()) {

			// make the .torrent file to download a single file
			new MakeBitTorrentFile(torrentFile, origin, 
					       trackerUrl);
		    } else {

			// make the .torrent file to download all the files
			// in the specified directory
			String list[] = origin.list();
			ArrayList files = new ArrayList();

			for (int i = 0; i < list.length; i++) {

			    File file = new File(args[i + 2]);
			    files.add(file);
			}

			new MakeBitTorrentFile(torrentFile, origin, files, 
					       trackerUrl);
		    }
		} else System.err.println("origin file " + 
					  origin.getAbsolutePath() + 
					  " doesn't exist");
	    } else {

		ArrayList files = new ArrayList();
		String common = null;

		// find the common root of all the specified files
		for (int i = 0; i < numFiles; i++) {

		    File file = new File(args[i + 2]);
		    
		    if (common != null) {

			String filename = file.getAbsolutePath();
			
			if (filename.length() > common.length()) {

			    filename = filename.substring(0, common.length());

			} else if (filename.length() < common.length()) {
			    
			    common = common.substring(0, filename.length());
			}
			
			while ((filename.length() > 0) && 
			       (!filename.equals(common))) {			    
			    filename = 
				filename.substring(0, filename.length() - 1);
			    common = common.substring(0, common.length() - 1);
			}
		    } else common = file.getAbsolutePath();

		    if (file.exists()) files.add(file);
		    else System.err.println("WARNING: origin file " + 
					    file.getAbsolutePath() + 
					    " doesn't exist");
		}

		while ((common.length() > 0) && 
		       (!common.endsWith(File.pathSeparator)))
		    common = common.substring(0, common.length() - 1);

		// make the .torrent file for the multiple file download
		new MakeBitTorrentFile(torrentFile, new File(common), files, 
				       trackerUrl);
	    }
	} catch (Exception e) {

	    e.printStackTrace();
	}
    }
}
