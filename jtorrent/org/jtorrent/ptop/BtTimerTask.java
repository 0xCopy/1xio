
package org.jtorrent.ptop;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

/**
 * This class allows for classes which implement Runnable to be have their
 * run() methods to be called at a regular specified interval
 *
 * @author Hunter Payne
 */
public class BtTimerTask extends TimerTask {

    private static Timer timer = new Timer(true);

    private Runnable run;
    private long lastRun;
    private long scheduleInterval;
    private boolean stopped;

    /**
     * Starts this task
     *
     * @param run -- object whoes run method will be called
     * @param scheduleInterval -- how ofter to run this object's run method
     */
    public BtTimerTask(Runnable run, long scheduleInterval) {

	this.run = run;
	timer.schedule(this, scheduleInterval * 1000, scheduleInterval * 1000);
	stopped = false;
    }

    /**
     * stops execution of this task
     */
    public boolean cancel() {

	super.cancel();
	stopped = true;
	return true;
    }

    /**
     * called by the timer
     */
    public void run() {

	if (!stopped) {
	    
	    lastRun = (new Date()).getTime();

	    try {

		run.run();

	    } catch (Exception e) {

		e.printStackTrace();
	    }
	}
    }

    /**
     * the time of the last execution of this task
     */
    public long scheduledExecutionTime() {

	return lastRun;
    }
}
