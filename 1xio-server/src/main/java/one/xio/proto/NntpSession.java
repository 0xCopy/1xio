package one.xio.proto;

import alg.Pair;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * User: jim
 * Date: Jul 12, 2009
 * Time: 3:49:13 AM
 */
public class NntpSession {
    public NntpClientLifeCycle lifecyle = NntpClientLifeCycle.exec;

  /*  
    {
        final Map<String, String> map = System.getenv();
        System.err.println(""+String.valueOf( map) );
    }*/
    public String AUTHINFO$20USER = System.getenv("NNTP_USER");
    public String AUTHINFO$20PASS = System.getenv("NNTP_PASS");   
//  uuencoded  public String FETCH_ID = "<letp55tti95uavt6nekt606mgv8ikbfusb@4ax.com>";

    public String FETCH_ID =System.getenv("NNTP_FETCH");
//  fail yenc  public String FETCH_ID = "<fuGdnebDw_jkdQXUnZ2dnUVZ_jYAAAAA@giganews.com>";
    public ByteBuffer LIST;
    public int gcursor;
    public FileChannel gchannel;
    public RandomAccessFile gfile;
    public List<Pair<Integer, LinkedList<Integer>>> groupList;
    ;
    public FileChannel bodyChannel;
    public RandomAccessFile bodyFile;
    public long bodyCursor;
    public LinkedList<Pair<Integer, LinkedList<Integer>>> bodyLines;
    public ByteBuffer BODY;
    public String outputName;
    public File bodyTmpFile;
}
