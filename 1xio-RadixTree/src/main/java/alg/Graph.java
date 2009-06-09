package alg;

import java.io.*;
import java.nio.*;
import java.util.*;

//
//enum GraphNode {
//    pos(4),
//    len(4),
//
//    type(1),
//    count(1),;
//    static int position;
//    private final int size;
//    private final int offset;
//
//
//    GraphNode(int size) {
//
//        //To change body of created methods use File | Settings | File Templates.
//        this.size = size;
//        offset = init(size);
//    }
//
//    private int init(int size) {
//        int offset = position;
//        position += size;
//        return offset;
//    }
//}
//


/**
 * User: jim
 * Date: Jun 7, 2009
 * Time: 9:17:33 PM
 */
public class Graph {
    final
    ByteBuffer src;
    public ByteBuffer indexBuffer;
    RandomAccessFile indexFile;
    private static final byte TYPE_DATA = 1;
    GraphNode root = new GraphNode(this);
    private static final int MINA = (int) 'a' <= (int) 'A' ? (int) 'a' : (int) 'A';
//    private
//    GraphNode root;


    public Graph(final ByteBuffer src) {
        this.src = src;
        root.nodes = new ArrayList<GraphNode>();
        //            final File tempFile = createTempFile("AAA", "XXX");
//            indexFile = new RandomAccessFile(tempFile.getAbsolutePath(), "rwc");
//
//            indexFile.setLength(8 << 23);
//            indexBuffer = indexFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 8 << 23);

    }


    public void create() {
        final ByteBuffer src = this.src.duplicate();
        newNode:
        while (src.hasRemaining()) {

            final int p = src.position();

            GraphNode loft = root;
            final GraphNode progress = new GraphNode(p, 0, TYPE_DATA, this);

//            int l = 0;
            byte loftByte, inputByte = 0;
            while (src.hasRemaining()) {

                /**
                 * on token break...
                 */
                int l;
                try {
                    if (((inputByte = src.get()) & 0xff) < MINA) {
                        continue newNode;
                    } else {
                        l = p - src.position();
                        ++progress.len;
                    }
                } catch (Throwable e) {

                    System.err.println("create complete.");
                    return;
                }


                if (loft.len >= l) {
                    GraphNode sel = null;
                    for (final GraphNode node : loft.nodes) {
                        if (src.get(node.pos) > inputByte)//our new loft node
                        {
                            break;
                        } else {
                            if (src.get(node.pos) == inputByte) {
                                sel = node;
                                break;
                            }
                        }

                    }
                    if (sel == null) {     // no suitable parent was located 

                        loft.nodes.add(progress);

                        while (src.hasRemaining() && src.get() > ' ') {
                            progress.len++;
                        }
                        continue newNode;

                    } else {     //sel was found, so we now begin descending into sel as loft.
                        loft = sel;
                        progress.pos++;
                        progress.len = 0;
            l=0;
                    }
                }

                try {
                    final byte b = src.get(loft.pos + l);
                    if (inputByte != b) {
                        if (inputByte < b) {              //bifurcate fun fun fun!
                            new GraphNode(loft, l, progress);//implicit merge on this ctor.


                            while (src.hasRemaining() && src.get() > ' ') {
                                progress.len++;
                            }
                            continue newNode;
                        }

                    }
                    progress.pos++;
                } catch (Exception e) {
//                    e.printStackTrace();  //TODO: Verify for a purpose
                }
            }
        }
    }

    private void put(final GraphNode root, final GraphNode n) {
        if (root.len == n.len) {
            //if match assert type+data even if not previously.
            final int i = root.compareTo(n);
            if (i == 0) {
                root.type = TYPE_DATA;
                return;
            }
        }
        int splitPoint = 0;
        byte b1;
        byte b2;
        final int biggestOverlap = Math.min(n.len, root.len);
        while ((splitPoint < biggestOverlap) && ((b1 = src.get(root.pos + splitPoint)) == (b2 = src.get(n.pos + splitPoint))))
            ++splitPoint;

        if (splitPoint < biggestOverlap)//bifurcate, typical
        {
            final GraphNode synth = new GraphNode(root, splitPoint, n);
        } else {
            final GraphNode graphNode = new GraphNode(n);
            graphNode.len -= splitPoint;
            graphNode.pos += splitPoint;
            root.nodes.add(graphNode);
        }
    }
}
