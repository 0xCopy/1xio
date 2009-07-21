
package org.jtorrent.ptop;

import java.io.IOException;

import org.jtorrent.Config;

/**
 * This class is a Java port of the Uploaded.py module of BitTorrent 3.0.2
 *
 * @author Hunter Payne
 */
public class Uploader {

    private boolean choked;
    private boolean interested;
    private HashCheckStore store;
    private Connection connection;
    private Choker choker;
    private int maxSliceLength;

    public Uploader(HashCheckStore store, Connection connection, 
		    Choker choker) {

	maxSliceLength = Integer.parseInt(Config.get("max_slice_length"));
	this.choker = choker;
	this.connection = connection;
	this.store = store;
	choked = true;
	interested = false;
    }

    public void close() {

	choker.connectionLost(connection);
    }

    public void gotNotInterested() {

	if (interested) {

	    interested = false;
	    choker.notInterested(connection);
	}
    }

    public void gotInterested() {

	if (!interested) {

	    interested = true;
	    choker.interested(connection);
	}
    }

    public void gotRequest(int index, int begin, int length) {

	if (!interested || (length > maxSliceLength)) {

	    connection.close();
	    return;
	}

	if (!choked) {

	    try {
	
		if (store != null) {

		    byte piece[] = new byte[length];
		    SingleFileStore.ByteArrayByteChannel channel = 
			new SingleFileStore.ByteArrayByteChannel(piece);
		    store.loadPiece(channel, index, begin, length);
		    connection.sendPiece(piece, index, begin);
		}
	    } catch (IOException ioe) {
		
		ioe.printStackTrace();
	    }
	}
    }

    public void choke() {

	if (!choked) {

	    choked = true;
	    connection.sendChoke();
	}
    }

    public void unchoke() {

	if (choked) {

	    choked = false;
	    connection.sendUnchoke();
	}
    }

    public boolean isChoked() {

	return choked;
    }

    public boolean isInterested() {

	return interested;
    }
}
