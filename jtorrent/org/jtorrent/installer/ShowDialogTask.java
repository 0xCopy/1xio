
package org.jtorrent.installer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * This task shows a message dialog to the user.
 *
 * @author Hunter Payne
 */
public class ShowDialogTask extends Task {

    private String message;

    /** shows the dialog to the user */
    public void execute() throws BuildException {

	javax.swing.JOptionPane.showMessageDialog(null, message);
    }

    /** sets message to display in the dialog */
    public void setMessage(String message) { this.message = message; }
}
