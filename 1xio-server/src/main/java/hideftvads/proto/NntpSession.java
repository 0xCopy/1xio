package hideftvads.proto;

import java.nio.*;
import java.nio.channels.*;
import java.io.*;

/**
 * User: jim
 * Date: Jul 12, 2009
 * Time: 3:49:13 AM
 */
public class NntpSession {
    public NntpClientLifeCycle lifecyle = NntpClientLifeCycle.exec;

    public String AUTHINFO$20USER="xxx";
    public String AUTHINFO$20PASS="xxx";

    public ByteBuffer groups;
    public int gcursor;
    public FileChannel gchannel;
    public RandomAccessFile gfile;
}
