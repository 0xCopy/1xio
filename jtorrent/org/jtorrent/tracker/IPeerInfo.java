
package org.jtorrent.tracker;

import java.util.ArrayList;

/**
 * This interface is implemented by classes which model the data stored by
 * tracker for one peer.
 *
 * @author Hunter Payne
 */
public interface IPeerInfo {

    /** accessor for the peer id of this peer */
    public byte[] getPeerId();

    /** accessor for the ip of this peer */
    public byte[] getPeerIp();

    /** accessor for the port of this peer */
    public int getPort();

    /** accessor for the last time this peer accessed the tracker */
    public long getLastAccess();

    /** accessor for the info hash which the peer is downloading */
    public byte[] getInfoHash();

    /** accessor for the total number of bytes downloaded by this peer */
    public long getDownloaded();

    /** accessor for the total number of bytes uploaded by this peer */
    public long getUploaded();

    /** accessor for the cached list of peers given to this peer */
    public ArrayList getPeerCache();

    /** mutator for the peer id */
    public void setPeerId(byte[] peerId);

    /** mutator for the peer's ip address */
    public void setPeerIp(byte[] peerIp);

    /** mutator for the peer's port */
    public void setPort(int port);

    /** mutator for the info hash of this peer's download */
    public void setInfoHash(byte[] infoHash);

    /** pings this peer by updating its last accessed time */
    public void updateAccess();

    /** mutator for the total number of bytes downloaded by this peer */
    public void setDownloaded(long downloaded);

    /** mutator for the total number of bytes uploaded by this peer */
    public void setUploaded(long uploaded);

    /** mutator for the list of peers to send to this peer */
    public void setPeerCache(ArrayList cache);
}
