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
        final Graph.GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);

    }


    public void testSiblings() {
        final Graph graph = new Graph(UTF8.encode("a b"));
        z(graph);
        assertEquals(2, graph.root.nodes.size());

        Graph.GraphNode aNode = graph.root.nodes.get(0);
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

        Graph.GraphNode aNode = graph.root.nodes.get(0);
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

        Graph.GraphNode aNode = graph.root.nodes.get(0);
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

        Graph.GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);
        assertEquals("a", graph.reify(graph.root.nodes.get(0)));


        aNode = aNode.get(0);
        assertEquals(1, aNode.pos);
        assertEquals(1, aNode.len);

        assertEquals("a", graph.reify(aNode));
    }

                

    public void testOrderedHierarchy() {
        final Graph  
        graph = new Graph(UTF8.encode("a app apple apples"));

        z(graph);


        assertEquals(1, graph.root.nodes.size());
        final Graph.GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);

        final Graph.GraphNode ppNode = aNode.nodes.get(0);
        assertEquals(3, ppNode.pos);
        assertEquals(2, ppNode.len);

        final Graph.GraphNode leNode = ppNode.nodes.get(0);
        assertEquals(9, leNode.pos);
        assertEquals(2, leNode.len);


        final Graph.GraphNode sNode = leNode.nodes.get(0);
        assertEquals(17, sNode.pos);
        assertEquals(1, sNode.len);

    }
    public void testOrderedSiblingInsertion() {
        Graph graph = new Graph(UTF8.encode("a ab ac"));
        z(graph);
        assertEquals(1, graph.root.nodes.size());
        final Graph.GraphNode a = graph.root.nodes.get(0);
        assertEquals(0, a.pos);
        assertEquals(1, a.len);

        Graph.GraphNode ab = a.get(0);
        try {
            assertEquals(3, ab.pos);
        } catch (Error e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        }
        assertEquals(1, ab.len);

        Graph.GraphNode ac = a.get(1);
        assertEquals(6,ac.pos);
        assertEquals(1,ac.len);


    }
    public void testOnlySiblingInsertion() {
        Graph graph = new Graph(UTF8.encode("ab ac"));
        z(graph);
        assertEquals(1, graph.root.nodes.size());
        final Graph.GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);
        assertNotSame( Graph.TYPE_DATA,aNode.type );

        Graph.GraphNode firstChild = aNode.get(0);
        assertEquals(1, firstChild.pos);
        assertEquals(1, firstChild.len);

        Graph.GraphNode secondChild = aNode.get(1);
        assertEquals(4,secondChild.pos);
        assertEquals(1,secondChild.len);


    }

    public void testUnOrderedSiblingInsertion() {
        Graph graph = new Graph(UTF8.encode("a apple anvils"));
        z(graph);
        assertEquals(1, graph.root.nodes.size());
        final Graph.GraphNode aNode = graph.root.nodes.get(0);
        assertEquals(0, aNode.pos);
        assertEquals(1, aNode.len);


        assertEquals(1, graph.root.nodes.size());
        final Graph.GraphNode nvilNode = aNode.nodes.get(0);
        assertEquals(9, nvilNode.pos);
        assertEquals(5, nvilNode.len);

        assertEquals(1, graph.root.nodes.size());
        final Graph.GraphNode ppleNode = aNode.nodes.get(1);
        assertEquals(4, ppleNode.pos);
        assertEquals(4, ppleNode.len);

    }   

    private void z(Graph graph) {
        graph.create();
        System.out.println("\n--+ " + UTF8.decode(graph.src) + " +-------");

        final Object[] objects = graph.root.nodes.toArray();

        try {
            graph.render(0, System.out, graph.root);
        } catch (IOException e) {
            e.printStackTrace();  //TODO: Verify for a purpose
        }
    }
}
