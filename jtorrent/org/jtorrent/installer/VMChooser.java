
package org.jtorrent.installer;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Vector;

/**
 * This is a simple AWT dialog to allow the user to choose a specific JVM 
 * from a list of JVMs.
 * 
 * @author Hunter Payne
 */
public class VMChooser extends Frame implements ActionListener {

    private JVM selected;
    private Vector vms;
    private List vmList;
    private Button okButton;
    private Button cancelButton;
    private boolean finished;

    /**
     * displays the dialog and returns when it has been closed by the user
     *
     * @param vms -- the list of JVMs from which to choose
     */
    VMChooser(Vector vms) {

	super("Choose Virtual Machine");

	finished = false;
	selected = null;
	this.vms = vms;
	vmList = new List(vms.size(), false);

	for (int i = 0; i < vms.size(); i++) {

	    JVM jvm = (JVM)vms.get(i);
	    vmList.add(jvm.getName());
	}

	okButton = new Button("OK");
	cancelButton = new Button("Cancel");
	okButton.addActionListener(this);
	cancelButton.addActionListener(this);
	vmList.select(0);

	Panel buttonPanel = new Panel();
	buttonPanel.add(okButton);
	buttonPanel.add(cancelButton);
	buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

	setLayout(new GridLayout(2, 1, 5, 5));
	add(vmList);
	add(buttonPanel);

	pack();
	setVisible(true);
    }

    /** returns true if this dialog has been closed by the user */
    public boolean isFinished() { return finished; }

    /** 
     * returns the selected JVM or null if none was selected
     */
    public JVM getSelected() {

	return selected;
    }

    /** called when the user presses a button */
    public void actionPerformed(ActionEvent event) {

	if (event.getSource() == okButton) {

	    int index = vmList.getSelectedIndex();
	    finished = true;
	    
	    if (index != -1) {

		selected = (JVM)vms.get(index);
		System.out.println("selected " + selected);
		dispose();
		setVisible(false);

	    } else {

		System.err.println("no JVM selected!!!");
	    }
	} else if (event.getSource() == cancelButton) {

	    finished = true;
	    dispose();
	    setVisible(false);
	}
    }
    /*
    public static void main(String args[]) {

	Vector v = new Vector();
	v.add(new JVM("test1", new java.io.File("/usr/local/j2sdk1.4.1/"), "command1"));
	v.add(new JVM("test2", new java.io.File("/usr/local/j2sdk1.4.1/"), "command2"));
	VMChooser chooser = new VMChooser(v);
	System.out.println("selected " + chooser.getSelected());
	}*/
}
