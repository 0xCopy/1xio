
package org.jtorrent.ptop;

import java.util.ArrayList;

import org.jtorrent.Config;

/**
 * This class is a port of the functionality in Choker.py in BitTorrent 3.0.2
 */
public class Choker implements Runnable {

    private int maxUploads;
    private int count;
    private ArrayList connections;
    private ArrayList preforder;
    private boolean interrupted;

    public Choker() {

	maxUploads = Integer.parseInt(Config.get("max_uploads"));
	connections = new ArrayList();
	preforder = new ArrayList();
	interrupted = false;
	count = 0;
	BtTimerTask task = new BtTimerTask(this, 10 * 60);
    }

    public void run() {

	count++;
	
	if (count % 3 == 0) {

	    if (!interrupted) {

		for (int i = 1; i < connections.size(); i++) {

		    Uploader uploader = 
			((Connection)connections.get(i)).getUploader();

		    if (!uploader.isInterested() || !uploader.isChoked()) {

			connections.remove(i);
			i = connections.size();
		    }
		}
	    }

	    interrupted = false;
	}

	preforder.clear();
	preforder = new ArrayList(connections);
	if (preforder.size() > 0) preforder.remove(0);

	for (int i = 0; i < preforder.size() - 1; i++) {

	    for (int j = i + 1; j < preforder.size(); j++) {

		Connection con1 = (Connection)preforder.get(i);
		Connection con2 = (Connection)preforder.get(j);

		if (con1.getRate() < con2.getRate()) {

		    Connection temp = (Connection)preforder.get(i);
		    preforder.set(i, preforder.get(j));
		    preforder.set(j, temp);
		}
	    }
	}

	rechoke();
    }

    private void rechoke() {

	if (connections.isEmpty()) return;
	Connection first = (Connection)connections.get(0);
	int count = 0;
	Uploader uploader = first.getUploader();
	
	if (count < maxUploads) {

	    uploader.unchoke();
	    if (uploader.isInterested()) count++;

	} else uploader.choke();

	for (int i = 0; i < preforder.size(); i++) {

	    Connection con = (Connection)preforder.get(i);
	    
	    if (con != first) {

		uploader = first.getUploader();
		
		if (count < maxUploads) {
		    
		    uploader.unchoke();
		    if (uploader.isInterested()) count++;
		    
		} else uploader.choke();
	    }
	}
    }

    public void connectionMade(Connection con) {

	connections.add(0, con);
	interrupted = true;
	rechoke();
    }

    public void connectionLost(Connection con) {

	if (connections.get(0) == con) interrupted = true;
	connections.remove(con);
	
	for (int i = 0; i < preforder.size(); i++) {

	    if (preforder.get(i) == con) {

		preforder.remove(i);
		i = preforder.size();
	    }
	}

	rechoke();
    }

    public void interested(Connection con) {

	if (!con.getUploader().isChoked()) rechoke();
    }

    public void notInterested(Connection con) {

	if (!con.getUploader().isChoked()) rechoke();
    }
}
