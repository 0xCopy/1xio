
package org.jtorrent.ptop;

/**
 * Classes which implement this interface provide a mechanism for monitoring
 * the status of the progress of the application.
 *
 * @author Hunter Payne
 */
public interface IMonitor {

    /**
     * called to notify this implementation of an error
     *
     * @param msg -- error message
     */
    public void error(String msg);

    /**
     * called to notify this implementation of some new event
     *
     * @param msg -- notification message
     */
    public void updateStatus(String msg);

    /**
     * called to update the progress of the download
     *
     * @param curr -- current progress
     * @param total -- total number of operations to complete
     */
    public void updateProgress(int curr, int total);
}
