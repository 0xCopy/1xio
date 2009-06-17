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
    public final Comparator<GraphNode> comparator;


    public Graph(final ByteBuffer src) {
        this.src = src;
        root.nodes = new ArrayList<GraphNode>();
        comparator = new GraphComparator();
    }


    /**
     * this is a lazy creation indexer of input bytes.
     * <p/>
     * nodes are anchored to the tree on the basis of thier first byte,
     * and worst case tree is a trie structure.  as we parse each byte of the input token, we prolong its length,
     * we end it , or we move a nearer-matching parent into the insertionCursor.
     * <p/>
     * nodes are stored in sorted arrays based on indirect bytes described by (pos,len) pairs.
     */
    public void create() {
        final ByteBuffer src = this.src.duplicate();
        newNode:
        while (src.hasRemaining()) {

            final int tokenStart = src.position();
            final GraphNode progress = new GraphNode(tokenStart, 0, TYPE_DATA);
            byte inByte = 0;

            GraphNode insertionCursor = null;

            progress.len = 1;
            insertionCursor = handleOverflow(src, root, progress);
            progress.pos = tokenStart;
            progress.len = 0;


            while (src.hasRemaining()) {

                boolean overflow = false;
                boolean isWhite = false;

                while (src.hasRemaining()
                        && !(isWhite = ((inByte = src.get()) < MINA))
                        && !(overflow = (insertionCursor != null
                        && progress.len >= insertionCursor.len))
                        && (insertionCursor != null) && (inByte == (src.get(insertionCursor.pos + progress.len)))) {
                    join(src, progress);
                }


                if (isWhite) break;


                join(src, progress);
                if (overflow || (insertionCursor != null && progress.len > insertionCursor.len)) {
                    insertionCursor = handleOverflow(src, insertionCursor, progress);
                    continue;
                } else {
                    if (insertionCursor != null)
                        insertionCursor = handleBifurcate(insertionCursor, progress);
                    else
                        do {
                        } while (false);
                }

            }
        }
    }

    private void join(final ByteBuffer src, final GraphNode progress) {
        progress.len = src.position() - progress.pos;
    }

    GraphNode handleBifurcate(GraphNode insertionCursor, final GraphNode progress) {

        final int splitPoint = progress.len;

        progress.pos = insertionCursor.pos + splitPoint;
        progress.len = insertionCursor.len - splitPoint;

        insertionCursor.len = progress.pos - insertionCursor.pos;

        progress.nodes = insertionCursor.nodes;
        insertionCursor.nodes = new CopyOnWriteArrayList<GraphNode>(new GraphNode[]{progress});


        insertionCursor = null;
        return insertionCursor;


    }

    GraphNode handleOverflow(final ByteBuffer src, GraphNode insertionCursor, final GraphNode progress) {
        progress.pos += insertionCursor.len;
        progress.len -= insertionCursor.len;

        if (insertionCursor.nodes != null && !insertionCursor.nodes.isEmpty()) {
            final GraphNode[] graphNodes = insertionCursor.nodes.toArray(new GraphNode[0]);

            int ix = Arrays.binarySearch(graphNodes, progress, comparator);

            if (ix >= 0) {
                final GraphNode olderNode = insertionCursor.get(ix);
                return insertionCursor = olderNode;
            } else {
                ix = -ix - 1;
                insertionCursor.nodes.add(ix, progress);
            }

        } else {
            insertionCursor.nodes = new CopyOnWriteArrayList<GraphNode>(new GraphNode[]{progress});
            return null;
        }
        return insertionCursor;
    }

    String reify(final GraphNode progress) {
        final ByteBuffer buffer2 = src.duplicate();
        Buffer buffer3 = null;
        try {
            buffer3 = buffer2.limit(progress.pos + progress.len);
        } catch (Exception e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        }
        final ByteBuffer buffer1 = (ByteBuffer) buffer3.position(progress.pos);
        final CharBuffer buffer = CHARSET.decode(buffer1);

        return buffer.toString();
    }


    void render(final int depth, final PrintStream c, final GraphNode n) throws IOException {
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
            for (final GraphNode node : n.nodes) {
                if (node != null)
                    render(depth + 1, c, node);
            }
    }


    public void add(final GraphNode progress, final GraphNode parent) {
        if (parent.nodes == null || parent.nodes.isEmpty()) {
            parent.nodes = new CopyOnWriteArrayList<GraphNode>(new GraphNode[]{progress});
        } else {
            parent.nodes.add(progress);
            //noinspection unchecked
            Arrays.sort(parent.nodes.toArray(), (Comparator) comparator);
        }
    }

    /**
     * User: jim
     * Date: Jun 11, 2009
     * Time: 4:56:00 PM
     */
    class GraphComparator implements Comparator<GraphNode> {

        public int compare(final GraphNode o1, final GraphNode o2) {

            int l = 0;
            int c = 0;
            final int i = Math.min(o1.len, o2.len);
            try {
                while (l < i && (0) == (c = src.get(o1.pos + l) - src.get(o2.pos + l))) {
                    l++;
                    if (l > i) {
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
