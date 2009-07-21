
package org.jtorrent.tracker;

import java.io.*;
import java.sql.*;
import java.util.Date;
import java.util.ArrayList;

import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * This class manages the info of all the peers on behalf of the tracker
 * 
 * @author Hunter Payne
 */
public class SQLPeerInfoManager implements IPeerInfoManager {

    private DataSource dataSource;
    private TrackerServlet log;

    /**
     * Creates a new peer manager object
     *
     * @param servlet -- for logging
     * @param dataSourceName -- name of the database connection to use
     */
    public SQLPeerInfoManager(TrackerServlet servlet, String dataSourceName) {

	log = servlet;
	dataSource = null;

	try {

	    Context initCtx = new InitialContext();
	    Context envCtx = (Context) initCtx.lookup("java:comp/env");

	    // Look up our data source
	    dataSource = (DataSource)envCtx.lookup(dataSourceName);

	} catch (Exception e) {

	    e.printStackTrace();
	}

	if (dataSource == null) System.exit(1);
    }

    private Connection getConnection() throws SQLException {

	return dataSource.getConnection();
    }

    /**
     * finds the peer with the specified peer id and info hash
     *
     * @param infoHash -- hash of the info part of the .torrent bencoded 
     * message
     * @param peerId -- id of the peer
     */
    public IPeerInfo getPeerInfo(byte[] infoHash, byte[] peerId) {

	Connection con = null;
	PreparedStatement prstmt = null;
	ResultSet rs = null;

	try {

	    con = getConnection();
	    prstmt = 
		con.prepareStatement("SELECT * FROM PEERS WHERE peerId = ?");
	    prstmt.setBytes(1, peerId);
	    rs = prstmt.executeQuery();

	    while (rs.next()) {

		byte rowInfoHash[] = rs.getBytes("info_hash");

		if ((rowInfoHash.length == infoHash.length) &&
		    (rowInfoHash.length == 20)) {

		    boolean matched = true;

		    for (int i = 0; i < 20; i++) 
			if (rowInfoHash[i] != infoHash[i])
			    matched = false;

		    if (matched) {

			PeerInfo info = 
			    new PeerInfo(peerId, rs.getBytes("peer_id"), 
					 rs.getInt("port"), 
					 rs.getLong("last_access"), 
					 rowInfoHash, rs.getLong("downloaded"),
					 rs.getLong("uploaded"), 
					 rs.getBytes("peer_cache"));
			rs.close();
			rs = null;
			prstmt.close();
			prstmt = null;
			con.close();
			con = null;

			return info;
		    }
		}
	    }

	    rs.close();
	    rs = null;
	    prstmt.close();
	    prstmt = null;
	    con.close();
	    con = null;

	} catch (Exception e) {

	    log.log(e.getMessage(), e);

	    try {

		if (rs != null) rs.close();

	    } catch (Exception e1) {}

	    try {

		if (prstmt != null) prstmt.close();

	    } catch (Exception e2) {}

	    try {

		if (con != null) con.close();

	    } catch (Exception e3) {}
	}

	return null;
    }

    /**
     * makes a new peer info object
     *
     * @param peerId -- id of the new peer
     * @param peerIp -- ip of the new peer
     * @param port -- port of the new peer
     * @param infoHash -- infoHash of the new peer
     */
    public IPeerInfo makeNewPeerInfo(byte[] peerId, byte[] peerIp, int port, 
				     byte[] infoHash) {

	Connection con = null;
	PreparedStatement prstmt = null;
	long lastAccess = (new Date()).getTime();
	
	try {

	    con = getConnection();
	    prstmt = 
		con.prepareStatement("INSERT INTO PEERS (peer_id, peer_ip, port, info_hash, last_access, downloaded, uploaded, peer_cache) VALUES (?, ?, ?, ?, ?, ?, ?, NULL)");
	    prstmt.setBytes(1, peerId);
	    prstmt.setBytes(2, peerIp);
	    prstmt.setInt(3, port);
	    prstmt.setBytes(4, infoHash);
	    prstmt.setLong(5, lastAccess);
	    prstmt.setLong(6, 0);
	    prstmt.setLong(7, 0);
	    
	    prstmt.executeUpdate();
	    prstmt.close();
	    prstmt = null;
	    con.close();
	    con = null;

	    return (new PeerInfo(peerId, peerIp, port, lastAccess, infoHash, 
				 0, 0, null));

	} catch (Exception e) {

	    log.log(e.getMessage(), e);

	    try {

		if (prstmt != null) prstmt.close();

	    } catch (Exception e2) {}

	    try {

		if (con != null) con.close();

	    } catch (Exception e3) {}
	}

	return null;
    }

    /** saves the peer to the DB  */
    public void savePeer(IPeerInfo peer) {

	Connection con = null;
	PreparedStatement prstmt = null;

	try {

	    con = getConnection();

	    if (peer.getPeerCache() != null) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(peer.getPeerCache());

		prstmt = 
		    con.prepareStatement("UPDATE PEERS SET peer_ip = ?, port = ?, info_hash = ?, last_access = ?, downloaded = ?, uploaded = ?, peer_cache = ? WHERE peer_id = ?");
		prstmt.setBytes(7, baos.toByteArray());
		prstmt.setBytes(8, peer.getPeerId());

	    } else {

		prstmt = 
		    con.prepareStatement("UPDATE PEERS SET peer_ip = ?, port = ?, info_hash = ?, last_access = ?, downloaded = ?, uploaded = ?, peer_cache = NULL WHERE peer_id = ?");
		prstmt.setBytes(7, peer.getPeerId());
	    }

	    prstmt.setBytes(1, peer.getPeerIp());
	    prstmt.setInt(2, peer.getPort());
	    prstmt.setBytes(3, peer.getInfoHash());
	    prstmt.setLong(4, peer.getLastAccess());
	    prstmt.setLong(5, peer.getDownloaded());
	    prstmt.setLong(6, peer.getUploaded());

	    prstmt.executeQuery();

	    prstmt.close();
	    prstmt = null;
	    con.close();
	    con = null;

	} catch (Exception e) {

	    log.log(e.getMessage(), e);

	    try {

		if (prstmt != null) prstmt.close();

	    } catch (Exception e2) {}

	    try {

		if (con != null) con.close();

	    } catch (Exception e3) {}
	}
    }

    /** gets a list of all the peer objects for a specified info hash */
    public java.util.ArrayList getAllPeers(byte[] infoHash) {

    	Connection con = null;
	PreparedStatement prstmt = null;
	ResultSet rs = null;

	try {

	    ArrayList ret = new ArrayList();
	    con = getConnection();
	    prstmt = con.prepareStatement("SELECT peer_id, peer_ip, port, info_hash FROM PEERS where info_hash = ?");
	    prstmt.setBytes(1, infoHash);
	    rs = prstmt.executeQuery();

	    while (rs.next()) {

		byte peerId[] = rs.getBytes("peer_id");
		byte peerIp[] = rs.getBytes("peer_ip");
		int port = rs.getInt("port");
		
		PeerInfo peer = new PeerInfo();
		peer.setPeerId(peerId);
		peer.setPeerIp(peerIp);
		peer.setPort(port);
		peer.setInfoHash(infoHash);
		ret.add(peer);
	    }

	    rs.close();
	    rs = null;
	    prstmt.close();
	    prstmt = null;
	    con.close();
	    con = null;
	    return ret;

	} catch (Exception e) {

	    log.log(e.getMessage(), e);

	    try {

		if (rs != null) rs.close();

	    } catch (Exception e1) {}

	    try {

		if (prstmt != null) prstmt.close();

	    } catch (Exception e2) {}

	    try {

		if (con != null) con.close();

	    } catch (Exception e3) {}
	}

	return null;
    }

    /** updates the last access time of the peer */
    public void updateAccess(IPeerInfo peer) {

	peer.updateAccess();
	savePeer(peer);
    }

    /** checks and removes peers that have timed out */
    public void timeoutPeers(long interval) {

	Connection con = null;
	PreparedStatement prstmt = null;

	try {

	    con = getConnection();
	    prstmt = 
		con.prepareStatement("DELETE FROM PEERS WHERE last_access < ?");
	    prstmt.setLong(1, (new Date()).getTime() - interval);

	    prstmt.executeQuery();

	    try {

		prstmt.close();

		// the above call is giving us problems
	    } catch (Exception useless) { }

	    prstmt = null;
	    con.close();
	    con = null;

	} catch (Exception e) {

	    log.log(e.getMessage(), e);

	    try {

		if (prstmt != null) prstmt.close();

	    } catch (Exception e2) {}

	    try {

		if (con != null) con.close();

	    } catch (Exception e3) {}
	}
    }

    /** closes this manager and clears its caches */
    public void close() {

	Connection con = null;
	Statement stmt = null;

	try {

	    con = getConnection();
	    stmt = con.createStatement();

	    // special shutdown command of simple DB!!!
	    stmt.execute("SHUTDOWN NOWAIT");

	    stmt.close();
	    stmt = null;
	    con.close();
	    con = null;

	} catch (Exception e) {

	    log.log(e.getMessage(), e);

	    try {

		if (stmt != null) stmt.close();

	    } catch (Exception e2) {}

	    try {

		if (con != null) con.close();

	    } catch (Exception e3) {}
	}
    }
}
