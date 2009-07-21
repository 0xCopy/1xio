
package org.jtorrent.tracker;

/**
 * This interface is implemented by classes which manage the info of all the 
 * peer's info
 * 
 * @author Hunter Payne
 */
public interface IPeerInfoManager {

    /**
     * finds the peer with the specified peer id and info hash
     *
     * @param infoHash -- hash of the info part of the .torrent bencoded 
     * message
     * @param peerId -- id of the peer
     */
    public IPeerInfo getPeerInfo(byte[] infoHash, byte[] peerId);

    /**
     * makes a new peer info object
     *
     * @param peerId -- id of the new peer
     * @param peerIp -- ip of the new peer
     * @param port -- port of the new peer
     * @param infoHash -- infoHash of the new peer
     */
    public IPeerInfo makeNewPeerInfo(byte[] peerId, byte[] peerIp, int port, 
				     byte[] infoHash);

    /** saves the peer to the DB or disk or where it is stored */
    public void savePeer(IPeerInfo peer);

    /** gets a list of all the peer objects for a specified info hash */
    public java.util.ArrayList getAllPeers(byte[] infoHash);

    /** updates the last access time of the peer */
    public void updateAccess(IPeerInfo peer);

    /** checks and removes peers that have timed out */
    public void timeoutPeers(long timeout);

    /** closes this manager and clears its caches */
    public void close();
}
