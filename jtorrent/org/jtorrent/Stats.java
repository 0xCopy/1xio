
package org.jtorrent;

import java.util.Hashtable;
import java.util.Vector;

/**
Provides a container for all application statistics. These statistics are used
by all application components.
<p>
A set of statistics is simply a map of key names to values. Certain keys
represent substatistics. For example, there will be connection statistics for
each connected peer, such as bytes transferred. Each of these substats is
another map of key names to values. Any <code>Stats</code> object may
contain any number of others.

@author Dave Blau
@version 0.1
*/
public class Stats
{
	private static Hashtable    statMap = new Hashtable();
	private static Hashtable substatMap = new Hashtable();

	// Available statistics
	public static final Integer CONN               = new Integer(100);
	public static final Integer CONN_IP            = new Integer(101);
	public static final Integer CONN_PORT          = new Integer(102);
	public static final Integer CONN_DIR           = new Integer(10300);
	public static final Integer CONN_DIR_THROTTLE  = new Integer(10301);
	public static final Integer CONN_DIR_BYTES     = new Integer(10302);
	public static final Integer CONN_DIR_RATE_AVG  = new Integer(10303);
	public static final Integer CONN_DIR_RATE_INST = new Integer(10304);

	public static final Integer FILE               = new Integer(200);
	public static final Integer FILE_SIZE          = new Integer(201);
	public static final Integer FILE_PIECE_SIZE    = new Integer(202);
	public static final Integer FILE_NUM_PIECES    = new Integer(203);
	public static final Integer FILE_PIECE         = new Integer(20400);
	public static final Integer FILE_PIECE_HASH    = new Integer(20401);
	public static final Integer FILE_PIECE_CONN    = new Integer(20402);
	public static final Integer FILE_PIECE_BYTES   = new Integer(20403);

	/**
	Retrieve the value of a particular stat.

	@param name The name of the stat
	@return The value of the stat
	@throws IllegalArgumentException If the name is unrecognized
	*/
	public static synchronized long get( Integer name )
		throws IllegalArgumentException
	{
			Long ret = (Long)statMap.get( name );
			if( null == ret )
			{
				ret = new Long(0);
				statMap.put( name, ret );
			}
			return ret.longValue();
	}

	
	/**
	Set the value of a particular stat.

	@param name The name of the stat
	@param value The value to which to set the stat
	@throws IllegalArgumentException If the name is unrecognized
	*/
	public static synchronized void set( Integer name, long value )
		throws IllegalArgumentException
	{
		try
		{
			statMap.put( name, new Long(value) );
		}
		catch ( ArrayIndexOutOfBoundsException aioobe )
		{
			throw new IllegalArgumentException( aioobe.getMessage() );
		}
	}

	
	/**
	Clears the value of a particular stat.

	@param name The name of the stat
	@throws IllegalArgumentException If the name is unrecognized
	*/
	public static void reset( Integer name )
		throws IllegalArgumentException
	{
		set( name, 0 );
	}

	
	/**
	Updates the value of a particular stat. The given increment will be added
	to the value of the stat.

	@param name The name of the stat
	@param incr The value to add to the stat
	@return The new value of the stat
	@throws IllegalArgumentException If the name is unrecognized
	*/
	public static long update( Integer name, long incr )
		throws IllegalArgumentException
	{
		long ret = incr + get( name );
		set( name, ret );
		return ret;
	}

	/**
	Creates a new substatistic of the given type.

	@param type The type of substat to create
	@return The index of this substat
	@throws IllegalArgumentException If the name is unrecognized
	*/
	public static synchronized int newSub( Integer type )
		throws IllegalArgumentException
	{
		try
		{
			Vector vec = (Vector)substatMap.get( type );
			if( null == vec )
			{
				vec = new Vector();
				substatMap.put( type, vec );
			}
			vec.add( new Stats() );
			return vec.size() - 1;
		}
		catch ( ArrayIndexOutOfBoundsException aioobe )
		{
			throw new IllegalArgumentException( aioobe.getMessage() );
		}
	}

	/**
	Returns a substatistic of the given type.

	@param type The type of substat to create
	@param which The index of the particular substat
	@return The substat
	@throws IllegalArgumentException If the name or index is unrecognized
	*/
	public static synchronized Stats getSub( Integer type, int which )
		throws IllegalArgumentException
	{
		try
		{
			return (Stats)((Vector)substatMap.get( type )).get( which );
		}
		catch ( ArrayIndexOutOfBoundsException aioobe )
		{
			throw new IllegalArgumentException( aioobe.getMessage() );
		}
	}

	/**
	Delete a substatistic's values.

	@param type The type of substat to create
	@param which The index of the particular substat
	@throws IllegalArgumentException If the name or index is unrecognized
	*/
	public static synchronized void delSub( Integer type, int which )
		throws IllegalArgumentException
	{
		try
		{
			((Vector)substatMap.get( type )).set( which, null );
		}
		catch ( ArrayIndexOutOfBoundsException aioobe )
		{
			throw new IllegalArgumentException( aioobe.getMessage() );
		}
	}
}
