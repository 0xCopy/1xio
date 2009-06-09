package alg;

import java.util.*;
import java.util.concurrent.*;

/**
 * User: jim
 * Date: Jun 8, 2009
 * Time: 12:40:31 AM
 */
class GraphNode implements Comparable<GraphNode> {
    int type, pos, len;
    List<GraphNode> nodes;
    private Graph graph;

    public GraphNode(final Graph graph) {
        this.graph = graph;
    }


    /**
     * this is splitting a bigger key.  we move the original child nodes to the new Node, we accept a list of replacement nodes, ussually a synthetic node which is sibling of this copy node.
     *
     * <pre>
     * e.g. Nodes in Tree ["app -+ apple "] adding ["anvil"]  creates synthetic node "a" with children 
     * 
     * [a      ] type=0;len=1;
     * [ nvil  ] type=1;len=4;
     * [ pp    ] type=1;len=2;
     * [   le  ] type=1;len=2;
     * </pre> 
     * 
     * 
     * @param parent an existing node 
     * @param splitPoint the number of chars in the new shorter word/key
     * @param newGenerationSiblings when adding "anvil" above, the 
     */
    public GraphNode(final GraphNode parent, final int splitPoint, final GraphNode... newGenerationSiblings) {

        this(
                parent.pos + splitPoint,
                parent.len - splitPoint,
                parent.type,
                parent.graph,
                (GraphNode[]) parent.nodes.toArray(newGenerationSiblings));
        //the original node is now a synthetic node
        parent.type = 0;

        //         the original length is shortened because of splitting. but remains the one true origin offset of the subtree

        parent.len -= splitPoint;
        final List<GraphNode> ns = new CopyOnWriteArrayList<GraphNode>(newGenerationSiblings);
        ns.add(this);
        final GraphNode[] objects = ns.toArray(newGenerationSiblings);
        Arrays.sort(objects);

        parent.nodes = ns;
    }

    public GraphNode(final GraphNode orig) {
        this(orig.pos, orig.len, orig.type, orig.graph, (GraphNode[]) orig.nodes.toArray());
    }

    public GraphNode(final int pos, final int len, final int type, final Graph graph, final GraphNode... nodes) {
        this.pos = pos;
        this.len = len;
        this.type = type;
        this.graph = graph;
        this.nodes = new CopyOnWriteArrayList<GraphNode>(nodes);
    }


    public int compareTo(final GraphNode other) {

        int l = 0;
        int c;
        final int i = Math.min(len, other.len);
        while ((0) == (c = graph.src.get(pos + l) - graph.src.get(other.pos + l))) {
            l++;
            if (l > i)
                return other.len - len;
        }
        return c;
    }

    public void add(final GraphNode progress) {
        nodes.add(progress);
        Arrays.sort(nodes.toArray());
    }
    
}
