/*
The MIT License

Copyright (c) 2008 Tahseen Ur Rehman, Javid Jamae

http://code.google.com/p/radixtree/

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package ds.tree;

import alg.Pair;
import javolution.text.Text;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Implementation for Radix tree {@link RadixTree}
 *
 * @author Tahseen Ur Rehman (tahseen.ur.rehman {at.spam.me.not} gmail.com)
 * @author Javid Jamae
 */
public class RadixTreeImpl<T> extends Pair<RadixTreeNode<T>, AtomicLong> implements RadixTree<T> {
//    private static final Iterable<Pair<Text,T>>[] EMPTYPAIRS = new Iterable<Pair<Text, T>>[]{};
//
//    private RadixTreeNode<T> $1;
//
//    private AtomicLong $2;

    /**
     * Create a Radix Tree with only the default node root.
     */
    public RadixTreeImpl() {

        super(new RadixTreeNode<T>(), new AtomicLong(0));
        $1().key = Text.EMPTY;
    }

    @SuppressWarnings("unchecked")
    public T find(Text key) {
        Visitor<T> visitor = new Visitor<T>() {

            T result = null;

            public void visit(Text key, RadixTreeNode<T> parent,
                              RadixTreeNode<T> node) {
                if (node.real)
                    result = node.value;
            }

            public Object getResult() {
                return result;
            }
        };

        $(key, visitor);

        return (T) visitor.getResult();
    }

    public boolean delete(Text key) {
        Visitor<T> visitor = new Visitor<T>() {
            boolean delete = false;

            public void visit(Text key, RadixTreeNode<T> parent,
                              RadixTreeNode<T> node) {
                delete = node.real;

                // if it is a real node
                if (delete) {
                    // If there no children of the node we need to
                    // delete it from the its parent children list
                    if (node.nodes.size() == 0) {
                        final List<RadixTreeNode<T>> childern = parent.nodes;
                        Iterator<RadixTreeNode<T>> it = childern.iterator();

                        while (it.hasNext()) {
                            if (it.next().key.equals(node.key)) {
                                it.remove();
                                break;
                            }
                        }

                        // if parent is not real node and has only one child
                        // then they need to be merged.
                        if (parent.nodes.size() == 1 && !parent.real) {
                            mergeNodes(parent, parent.nodes.get(0));
                        }
                    } else {
                        if (node.nodes.size() == 1) {
                            // we need to merge the only child of this node with
                            // itself
                            mergeNodes(node, node.nodes.get(0));
                        } else { // we jus need to mark the node as non real.
                            node.real = false;
                        }
                    }
                }
            }

            /**
             * Merge a child into its parent node. Opertaion only valid if it is
             * only child of the parent node and parent node is not a real node.
             *
             * @param parent
             *            The parent Node
             * @param child
             *            The child Node
             */
            void mergeNodes(RadixTreeNode<T> parent,
                            RadixTreeNode<T> child) {
                parent.key = parent.key.plus(child.key);
                parent.real = child.real;
                parent.value = child.value;
                parent.nodes = child.nodes;
            }

            public Object getResult() {
                return delete;
            }
        };

        $(key, visitor);

        if ((Boolean) visitor.getResult()) {
//            set$2(get$2() - 1);

            $2().decrementAndGet();
        }


        return (Boolean) visitor.getResult();
    }

    /*
     * (non-Javadoc)
     * @see ds.tree.RadixTree#insert(java.lang.Text, java.lang.Object)
     */
    public RadixTree<T> insert(Text key, T value) throws DuplicateKeyException {
        try {
            insert(key, $1(), value);
        } catch (DuplicateKeyException e) {
            // re-throw the exception with 'key' in the message
            throw new DuplicateKeyException(Text.intern("Duplicate key: '").plus(key).plus("'"));
        }
//        set$2(get$2() + 1);
        $2().incrementAndGet();
        return this;
    }

    /**
     * Recursively insert the key in the radix tree.
     *
     * @param key   The key to be inserted
     * @param node  The current node
     * @param value The value associated with the key
     * @throws DuplicateKeyException If the key already exists in the database.
     */
    public RadixTree<T> insert(Text key, RadixTreeNode<T> node, T value)
            throws DuplicateKeyException {

        int numberOfMatchingCharacters = node.getNumberOfMatchingCharacters(key);

        // we are either at the $1 node
        // or we need to go down the tree
        if (!node.key.isBlank()
                && numberOfMatchingCharacters != 0
                && (numberOfMatchingCharacters >= key.length()
                || numberOfMatchingCharacters < node.key.length())) {
            if (numberOfMatchingCharacters == key.length() && numberOfMatchingCharacters == node.key.length()) {

                if (node.real) {
                    throw new DuplicateKeyException(Text.intern("Duplicate key"));
                } else {
                    node.real = value != null;
                    node.value = value;
                }
            }
            // This node need to be split as the key to be inserted
            // is a prefix of the current node key
            else {
                if (numberOfMatchingCharacters > 0
                        && numberOfMatchingCharacters < node.key.length()) {
                    RadixTreeNode<T> n1 = new RadixTreeNode<T>();
                    n1.key = node.key.subtext(numberOfMatchingCharacters, node.key.length());
                    n1.real = node.real;
                    n1.value = node.value;
                    n1.nodes = node.nodes;

                    node.key = key.subtext(0, numberOfMatchingCharacters);
                    node.real = false;
                    node.nodes = new ArrayList<RadixTreeNode<T>>();
                    node.nodes.add(n1);

                    if (numberOfMatchingCharacters >= key.length()) {
                        node.value = value;
                        node.real = true;
                    } else {
                        RadixTreeNode<T> n2 = new RadixTreeNode<T>();
                        n2.key = key.subtext(numberOfMatchingCharacters, key.length());
                        n2.real = true;
                        n2.value = value;
                        node.nodes.add(n2);
                    }
                }
                // this key need to be added as the child of the current node
                else {
                    RadixTreeNode<T> n = new RadixTreeNode<T>();
                    n.key = node.key.subtext(numberOfMatchingCharacters, node.key.length());
                    n.nodes = node.nodes;
                    n.real = node.real;
                    n.value = node.value;

                    node.key = key;
                    node.real = true;
                    node.value = value;

                    node.nodes.add(n);
                }


            }

        }
        // there is a exact match just make the current node as data node
        else {
            boolean flag = false;
            Text newText = key.subtext(numberOfMatchingCharacters, key.length());
            for (RadixTreeNode<T> child : node.nodes) {
                if (child.key.startsWith(newText.charAt(0) + "")) {
                    flag = true;
                    insert(newText, child, value);
                    break;
                }
            }

            // just add the node as the child of the current node
            if (flag == false) {
                RadixTreeNode<T> n = new RadixTreeNode<T>();
                n.key = newText;
                n.real = true;
                n.value = value;

                node.nodes.add(n);
            }
        }

        return this;
    }

    public ArrayList<T> searchPrefix(Text key, int recordLimit) {
        ArrayList<T> keys = new ArrayList<T>();

        RadixTreeNode<T> node = searchPefix(key, $1());

        if (node != null) {
            if (node.real) {
                keys.add(node.value);
            }
            addAll(node, keys, recordLimit);
        }

        return keys;
    }

    RadixTreeImpl<T> addAll(RadixTreeNode<T> parent, ArrayList<T> keys, int limit) {
        Queue<RadixTreeNode<T>> queue = new LinkedList<RadixTreeNode<T>>();

        queue.addAll(parent.nodes);

        while (!queue.isEmpty()) {
            RadixTreeNode<T> node = queue.remove();
            if (node.real) {
                keys.add(node.value);
            }

            if (keys.size() == limit) {
                break;
            }

            queue.addAll(node.nodes);
        }
        return this;
    }

    RadixTreeNode<T> searchPefix(Text key, RadixTreeNode<T> node) {
        RadixTreeNode<T> result = null;

        int numberOfMatchingCharacters = node.getNumberOfMatchingCharacters(key);

        if (numberOfMatchingCharacters == key.length() && numberOfMatchingCharacters <= node.key.length()) {
            result = node;
        } else {
            if (node.key.isBlank()
                    || numberOfMatchingCharacters < key.length()
                    && numberOfMatchingCharacters >= node.key.length()) {
                Text newText = key.subtext(numberOfMatchingCharacters, key.length());
                for (RadixTreeNode<T> child : node.nodes) {
                    if (child.key.startsWith(newText.charAt(0) + "")) {
                        result = searchPefix(newText, child);
                        break;
                    }
                }
            }
        }

        return result;
    }

    public boolean contains(Text key) {
        Visitor<T> visitor = new Visitor<T>() {
            boolean result = false;

            public void visit(Text key, RadixTreeNode<T> parent,
                              RadixTreeNode<T> node) {
                result = node.real;
            }

            public Object getResult() {
                return result;
            }
        };

        $(key, visitor);

        return (Boolean) visitor.getResult();
    }

    /**
     * visit the node those key matches the given key
     *
     * @param key     The key that need to be visited
     * @param visitor The visitor object
     */
    public RadixTree<T> $(Text key, Visitor<T> visitor) {
        if ($1() == null) {
            return null;
        }
        $(key, visitor, null, $1());
        return this;
    }

    /**
     * recursively visit the tree based on the supplied "key". calls the Visitor
     * for the node those key matches the given prefix
     *
     * @param prefix  The key o prefix to search in the tree
     * @param visitor The Visitor that will be called if a node with "key" as its
     *                key is found
     * @param parent
     * @param node    The Node from where onward to search
     */
    RadixTree<T> $(Text prefix, Visitor<T> visitor,
                   RadixTreeNode<T> parent, RadixTreeNode<T> node) {

        int numberOfMatchingCharacters = node.getNumberOfMatchingCharacters(prefix);

        // if the node key and prefix match, we found a match!
        if (numberOfMatchingCharacters == prefix.length() && numberOfMatchingCharacters == node.key.length()) {
            visitor.visit(prefix, parent, node);
        } else {
            if (node.key.isBlank()// either we are at the
                    // $1
                    || numberOfMatchingCharacters < prefix.length()
                    && numberOfMatchingCharacters >= node.key.length()) {
                // OR we need to
                // traverse the nodes
                Text newText = prefix.subtext(numberOfMatchingCharacters, prefix.length());
                for (RadixTreeNode<T> child : node.nodes) {
                    // recursively search the child nodes
                    if (child.key.startsWith(newText.charAt(0) + "")) {
                        $(newText, visitor, node, child);
                        break;
                    }
                }
            }
        }
        return this;
    }


    /**
     * Display the Trie on console. WARNING! Do not use for large Trie. For
     * testing purpose only.
     */
    @Deprecated
    public RadixTree<T> display() {
        return display(0, $1());
    }

    @Deprecated
    RadixTreeImpl<T> display(int level, RadixTreeNode<T> node) {
        for (int i = 0; i < level; i++) {
            System.out.print(" ");
        }
        System.out.print("|");
        for (int i = 0; i < level; i++) {
            System.out.print("-");
        }

        if (node.real) {
            System.out.println(node.key + "[" + node.value + "]*");
        } else {
            System.out.println(node.key);
        }

        for (RadixTreeNode<T> child : node.nodes) {
            display(level + 1, child);
        }
        return this;
    }

    public long getSize() {
        return $2().get();
    }

    public RadixTree<T> insert(Pair<Text, T> textTPair) {
    return  insert(textTPair.$1(),textTPair.$2());

    }

    public RadixTree<T> insert(String key, T value) {
        return insert(Text.intern(key), (T) value);

    }

    public RadixTree<T> insert(Pair<Text, T>... pair) {
        for (Pair<Text, T> textTPair : pair)

            insert(textTPair.$1(), textTPair.$2());

        return this;
    }

    public RadixTree<T> insert(Iterable<Pair<Text, T>>... pair) {
        ArrayList<Pair<Text, T>> x = new ArrayList<Pair<Text, T>>();

        for (Iterable<Pair<Text, T>> pairIterable : pair)
            for (Pair<Text, T> tPair : pairIterable)
                x.add(tPair);

        return insert((Pair<Text, T>[]) x.toArray());
    }
}
