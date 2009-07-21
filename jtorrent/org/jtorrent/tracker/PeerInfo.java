
package org.jtorrent.tracker;

import java.util.ArrayList;
import java.util.Date;
import java.io.*;

/**
 * This class models the data stored by tracker for one peer.
 *
 * @author Hunter Payne
 */
public class PeerInfo implements IPeerInfo {

    protected byte[] peerId;
    protected byte[] peerIp;
    protected int port;
    protected long lastAccess;
    protected byte[] infoHash;
    protected long downloaded;
    protected long uploaded;
    protected ArrayList peerCache;

    /** creates a new empty peer object */
    public PeerInfo() {

	peerId = null;
	peerIp = null;
	port = -1;
	lastAccess = (new Date()).getTime();
	infoHash = null;
	downloaded = 0;
	uploaded = 0;
	peerCache = null;
    }

    /** constructs a peer object with the specified data */
    public PeerInfo(byte[] peerId, byte[] peerIp, int port, long lastAccess, 
		    byte[] infoHash, long downloaded, long uploaded, 
		    byte[] cache) {

	this.peerId = peerId;
	this.peerIp = peerIp;
	this.port = port;
	this.infoHash = infoHash;
	this.downloaded = downloaded;
	this.uploaded = uploaded;
	this.lastAccess = lastAccess;
	peerCache = null;
	
	if (cache != null) {

	    try {
		
		ByteArrayInputStream baos = new ByteArrayInputStream(cache);
		ObjectInputStream oos = new ObjectInputStream(baos);
		peerCache = (ArrayList)oos.readObject();
		
	    } catch (Exception e) {
		
		e.printStackTrace();
	    }
	}
    }

    /** accessor for the peer id of this peer */
    public byte[] getPeerId() {

	return peerId;
    }

    /** accessor for the ip of this peer */
    public byte[] getPeerIp() {

	return peerIp;
    }

    /** accessor for the port of this peer */
    public int getPort() {

	return port;
    }

    /** accessor for the last time this peer accessed the tracker */
    public long getLastAccess() {

	return lastAccess;
    }

    /** accessor for the info hash which the peer is downloading */
    public byte[] getInfoHash() {

	return infoHash;
    }

    /** accessor for the total number of bytes downloaded by this peer */
    public long getDownloaded() {

	return downloaded;
    }

    /** accessor for the total number of bytes uploaded by this peer */
    public long getUploaded() {

	return uploaded;
    }

    /** accessor for the cached list of peers given to this peer */
    public ArrayList getPeerCache() {

	return peerCache;
    }

    /** mutator for the peer id */
    public void setPeerId(byte[] peerId) {

	this.peerId = peerId;
    }

    /** mutator for the peer's ip address */
    public void setPeerIp(byte[] peerIp) {

	this.peerIp = peerIp;
    }

    /** mutator for the peer's port */
    public void setPort(int port) {

	this.port = port;
    }

    /** mutator for the info hash of this peer's download */
    public void setInfoHash(byte[] infoHash) {

	this.infoHash = infoHash;
    }

    /** pings this peer by updating its last accessed time */
    public void updateAccess() {

	lastAccess = (new Date()).getTime();
    }

    /** mutator for the total number of bytes downloaded by this peer */
    public void setDownloaded(long downloaded) {

	this.downloaded = downloaded;
    }

    /** mutator for the total number of bytes uploaded by this peer */
    public void setUploaded(long uploaded) {

	this.uploaded = uploaded;
    }

    /** mutator for the list of peers to send to this peer */
    public void setPeerCache(ArrayList cache) {

	peerCache = cache;
    }
}
