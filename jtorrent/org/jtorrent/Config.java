
package org.jtorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Iterator;
import java.util.Properties;

/**
Provides access to application-wide configuration data.

@author Dave Blau
@version 0.1
*/
public class Config
{
	private static File configFile   = null;
	private static Properties config = null;

	private Config() { }

	/**
	Loads the global properties from a file.

	@param path A {@link File} from which to load configuration data
	@throws FileNotFoundException if the config file couldn't be found
	@throws IOException if an I/O error occurs
	*/
	public static void load( File path )
		throws FileNotFoundException, IOException
	{
		configFile = path;
		reload();
	}

	/**
	Reloads the global properties from a file.

	@throws FileNotFoundException if the config file couldn't be found
	@throws IOException if an I/O error occurs
	*/
	public static synchronized void reload()
		throws FileNotFoundException, IOException
	{
		config = new Properties();
		config.load(new FileInputStream(configFile));
		cleanProperties();
	}

	/**
	Gets a configuration value.

	@param key The key to retrieve
	@return The value for that key, or <code>null</code>
	*/
	public static String get( String key )
	{
		return get( key, null );
	}

	/**
	Gets a configuration value, with default.

	@param key The key to retrieve
	@param defaultValue The default value if the key is not found
	@return The value for that key
	*/
	public static synchronized String get( String key, String defaultValue )
	{
		return config.getProperty( key, defaultValue );
	}

	/** cleans the commented lines out of the parsed configuration file */
	private static void cleanProperties()
	{
		Iterator it = config.keySet().iterator();

		while (it.hasNext())
		{
			String key = (String)it.next();
			String value = config.getProperty(key);
			int index = value.indexOf("#");

			if (index != -1)
				config.put( key, value.substring(0, index).trim() );
		}
	}
}
