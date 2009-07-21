
package org.jtorrent.installer;

import java.io.*;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * This is a custom ANT task which removes entries with a specific mime type 
 * from the /etc/mailcap file.  If the current process doesn't have 
 * permission to edit the /etc/mailcap file, or this platform doesn't have an
 * /etc/mailcap file, this task does nothing.
 * 
 *
 * @author Hunter Payne
 */
public class RemoveMimeTypeTask extends Task {

    private String type;
    private static File mailcap;

    static {

	mailcap = new File("/etc/mailcap");
    }

    /** executes the task */
    public void execute() throws BuildException {

	if ((mailcap.exists()) && (mailcap.canWrite())) {

	    try {

		FileInputStream stream = new FileInputStream(mailcap);
		StringBuffer buf = new StringBuffer();
		byte buffer[] = new byte[1024];
		int read = -1;
		
		while ((read = stream.read(buffer)) > 0) 
		    buf.append(new String(buffer, 0, read));
		
		FileOutputStream fos = new FileOutputStream(mailcap);
		StringTokenizer st = new StringTokenizer(buf.toString(), "\n");

		while (st.hasMoreElements()) {

		    String line = (String)st.nextElement();
		    if (!line.startsWith(type)) fos.write(line.getBytes());
		}

		fos.flush();
		fos.close();

	    } catch (IOException ioe) {

		ioe.printStackTrace();
	    }
	}
    }

    /** sets the mime type to remove */
    public void setType(String type) { this.type = type; }
}
