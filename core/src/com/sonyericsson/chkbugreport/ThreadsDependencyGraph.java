/*
 * Copyright (C) 2016 Tuenti Technologies
 *
 * This file is part of ChkBugReport.
 *
 * ChkBugReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChkBugReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChkBugReport.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sonyericsson.chkbugreport;

import java.util.*;

/**
 * Models a threads dependency graph with threads name and lock types info.
 */
public class ThreadsDependencyGraph {
    private final Digraph digraph;
    private final Map<String, Integer> threadsNodeIds;

    public ThreadsDependencyGraph(int size) {
        digraph = new Digraph(size);
        threadsNodeIds = new HashMap<String, Integer>();
    }

    /**
     * Adds a thread if missing
     * @param threadName
     */
    public void addThread(String threadName) {
        if (!threadsNodeIds.containsKey(threadName)) {
            threadsNodeIds.put(threadName, threadsNodeIds.size());
        }
    }

    /**
     * Adds a dependency from threadNameFrom to threadNameTo with a given lockType
     * @param threadNameFrom
     * @param threadNameTo
     * @param lockType
     */
    public void addThreadDependency(String threadNameFrom, String threadNameTo, String lockType) {
        try {
            digraph.addEdge(new LabeledEdge(
                    threadsNodeIds.get(threadNameFrom),
                    threadsNodeIds.get(threadNameTo),
                    threadNameFrom,
                    threadNameTo,
                    lockType
            ));
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a map of threadName to list of dependencies.
     */
    public Map<String, Iterable<LabeledEdge>> getThreadDependencyMap() {
        Map<String, Iterable<LabeledEdge>> map = new HashMap<String, Iterable<LabeledEdge>>();
        for (Map.Entry<String, Integer> entry : threadsNodeIds.entrySet()) {
            map.put(entry.getKey(), digraph.adj(entry.getValue()));
        }
        return map;
    }

    /**
     * Gets iterable of thread names.
     */
    public Iterable<String> getThreadNames() {
        return threadsNodeIds.keySet();
    }

    /**
     * Gets a list of thread names involved in deadlock, empty if not existing.
     */
    public List<String> getDeadLock() {
        List<String> list = new ArrayList<String>();
        DirectedCycle directedCycle = new DirectedCycle(digraph);
        Iterable<Integer> cycle = directedCycle.cycle();

        if (directedCycle.hasCycle()) {
            for (Integer node : cycle) {
                for (String key : threadsNodeIds.keySet()) {
                    if (threadsNodeIds.get(key).equals(node)) {
                        list.add(key);
                        break;
                    }
                }
            }
        }
        return list;
    }

    @Override
    public String toString() {
        return "{map=" + threadsNodeIds + ",digraph=" + digraph + "}";
    }
}




/**
 *  The <tt>Digraph</tt> class represents a directed graph of vertices
 *  named 0 through <em>V</em> - 1.
 *  It supports the following two primary operations: add an edge to the digraph,
 *  iterate over all of the vertices adjacent from a given vertex.
 *  Parallel edges and self-loops are permitted.
 *  <p>
 *  This implementation uses an adjacency-lists representation, which
 *  is a vertex-indexed array of {@link Bag} objects.
 *  All operations take constant time (in the worst case) except
 *  iterating over the vertices adjacent from a given vertex, which takes
 *  time proportional to the number of such vertices.
 *  <p>
 *  For additional documentation,
 *  see <a href="http://algs4.cs.princeton.edu/42digraph">Section 4.2</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */

class Digraph {
    private static final String NEWLINE = System.getProperty("line.separator");

    private final int V;           // number of vertices in this digraph
    private int E;                 // number of edges in this digraph
    private Bag<LabeledEdge>[] adj;    // adj[v] = adjacency list for vertex v
    private int[] indegree;        // indegree[v] = indegree of vertex v

    /**
     * Initializes an empty digraph with <em>V</em> vertices.
     *
     * @param  V the number of vertices
     * @throws IllegalArgumentException if V < 0
     */
    public Digraph(int V) {
        if (V < 0) throw new IllegalArgumentException("Number of vertices in a Digraph must be nonnegative");
        this.V = V;
        this.E = 0;
        indegree = new int[V];
        adj = (Bag<LabeledEdge>[]) new Bag[V];
        for (int v = 0; v < V; v++) {
            adj[v] = new Bag<LabeledEdge>();
        }
    }

    /**
     * Returns the number of vertices in this digraph.
     *
     * @return the number of vertices in this digraph
     */
    public int V() {
        return V;
    }

    /**
     * Returns the number of edges in this digraph.
     *
     * @return the number of edges in this digraph
     */
    public int E() {
        return E;
    }


    // throw an IndexOutOfBoundsException unless 0 <= v < V
    private void validateVertex(int v) {
        if (v < 0 || v >= V)
            throw new IndexOutOfBoundsException("vertex " + v + " is not between 0 and " + (V-1));
    }

    /**
     * Adds the directed edge v->w to this digraph.
     *
     * @throws IndexOutOfBoundsException unless both 0 <= v < V and 0 <= w < V
     */
    public void addEdge(LabeledEdge labeledEdge) {
        validateVertex(labeledEdge.from());
        validateVertex(labeledEdge.to());
        adj[labeledEdge.from()].add(labeledEdge);
        indegree[labeledEdge.to()]++;
        E++;
    }

    /**
     * Returns the vertices adjacent from vertex <tt>v</tt> in this digraph.
     *
     * @param  v the vertex
     * @return the vertices adjacent from vertex <tt>v</tt> in this digraph, as an iterable
     * @throws IndexOutOfBoundsException unless 0 <= v < V
     */
    public Iterable<LabeledEdge> adj(int v) {
        validateVertex(v);
        return adj[v];
    }

    /**
     * Returns a string representation of the graph.
     *
     * @return the number of vertices <em>V</em>, followed by the number of edges <em>E</em>,
     *         followed by the <em>V</em> adjacency lists
     */
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(V + " vertices, " + E + " edges " + NEWLINE);
        for (int v = 0; v < V; v++) {
            s.append(String.format("%d: ", v));
            for (LabeledEdge l : adj[v]) {
                s.append(String.format("%d ", l.to()));
                s.append(" - " + l.label());
            }
            s.append(NEWLINE);
        }
        return s.toString();
    }

}


class Bag<Item> implements Iterable<Item> {
    private Node<Item> first;    // beginning of bag
    private int N;               // number of elements in bag

    // helper linked list class
    private static class Node<Item> {
        private Item item;
        private Node<Item> next;
    }

    /**
     * Initializes an empty bag.
     */
    public Bag() {
        first = null;
        N = 0;
    }

    /**
     * Returns true if this bag is empty.
     *
     * @return <tt>true</tt> if this bag is empty;
     * <tt>false</tt> otherwise
     */
    public boolean isEmpty() {
        return first == null;
    }

    /**
     * Returns the number of items in this bag.
     *
     * @return the number of items in this bag
     */
    public int size() {
        return N;
    }

    /**
     * Adds the item to this bag.
     *
     * @param item the item to add to this bag
     */
    public void add(Item item) {
        Node<Item> oldfirst = first;
        first = new Node<Item>();
        first.item = item;
        first.next = oldfirst;
        N++;
    }

    /**
     * Returns an iterator that iterates over the items in this bag in arbitrary order.
     *
     * @return an iterator that iterates over the items in this bag in arbitrary order
     */
    public Iterator<Item> iterator() {
        return new ListIterator<Item>(first);
    }

    // an iterator, doesn't implement remove() since it's optional
    private class ListIterator<Item> implements Iterator<Item> {
        private Node<Item> current;

        public ListIterator(Node<Item> first) {
            current = first;
        }

        public boolean hasNext() {
            return current != null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public Item next() {
            if (!hasNext()) throw new NoSuchElementException();
            Item item = current.item;
            current = current.next;
            return item;
        }
    }
}

class DirectedCycle {
    private boolean[] marked;        // marked[v] = has vertex v been marked?
    private int[] edgeTo;            // edgeTo[v] = previous vertex on path to v
    private boolean[] onStack;       // onStack[v] = is vertex on the stack?
    private Stack<Integer> cycle;    // directed cycle (or null if no such cycle)

    /**
     * Determines whether the digraph <tt>G</tt> has a directed cycle and, if so,
     * finds such a cycle.
     * @param G the digraph
     */
    public DirectedCycle(Digraph G) {
        marked  = new boolean[G.V()];
        onStack = new boolean[G.V()];
        edgeTo  = new int[G.V()];
        for (int v = 0; v < G.V(); v++)
            if (!marked[v] && cycle == null) dfs(G, v);
    }

    // check that algorithm computes either the topological order or finds a directed cycle
    private void dfs(Digraph G, int v) {
        onStack[v] = true;
        marked[v] = true;
        for (LabeledEdge labeledEdge : G.adj(v)) {

            // short circuit if directed cycle found
            if (cycle != null) return;

                //found new vertex, so recur
            else if (!marked[labeledEdge.to()]) {
                edgeTo[labeledEdge.to()] = v;
                dfs(G, labeledEdge.to());
            }

            // trace back directed cycle
            else if (onStack[labeledEdge.to()]) {
                cycle = new Stack<Integer>();
                for (int x = v; x != labeledEdge.to(); x = edgeTo[x]) {
                    cycle.push(x);
                }
                cycle.push(labeledEdge.to());
                cycle.push(v);
                assert check();
            }
        }
        onStack[v] = false;
    }

    /**
     * Does the digraph have a directed cycle?
     * @return <tt>true</tt> if the digraph has a directed cycle, <tt>false</tt> otherwise
     */
    public boolean hasCycle() {
        return cycle != null;
    }

    /**
     * Returns a directed cycle if the digraph has a directed cycle, and <tt>null</tt> otherwise.
     * @return a directed cycle (as an iterable) if the digraph has a directed cycle,
     *    and <tt>null</tt> otherwise
     */
    public Iterable<Integer> cycle() {
        return cycle;
    }


    // certify that digraph has a directed cycle if it reports one
    private boolean check() {

        if (hasCycle()) {
            // verify cycle
            int first = -1, last = -1;
            for (int v : cycle()) {
                if (first == -1) first = v;
                last = v;
            }
            if (first != last) {
                System.err.printf("cycle begins with %d and ends with %d\n", first, last);
                return false;
            }
        }

        return true;
    }

}
