
package org.jtorrent.installer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * This is a custom ANT task which adds an entry to the /etc/mailcap file
 * to configure a browser to run the a plugin when a file with a specific
 * mime type is downloaded.  If the current process doesn't have permission
 * to edit the /etc/mailcap file, or this platform doesn't have an /etc/mailcap
 * file, this task does nothing.
 *
 * @author Hunter Payne
 */
public class AddMimeTypeTask extends Task {

    private String type;
    private String command;
    private static File mailcap;

    static {

	mailcap = new File("/etc/mailcap");
    }

    /**
     * This method executes the task
     */
    public void execute() throws BuildException {

	if ((mailcap.exists()) && (mailcap.canWrite())) {

	    String append = type + "; " + command + "\n";

	    try {

		FileOutputStream fos = new FileOutputStream(mailcap, true);
		DataOutputStream dos = new DataOutputStream(fos);
		dos.writeChars(append);
		dos.flush();
		dos.close();

	    } catch (IOException ioe) {

		ioe.printStackTrace();
	    }
	}
    }

    /** sets the mime type to add */
    public void setType(String type) { this.type = type; }

    /** sets the command to launch the plugin */
    public void setCommand(String command) { this.command = command; }
}
