
package org.jtorrent.plugin;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;

import org.jtorrent.ptop.IMonitor;
import org.jtorrent.ptop.ConnectionRate;
import org.jtorrent.ptop.Downloader;

/**
 * This class is the GUI dialog which shows the user the status of the download
 * <br />
 *
 * @author Hunter Payne
 */
public class MonitorFrame extends JFrame implements ActionListener, IMonitor {

    private JLabel noteLabel;
    private JLabel timeLabel;
    private JLabel downloadLabel;
    private JLabel uploadLabel;
    private JProgressBar statusBar;
    private JProgressBar candyBar;
    private JButton cancelButton;
    private Downloader downloader;
    private long length;
    private double bytesAlreadyDownloaded;

    /**
     * Displays the GUI dialog which displays the status of the download
     *
     * @param title -- title of the frame
     * @param filename -- name of the file to download
     * @param operations -- number of parts of the download
     * @param length -- total length of the download
     * @param initBytes -- number of bytes previously downloaded
     * @param downloader -- downloader object
     */
    public MonitorFrame(String title, String filename, long length, 
			long initBytes, Downloader downloader) {

	super(title);

	this.downloader = downloader;
	this.length = length;
	bytesAlreadyDownloaded = (double)initBytes;
	noteLabel = new JLabel("Saving: " + filename, SwingConstants.CENTER);
	timeLabel = new JLabel("Unknown", SwingConstants.CENTER);
	downloadLabel = new JLabel("Download Rate: 0.0 KB/s", 
				   SwingConstants.CENTER);
	uploadLabel = new JLabel("Upload Rate: 0.0 KB/s", 
				 SwingConstants.CENTER);

	statusBar = new JProgressBar(0, 100);
	candyBar = new JProgressBar(0, 100);
	candyBar.setIndeterminate(true);

	cancelButton = new JButton("cancel");
	cancelButton.addActionListener(this);
	Border outBorder = BorderFactory.createRaisedBevelBorder();
	cancelButton.setBorder(outBorder);

	Container content = new JPanel();
	content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
	content.add(wrapComp(noteLabel));
	content.add(wrapComp(timeLabel));
	content.add(wrapComp(statusBar));
	content.add(wrapComp(candyBar));
	content.add(wrapComp(downloadLabel));
	content.add(wrapComp(uploadLabel));
	content.add(wrapComp(cancelButton));
	content.add(Box.createRigidArea(new Dimension(3, 3)));

	addWindowListener(new BTWindowAdapter());
	setContentPane(content);
	pack();
	setSize((int)(getWidth() * 2), getHeight());
	setVisible(true);
    }

    public class BTWindowAdapter extends WindowAdapter {

	public void windowClosing(WindowEvent e) {

	    setVisible(false);
	    dispose();
	    System.exit(0);
	}
    }

    private JComponent wrapComp(JComponent comp) {

	JPanel panel = new JPanel();
	panel.setLayout(new FlowLayout(FlowLayout.CENTER));
	panel.add(comp);
	return panel;
    }

    /**
     * called when the user presses the cancel button
     */
    public void actionPerformed(ActionEvent event) {

	if (event.getSource() == cancelButton) {
	    
	    setVisible(false);
	    dispose();
	    System.exit(0);
	}
    }

    private static String formatDouble(double d) {

	String s = Double.toString(d);
	int dotIndex = s.indexOf(".");

	if ((dotIndex != -1) && (dotIndex < s.length() - 3)) {

	    s = s.substring(0, dotIndex + 3);
	}

	return s;
    }

    /**
     * called to notify the GUI of an error
     *
     * @param msg -- error message
     */
    public void error(String msg) {

	// TODO show in dialog
	noteLabel.setText(msg);
    }

    /**
     * called to notify the GUI of some new event
     *
     * @param msg -- notification message
     */
    public void updateStatus(String msg) {

	noteLabel.setText(msg);

	if (downloader != null)
	    downloadLabel.setText("Download Rate " + 
				  formatDouble(downloader.getRate())+
				  " KB/s");
	else downloadLabel.setText("finished");
	uploadLabel.setText("Upload Rate: " + 
			    formatDouble(downloader.getUploadRate()) + " KB/s");
	double percent = 
	    (((double)downloader.getDownloaded() + bytesAlreadyDownloaded) / 
	     (double)length) * 100.0;
	timeLabel.setText(formatDouble(percent) + "% complete");
	statusBar.setValue((int)percent);
	statusBar.revalidate();
    }

    /**
     * called to update the progress of the download
     *
     * @param curr -- current progress
     * @param total -- total number of operations to complete
     */
    public void updateProgress(int curr, int total) {

	if (downloader != null)
	    downloadLabel.setText("Download Rate " + 
				  formatDouble(downloader.getRate())+
				  " KB/s");
	else downloadLabel.setText("finished");
	uploadLabel.setText("Upload Rate: " + 
			    formatDouble(downloader.getUploadRate()) + " KB/s");

	double percent = 
	    (((double)downloader.getDownloaded() + bytesAlreadyDownloaded) / 
	     (double)length) * 100.0;
	timeLabel.setText(formatDouble(percent) + "% complete");
	statusBar.setValue((int)percent);
	statusBar.revalidate();
    }
}
