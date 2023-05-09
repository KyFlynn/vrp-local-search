package solver.util;

public class Node {
    public Node prev;
    public int val;
    public int vehicle;
    public Node next;

    public Node(Node p, int c, int v, Node n) {
        prev = p;
        val = c;
        vehicle = v;
        next = n;
    }

}
