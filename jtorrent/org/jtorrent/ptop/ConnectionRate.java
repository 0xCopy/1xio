
package org.jtorrent.ptop;

import java.util.Date;

import org.jtorrent.Config;
import org.jtorrent.Stats;

/**
Provides a class for manipulating connection bandwidth rates.

@author Hunter Payne, Dave Blau
@version 0.2
*/
public class ConnectionRate
{
	private long maxRatePeriod;
	private long maxPause;
	private long startTime;
	private long lastTime;

	private Stats dirStats;
	
	/**
	Creates a new rate monitor.

	@param which The handle assigned to this connection by newSub()
	@param dir The direction handled by this rate monitor
	*/
	public ConnectionRate( int which, int dir )
	{
		maxPause = Integer.parseInt(Config.get("rate_max_recalculate_interval"));
		int fudge = Integer.parseInt(Config.get("rate_upload_fudge"));
		startTime = lastTime = (new Date()).getTime() - fudge;

		Stats connStats = Stats.getSub( Stats.CONN, which );
		connStats.newSub( Stats.CONN_DIR );
		dirStats = connStats.getSub( Stats.CONN_DIR, dir );
		dirStats.reset( Stats.CONN_DIR_BYTES );
	}

	public void updateRate(long amount)
	{
		long now = (new Date()).getTime();
		long newBytes = dirStats.update( Stats.CONN_DIR_BYTES, amount );

		dirStats.set( Stats.CONN_DIR_RATE_AVG, 1000 * newBytes / ( now - startTime ) );
		dirStats.set( Stats.CONN_DIR_RATE_INST, 1000 * amount / ( now - lastTime ) );
		lastTime = now;
	}

	public double getRate()
	{
		if ((new Date()).getTime() - lastTime > maxPause)
			updateRate(0);

		return dirStats.get( Stats.CONN_DIR_RATE_INST );
	}
}
