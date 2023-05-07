package solver.ls;

public class Node {
    public Node prev;
    public int val;
    public Node next;

    public Node(Node p, int v, Node n) {
        prev = p;
        val = v;
        next = n;
    }

}
