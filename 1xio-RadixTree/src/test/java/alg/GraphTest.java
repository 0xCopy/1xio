package alg;

import com.thoughtworks.xstream.*;
import junit.framework.*;

import java.nio.*;
import java.nio.charset.*;

/**
 * User: jim
 * Date: Jun 8, 2009
 * Time: 5:06:17 AM
 */
public class GraphTest extends TestCase {

    private static final String C = "apple app anvil a";
    private static final Charset UTF8 = Charset.forName("UTF8");
    private static final ByteBuffer SRC = UTF8.encode(C);
    private static final XStream X_STREAM = new XStream();


    public void setUp() {


        SRC.rewind();

    }

    public void tearDown() {
        // Add your code here
    }

    public void testCreate() {
        final Graph graph = new Graph(SRC);


        graph.create();

    }
//
//    public void testPrint() {
//
//
//        Graph graph;
//
//
//        graph = new Graph(UTF8.encode("apple app a"));
//
//
//        z(graph);
//
//        graph = new Graph(UTF8.encode("apple a app"));
//
//
//        z(graph);
//
//        graph = new Graph(UTF8.encode("apple app"));
//
//
//        z(graph);
//
//    }

    public void testSingular() {
        Graph graph = new Graph(UTF8.encode("a"));
        z(graph);

        assertEquals(1, graph.root.nodes.size());
        final GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);


    }


    public void testOrderedSiblingInsertion() {
        Graph graph = new Graph(UTF8.encode("a anvil apples"));
        z(graph);

        assertEquals(1, graph.root.nodes.size());
        final GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);

        assertEquals(1, graph.root.nodes.size());
        final GraphNode nvilNode = aNode.nodes.get(0);
        assertEquals(4, nvilNode.pos);
        assertEquals(4, nvilNode.len);

        assertEquals(1, graph.root.nodes.size());
        final GraphNode ppleNode = aNode.nodes.get(1);
        assertEquals(10, ppleNode.pos);
        assertEquals(5, ppleNode.len);


    }

    public void testUnOrderedSiblingInsertion() {
        Graph graph = new Graph(UTF8.encode("a apple anvils"));
        z(graph);

        assertEquals(1, graph.root.nodes.size());
        final GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);


        assertEquals(1, graph.root.nodes.size());
        final GraphNode nvilNode = aNode.nodes.get(0);
        assertEquals(4, nvilNode.pos);
        assertEquals(4, nvilNode.len);

        assertEquals(1, graph.root.nodes.size());
        final GraphNode ppleNode = aNode.nodes.get(1);
        assertEquals(10, ppleNode.pos);
        assertEquals(5, ppleNode.len);
        
    }

    public void testOrderedHierarchy() {
        Graph graph;
        graph = new Graph(UTF8.encode("a app apple"));

        z(graph);


        assertEquals(1, graph.root.nodes.size());
        final GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);

        final GraphNode ppNode = aNode.nodes.get(0);
        assertEquals(4, ppNode.pos);
        assertEquals(2, ppNode.len);

        final GraphNode leNode = aNode.nodes.get(0);
        assertEquals(10, leNode.pos);
        assertEquals(2, leNode.len);


    }

    private void z(Graph graph) {
        graph.create();
        System.err.println("\n--+ " + UTF8.decode(graph.src) + " +------------------");
        X_STREAM.toXML(graph, System.err);
    }
}
