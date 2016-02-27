package com.sonyericsson.chkbugreport;

public class LabelEdge {
    private final int v;
    private final int w;
    private final String label;

    public LabelEdge(int v, int w, String label) {
        if (v < 0) throw new IndexOutOfBoundsException("Vertex names must be nonnegative integers");
        if (w < 0) throw new IndexOutOfBoundsException("Vertex names must be nonnegative integers");
        this.v = v;
        this.w = w;
        this.label = label;
    }

    /**
     * Returns the tail vertex of the directed edge.
     * @return the tail vertex of the directed edge
     */
    public int from() {
        return v;
    }

    /**
     * Returns the head vertex of the directed edge.
     * @return the head vertex of the directed edge
     */
    public int to() {
        return w;
    }

    /**
     * Returns the label of the directed edge.
     * @return the label of the directed edge
     */
    public String label() {
        return label;
    }

    /**
     * Returns a string representation of the directed edge.
     * @return a string representation of the directed edge
     */
    public String toString() {
        return v + "->" + w + " " + label;
    }

}
