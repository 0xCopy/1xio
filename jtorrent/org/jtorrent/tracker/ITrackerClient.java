
package org.jtorrent.tracker;

import java.io.IOException;
import java.util.List;

/**
 * this interface is implemented by classes which interact with the tracker
 *
 * @author Hunter Payne
 */
public interface ITrackerClient {

    /** gets the list of peers as bencoded messages */
    public List getPeerList();

    /** sets the refresh interval to recheck the tracker */
    public void setRefreshInterval(long interval);

    /** sends the started message */
    public void started() throws IOException;

    /** sends the stopped message */
    public void stopped() throws IOException;

    /** sends the completed message */
    public void completed() throws IOException;
}
