package alg;

import java.util.*;

/**
 * User: jim
 * Date: Jun 8, 2009
 * Time: 12:40:31 AM
 */
class GraphNode   {
    int type, pos, len;
    List<GraphNode> nodes;
//     private  final GraphNode[] A ;

    
    public GraphNode(final int pos, int len, byte type
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
        return  nodes.get(i);
    }
}
