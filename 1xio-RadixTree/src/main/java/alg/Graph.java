package alg;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;


/**
 * this is a key graph of src ByteBuffer input token indexes.  the Node is a Triple {position,length,type} pointing to the graph
 * <p/>
 * User: jim
 * Date: Jun 7, 2009
 * Time: 9:17:33 PM
 */
public class Graph {
    final ByteBuffer src;


    static final int TYPE_DATA = 1;
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
                final boolean active = insertionCursor != null;
                boolean differ = false;
                int direction = 0;
                while (src.hasRemaining()
                        && !(isWhite = ((inByte = src.get()) < MINA))
                        && active
                        && !(overflow = (progress.len >= insertionCursor.len))
                        && (0 == (direction = (inByte - (src.get(insertionCursor.pos + progress.len)))))) {
                    join(src, progress);
                }


                if (!isWhite) {
                    if (direction <= 0) {
                        join(src, progress);
                    }

                    if (overflow || (active && progress.len > insertionCursor.len)) {
                        insertionCursor = handleOverflow(src, insertionCursor, progress);
                    } else {
                        if (active)
                            insertionCursor = handleBifurcate(insertionCursor, progress, direction);

                    }
                } else {
                    break;
                }
                 
                if (!src.hasRemaining()) join(src, progress);
            }
        }
    }

    private void join(final ByteBuffer src, final GraphNode progress) {
        progress.len = src.position() - progress.pos;
    }

    GraphNode handleBifurcate(GraphNode insertionCursor, final GraphNode progress, int direction) {

        final int splitPoint = progress.len;


        if (direction < 0) {
            progress.pos = insertionCursor.pos + splitPoint;
            progress.len = insertionCursor.len - splitPoint;

            insertionCursor.len = progress.pos - insertionCursor.pos;
            progress.nodes = insertionCursor.nodes;
            insertionCursor.nodes = new CopyOnWriteArrayList<GraphNode>(new GraphNode[]{progress});
            return null;
        } else {
            //create synthetic midpoint
            GraphNode synth = new GraphNode(insertionCursor.pos + splitPoint, insertionCursor.len - splitPoint, (byte) insertionCursor.type);
            int prior = insertionCursor.len;

            synth.nodes = insertionCursor.nodes;
            insertionCursor.nodes = new CopyOnWriteArrayList<GraphNode>(new GraphNode[]{synth, progress});
            insertionCursor.len -= splitPoint;
            insertionCursor.type = 0;
            progress.pos += splitPoint;
            progress.len -= prior - splitPoint;
            return null;
        }
//        return null;
    }

    GraphNode handleOverflow(final ByteBuffer src, GraphNode insertionCursor, final GraphNode progress) {
        progress.pos += insertionCursor.len;
        progress.len -= insertionCursor.len;

        if (insertionCursor.nodes != null && !insertionCursor.nodes.isEmpty()) {
            final GraphNode[] graphNodes = insertionCursor.nodes.toArray(new GraphNode[insertionCursor.nodes.size()]);

            int ix = Arrays.binarySearch(graphNodes, progress, comparator);

            if (ix >= 0) {
                final GraphNode olderNode = insertionCursor.get(ix);
                return insertionCursor = olderNode;
            } else {
                ix = -ix - 1;
                ((CopyOnWriteArrayList) insertionCursor.nodes).add(ix, progress);
                /*
                final ReentrantLock lock = ((CopyOnWriteArrayList) insertionCursor.nodes).lock;
                lock.lock();
                try {
                    Object[] elements = ((CopyOnWriteArrayList) insertionCursor.nodes).getArray();
                    int len = elements.length;
                    if (ix > len || ix < 0)
                    throw new IndexOutOfBoundsException("Index: "+ ix +
                                        ", Size: "+len);
                    Object[] newElements;
                    int numMoved = len - ix;
                    if (numMoved == 0)
                    newElements = Arrays.copyOf(elements, len + 1);
                    else {
                    newElements = new Object[len + 1];
                    System.arraycopy(elements, 0, newElements, 0, ix);
                    System.arraycopy(elements, ix, newElements, ix + 1,
                             numMoved);
                    }
                    newElements[ix] = progress;
                    ((CopyOnWriteArrayList) insertionCursor.nodes).setArray(newElements);
                } finally {
                    lock.unlock();
                }
                
                 */
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
        if (depth > 20) throw new Error("suspect recursion");
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

    /**
     * User: jim
     * Date: Jun 8, 2009
     * Time: 12:40:31 AM
     */
    static class GraphNode {
        int type, pos, len;
        List<GraphNode> nodes;


        public GraphNode(final int pos, int len, int type
        ) {
            this.pos = pos;
            this.len = len;
            this.type = type;

        }

        public GraphNode() {


        }

        @Override
        public String toString() {
            return "GraphNode{" +
                    "len=" + len +
                    ", pos=" + pos +
                    '}';
        }

        public GraphNode get(int i) {
            return nodes.get(i);
        }
    }
}
