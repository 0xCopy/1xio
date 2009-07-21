
package org.jtorrent.installer;

import java.io.File;

/**
 * This object represents a JVM, its location, and how to run its commands
 *
 * @author Hunter Payne
 */
public class JVM extends Object {

    private String name;
    private File location;
    private String command;

    /**
     * Builds a new JVM data object
     *
     * @param name -- name of the JVM for display to the user
     * @param location -- location of the JVM's main directory
     * @param command -- command to execute to run the JVM
     */
    public JVM(String name, File location, String command) {

	this.name = name;
	this.location = location;
	this.command = command;
    }

    /** access for the name of this JVM */
    public String getName() { return name; }

    /** access for the command for invoking the JVM */
    public String getCommand() { return command; }

    /** access for the location of this JVM */
    public File getLocation() { return location; }
 
    /** for debugging */
   public String toString() {

	return name + ":" + command + ":" + location;
    }
}
