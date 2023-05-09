package solver.util;

public class Node {
    public Node prev;
    public int customer;
    public int vehicle;
    public Node next;

    public Node(Node p, int c, int v, Node n) {
        prev = p;
        customer = c;
        vehicle = v;
        next = n;
    }

}
