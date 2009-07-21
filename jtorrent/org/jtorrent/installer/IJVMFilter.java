
package org.jtorrent.installer;

/**
 * Implementations of this interface are FilenameFilters which can identify 
 * a specific type of JVM.  
 */
public interface IJVMFilter extends java.io.FilenameFilter {

    /** path to the java executable */
    public String getCommand();

    /** name of this JVM suitable for display to the user */
    public String getName();
}
