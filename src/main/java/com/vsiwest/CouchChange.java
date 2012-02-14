package com.vsiwest;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: jim
 * Date: 2/14/12
 * Time: 4:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class CouchChange {
    public long seq;
    public String id;
    public ArrayList <TreeMap<String,String>>changes;
    public boolean deleted;
}
