package solver.util;

// Doubly linked cycle with pointer to starting node
// When empty, the start node points to itself in both prev and next fields

import java.util.ArrayList;

public class DoublyLinkedCycle {

    public int vehicle;
    public Node depot;
    public int nCustomers;

    public DoublyLinkedCycle(int v, Node[] customerNodes, ArrayList<Integer> customers) {
        vehicle = v;
        depot = customerNodes[0];
        nCustomers = customers.size();
        initCycle(customerNodes, customers);
    }

    public void initCycle(Node[] customerNodes, ArrayList<Integer> customers) {
        depot.vehicle = vehicle;
        if (customers != null) {
            Node v1 = depot;
            for (int i = 0; i < customers.size(); i++) {
                Node v2 = customerNodes[customers.get(i)];
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

    public void removeNode(Node n) throws Exception {
        if (n == depot) {
            throw new Exception("Attempt to remove depot from cycle.");
        }
        Node prevNode = n.prev;
        Node nextNode = n.next;
        prevNode.next = nextNode;
        nextNode.prev = prevNode;
        n.customer = -1;
        nCustomers -= 1;
    }

    public void addNode(Node n, Node prevNode, int newVal) {
        prevNode.next.prev = n;
        n.next = prevNode.next;
        prevNode.next = n;
        n.prev = prevNode;
        n.customer = newVal;
        nCustomers += 1;
    }
}


