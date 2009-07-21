
package org.jtorrent.ptop;

/**
 * object which represents one file in a DirectoryStore
 *
 * @author Hunter Payne
 */
public class FileInfo {

    /** relative name of the file */
    public String filePath;

    /** length of the file */
    public long fileLength;

    /**
     * @param path -- relative name of the file
     * @param length -- length of the file
     */
    public FileInfo(String path, long length) {

	filePath = path;
	fileLength = length;
    }
}
