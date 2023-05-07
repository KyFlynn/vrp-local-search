package solver.ls;

// Doubly linked cycle with pointer to starting node
// When empty, the start node points to itself in both previous and next

public class DoublyLinkedCycle {

    public Node start;

    public DoublyLinkedCycle(int startVal, int[] values) {
        start = new Node(null, startVal, null);
        initCycle(values);
    }

    public void initCycle(int[] values) {
        if (values != null) {
            Node v1 = start;
            for (int i = 0; i < values.length; i++) {
                Node v2 = new Node(v1, values[i], null);
                v1.next = v2;
                if (i == values.length - 1) {
                    v2.next = start;
                    start.prev = v2;
                } else {
                    v1 = v2;
                }
            }
        } else {
            start.prev = start;
            start.next = start;
        }
    }

    public void removeNode(Node n) throws Exception {
        if (n == start) {
            throw new Exception("Attempt to remove start node of cycle");
        }
        Node prevNode = n.prev;
        Node nextNode = n.next;
        prevNode.next = nextNode;
        nextNode.prev = prevNode;
    }

    public void addNode(Node prevNode, Node nextNode, int val) {
        Node n = new Node(prevNode, val, nextNode);
        prevNode.next = n;
        nextNode.prev = n;
    }
}


