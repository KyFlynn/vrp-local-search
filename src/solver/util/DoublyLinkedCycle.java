package solver.util;

// Doubly linked cycle with pointer to starting node
// When empty, the start node points to itself in both prev and next fields

import java.util.ArrayList;

public class DoublyLinkedCycle {

    public int vehicle;
    public Node depot;

    public DoublyLinkedCycle(int v, Node[] customerNodes, ArrayList<Integer> customers) {
        vehicle = v;
        depot = new Node(null, 0, v, null);
        initCycle(customerNodes, customers);
    }

    public void initCycle(Node[] customerNodes, ArrayList<Integer> customers) {
        if (customers != null) {
            Node v1 = depot;
            for (int i = 0; i < customers.size(); i++) {
                Node v2 = customerNodes[customers.get(i) - 1];
                v2.vehicle = vehicle;
                v2.prev = v1;
                v1.next = v2;
                if (i == customers.size() - 1) {
                    v2.next = depot;
                    depot.prev = v2;
                } else {
                    v1 = v2;
                }
            }
        } else {
            depot.prev = depot;
            depot.next = depot;
        }
    }

    // TODO CHANGE VEHICLE
    public void removeNode(Node n) throws Exception {
        if (n == depot) {
            throw new Exception("Attempt to remove depot from cycle.");
        }
        n.prev.next = n.next;
        n.next.prev = n.prev;
        n.prev = null;
        n.next = null;
        n.vehicle = -1;
    }

    public void addNode(Node n, Node prevNode) {
        prevNode.next.prev = n;
        n.next = prevNode.next;
        prevNode.next = n;
        n.prev = prevNode;
        n.vehicle = prevNode.vehicle;
    }
}


