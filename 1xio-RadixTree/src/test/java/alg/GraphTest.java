package alg;

import com.thoughtworks.xstream.*;
import junit.framework.*;

import java.io.*;
import java.nio.charset.*;

/**
 * User: jim
 * Date: Jun 8, 2009
 * Time: 5:06:17 AM
 */
public class GraphTest extends TestCase {

    private static final Charset UTF8 = Charset.forName("UTF8");
    private static final XStream X_STREAM = new XStream();


    public void setUp() {


        UTF8.encode("apple app anvil a").rewind();

    }

    public void tearDown() {
        // Add your code here
    }

    public void testCreate() {
        final Graph graph = new Graph(UTF8.encode("apple app anvil a"));
        graph.create();
    }

    public void testSingular() {
        final Graph graph = new Graph(UTF8.encode("a"));
        z(graph);

        assertEquals(1, graph.root.nodes.size());
        final GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);

    }


    public void testSiblings() {
        final Graph graph = new Graph(UTF8.encode("a b"));
        z(graph);
        assertEquals(2, graph.root.nodes.size());

        GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);
        assertEquals("a", graph.reify(graph.root.nodes.get(0)));

        aNode = graph.root.nodes.get(1);
        assertEquals(2, aNode.pos);
        assertEquals(1, aNode.len);
        assertEquals("b", graph.reify(graph.root.nodes.get(1)));


    }


    public void testJuxtaposedSiblings() {
        final Graph graph = new Graph(UTF8.encode("b a"));
        z(graph);
        assertEquals(2, graph.root.nodes.size());

        GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(2, aNode.pos);
        assertEquals(1, aNode.len);
        assertEquals("a", graph.reify(graph.root.nodes.get(0)));


        aNode = graph.root.nodes.get(1);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);

        assertEquals("b", graph.reify(graph.root.nodes.get(1)));
    }


    public void testChildAdd() {
        final Graph graph = new Graph(UTF8.encode("a aa"));
        z(graph);
        assertEquals(1, graph.root.nodes.size());

        GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);
        assertEquals("a", graph.reify(graph.root.nodes.get(0)));


        aNode = aNode.get(0);
        assertEquals(3, aNode.pos);
        assertEquals(1, aNode.len);

        assertEquals("a", graph.reify(aNode));
    }

    public void testChildPrecedes() {
        final Graph graph = new Graph(UTF8.encode("aa a"));
        z(graph);
        assertEquals(1, graph.root.nodes.size());

        GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);
        assertEquals("a", graph.reify(graph.root.nodes.get(0)));


        aNode = aNode.get(0);
        assertEquals(1, aNode.pos);
        assertEquals(1, aNode.len);

        assertEquals("a", graph.reify(aNode));
    }


    public void testOrderedSiblingInsertion() {
        Graph graph = new Graph(UTF8.encode("a anvil apples"));
        z(graph);
        assertEquals(1, graph.root.nodes.size());
        final GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);

        GraphNode nvilNode = aNode.get(0);
        assertEquals(4, nvilNode.pos);
        assertEquals(1, nvilNode.len);

        GraphNode pplesNode = aNode.get(1);
        assertEquals(9,pplesNode.pos);
        assertEquals(4,pplesNode.len);


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
        assertEquals(9, nvilNode.pos);
        assertEquals(5, nvilNode.len);

        assertEquals(1, graph.root.nodes.size());
        final GraphNode ppleNode = aNode.nodes.get(1);
        assertEquals(4, ppleNode.pos);
        assertEquals(4, ppleNode.len);

    }

    public void testOrderedHierarchy() {
        final Graph  
        graph = new Graph(UTF8.encode("a app apple apples"));

        z(graph);


        assertEquals(1, graph.root.nodes.size());
        final GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);

        final GraphNode ppNode = aNode.nodes.get(0);
        assertEquals(3, ppNode.pos);
        assertEquals(2, ppNode.len);

        final GraphNode leNode = ppNode.nodes.get(0);
        assertEquals(9, leNode.pos);
        assertEquals(2, leNode.len);


        final GraphNode sNode = leNode.nodes.get(0);
        assertEquals(17, sNode.pos);
        assertEquals(1, sNode.len);

    }

    private void z(Graph graph) {
        graph.create();
        System.out.println("\n--+ " + UTF8.decode(graph.src) + " +------------------");

        final Object[] objects = graph.root.nodes.toArray();

        try {
            graph.render(0, System.out, graph.root);
        } catch (IOException e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        }
    }
}
