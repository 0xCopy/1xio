package hideftvads.proto;

import alg.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * User: jim
 * Date: Jul 12, 2009
 * Time: 3:49:13 AM
 */
public class NntpSession {
    public NntpClientLifeCycle lifecyle = NntpClientLifeCycle.exec;

    public String AUTHINFO$20USER = "jimn235";
    public String AUTHINFO$20PASS = "xxxx";
    public String ID = "<letp55tti95uavt6nekt606mgv8ikbfusb@4ax.com>";
    public ByteBuffer LIST;
    public int gcursor;
    public FileChannel gchannel;
    public RandomAccessFile gfile;
    public List<Pair<Integer, LinkedList<Integer>>> groupList;
    ;
    public FileChannel bchannel;
    public RandomAccessFile bfile;
    public long bcursor;
    public CopyOnWriteArrayList<Pair<Integer, LinkedList<Integer>>> bLines;
    public ByteBuffer BODY;
    public String fname;
}
