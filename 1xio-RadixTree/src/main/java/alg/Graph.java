package alg;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;


/**
 * this is a key graph of src ByteBuffer input token indexes.  the Node is a Triple {position,length,type} pointing to the graph (todo: remove the graph reference from graph node;.
 * <p/>
 * User: jim
 * Date: Jun 7, 2009
 * Time: 9:17:33 PM
 */
public class Graph {
    final ByteBuffer src;


    static final byte TYPE_DATA = 1;
    GraphNode root = new GraphNode();
    private static final int MINA = (int) 'a' <= (int) 'A' ? (int) 'a' : (int) 'A';
    private static final Charset UTF8 = Charset.forName("UTF8");
    private static final Charset CHARSET = UTF8;
    private static final ByteBuffer EOL = UTF8.encode(",");
    public final Comparator<GraphNode> comparator;


    public Graph(final ByteBuffer src) {
        this.src = src;
        root.nodes = new ArrayList<GraphNode>();
        comparator = new GraphComparator();
    }


    /**
     * this is a lazy creation indexer of input bytes.
     * <p/>
     * nodes are added to the tree on the basis of thier first byte,
     * and worst case tree is a trie structure.  as we parse each byte of the input token, we prolong its length,
     * we end it, or we move a prospective parent into the cursor sites.
     * <p/>
     * nodes are stored in sorted arrays based on indirect bytes described by (pos,len) pairs.
     */
    public void create() {
        final ByteBuffer src = this.src.duplicate();
        newNode:
        while (src.hasRemaining()) {

            GraphNode insertionCursor = root;
            final GraphNode progress = new GraphNode(src.position(), 0, TYPE_DATA/*, this*/);

            //grab token.
            byte inByte = 0;
            while (src.hasRemaining()) {

                boolean overflow = false;
                boolean isWhite = false;
                boolean mustBifurcate = false;
                byte loftByte = -1;

                while (src.hasRemaining()
                        && !(isWhite = ((inByte = src.get()) < MINA)) //else token end checked here
                        && !(overflow = (insertionCursor != null  && ++progress.len > insertionCursor.len&& progress != insertionCursor))//overflow checked before read
                        && (mustBifurcate = progress
                        != insertionCursor
                        && (inByte
                        != (loftByte
                        = src.get(insertionCursor.pos + (progress.len))
                ))
                )
                        ) {
                    progress.len++;
                }


                if (isWhite) break;

                if (overflow) {

                    progress.pos += insertionCursor.len;
                    progress.len -= insertionCursor.len;

                    if (insertionCursor.nodes == null || insertionCursor.nodes.isEmpty()) {
                        insertionCursor.nodes = new CopyOnWriteArrayList<GraphNode>(new GraphNode[]{progress});
                    } else {
                        final GraphNode[] graphNodes = insertionCursor.nodes.toArray(new GraphNode[0]);

                        int ix = Arrays.binarySearch(graphNodes, progress, comparator);

                        if (ix >= 0) {
                        
                         
                        } else {
                            ix = -ix - 1;
                            
                            insertionCursor.nodes.add(ix, progress);
                        }
                        GraphNode olderNode = insertionCursor.get(ix);

                        //if first byte of both nodes same
                        if (src.get(olderNode.pos) == src.get(progress.pos)) {

                            //cursor moves to olderNode 
                            insertionCursor = olderNode;

                        } else {
                            add(progress, insertionCursor);
                            insertionCursor = progress;// 
                        }
                    }

                } else if (!mustBifurcate) {


                }

            }
        }
    }

    String reify(final GraphNode progress) {
        final CharBuffer buffer = CHARSET.decode((ByteBuffer) src.duplicate().limit(progress.pos + progress.len).position(progress.pos));
        return buffer.toString();
    }




    void render(int depth, PrintStream c, GraphNode n) throws IOException {
        c.print('\n');

        for (int i = 0; i < depth; i++) {
            c.write(' ');
        }
        c.print(n.toString());
        if (n.type != 0) {
            c.write(':');
            c.print(reify(n));
        }

if(n.nodes!=null)        for (GraphNode node : n.nodes) {
            render(depth + 1, c, node);
        }
    }


    public void add(final GraphNode progress, GraphNode parent) {
        if (parent.nodes == null || parent.nodes.isEmpty()) {
            parent.nodes = new CopyOnWriteArrayList<GraphNode>(new GraphNode[]{progress});
        } else {
            parent.nodes.add(progress);
            Arrays.sort(parent.nodes.toArray(), (Comparator) comparator);
        }
    }

    /**
     * User: jim
     * Date: Jun 11, 2009
     * Time: 4:56:00 PM
     */
    class GraphComparator implements Comparator<GraphNode> {

        public int compare(GraphNode o1, GraphNode o2) {

            int l = 0;
            int c;
            final int i = Math.min(o1.len, o2.len);
            while ((0) == (c = src.get(o1.pos + l) - src.get(o2.pos + l))) {
                l++;
                if (l > i)
                    return o2.len - o1.len;
            }
            return c;
        }
    }

}
