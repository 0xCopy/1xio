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
                        && !(isWhite = ((inByte = src.get()) < MINA))
                        && !(
                        overflow = (insertionCursor != null
                                && progress.len > insertionCursor.len
                        ))
                        && (mustBifurcate = !src.hasRemaining() || progress != insertionCursor
                        && (inByte != (loftByte = src.get(insertionCursor.pos + (progress.len)))
                ))) {
                    join(src, progress);
                }


                if (isWhite) break;


                if (!src.hasRemaining()) progress.len = src.limit() - progress.pos;
                else

                    join(src, progress);
                if (overflow || (insertionCursor != null
                        && progress.len > insertionCursor.len
                ))
                    insertionCursor = handleOverflow(src, insertionCursor, progress);
                else if (mustBifurcate && src.hasRemaining())

                    insertionCursor = handleBifurcate(insertionCursor, progress);
            }
        }
    }

    private void join(ByteBuffer src, GraphNode progress) {
        progress.len = src.position() - progress.pos;
    }

    GraphNode handleBifurcate(GraphNode insertionCursor, GraphNode progress) {

        int splitPoint = progress.len;

        progress.pos = insertionCursor.pos + splitPoint;
        progress.len = insertionCursor.len - splitPoint;

        insertionCursor.len = progress.pos - insertionCursor.pos;

        progress.nodes = insertionCursor.nodes;
        insertionCursor.nodes = new CopyOnWriteArrayList<GraphNode>(new GraphNode[]{progress});


        insertionCursor = null;
        return insertionCursor;


    }

    GraphNode handleOverflow(ByteBuffer src, GraphNode insertionCursor, GraphNode progress) {
        progress.pos += insertionCursor.len;
        progress.len -= insertionCursor.len;

        if (insertionCursor.nodes == null || insertionCursor.nodes.isEmpty()) {
            insertionCursor.nodes = new CopyOnWriteArrayList<GraphNode>(new GraphNode[]{progress});
        } else {
            final GraphNode[] graphNodes = insertionCursor.nodes.toArray(new GraphNode[0]);

            int ix = Arrays.binarySearch(graphNodes, progress, comparator);

            if (ix >= 0) {
                GraphNode olderNode = insertionCursor.get(ix);
//
//            //if first byte of both nodes same
//            if (src.get(olderNode.pos) == src.get(progress.pos)) {

                //cursor moves to olderNode      
//               
          /*    src.position(src.position()-1);*/ /* progress.pos--;*/ return insertionCursor = olderNode;
//
//            } else {
//                add(progress, insertionCursor);
//                insertionCursor = progress;// 
//            }

            } else {
                ix = -ix - 1;
                progress.pos--;

                insertionCursor.nodes.add(ix, progress);
//                progress.pos--;

            }

        }
        return insertionCursor;
    }

    String reify(final GraphNode progress) {
        ByteBuffer buffer2 = src.duplicate();
        Buffer buffer3 = null;
        try {
            buffer3 = buffer2.limit(progress.pos + progress.len);
        } catch (Exception e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        }
        ByteBuffer buffer1 = (ByteBuffer) buffer3.position(progress.pos);
        final CharBuffer buffer = CHARSET.decode(buffer1);

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

        if (n.nodes != null)
            for (GraphNode node : n.nodes) {
                if (node != null) render(depth + 1, c, node);
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
            int c = 0;
            final int i = Math.min(o1.len, o2.len);
            try {
                while (l < i && (0) == (c = src.get(o1.pos + l) - src.get(o2.pos + l))) {
                    l++;
                    if (l < i) {
                    } else {
                        return o1.len - o2.len;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();  //TODO: Verify for a purpose
            }
            return c;
        }
    }

}
