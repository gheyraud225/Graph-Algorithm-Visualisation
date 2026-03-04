import java.util.*; // Import necessary classes like List, Map, Set, ArrayList, HashMap, Iterator

/**
 * Represents a graph, which can be directed or undirected.
 * Stores nodes and edges (using adjacency list internally).
 */
public class Graph {

    private final boolean isDirected; // Flag to determine graph type
    private List<Node> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private Map<Node, List<Edge>> adjacence = new HashMap<>(); // Stores OUTGOING edges

    /**
     * Constructor.
     * @param isDirected true for a directed graph, false for undirected.
     */
    public Graph(boolean isDirected) {
        this.isDirected = isDirected;
    }

    /**
     * Returns true if the graph is directed, false otherwise.
     */
    public boolean isDirected() {
        return isDirected;
    }

    /**
     * Adds a node to the graph if it doesn't already exist.
     * Initializes its entry in the adjacency list.
     */
    public void addNode(Node node) {
        if (!adjacence.containsKey(node)) {
            nodes.add(node);
            adjacence.put(node, new ArrayList<>());
        }
    }

    /**
     * Adds a directed edge from source to target with the given weight.
     * Ensures both nodes exist in the graph first.
     */
    public void addEdge(Node source, Node target, int weight) {
        // Ensure nodes are added first
        addNode(source);
        addNode(target);

        Edge newEdge = new Edge(source, target, weight);
        edges.add(newEdge);
        adjacence.get(source).add(newEdge); // Add to outgoing list of source
    }

    /**
     * Adds or updates an edge based on the graph type (directed/undirected).
     * For undirected, it ensures edges exist in both directions.
     */
    public void addOrUpdateEdge(Node n1, Node n2, int weight) {
        // --- Edge n1 -> n2 ---
        Edge edge1 = findEdge(n1, n2);
        if (edge1 != null) {
            edge1.setWeight(weight); // Update existing
        } else {
            addEdge(n1, n2, weight); // Create new directed edge
        }

        // --- If UNDIRECTED, add/update n2 -> n1 as well ---
        if (!isDirected) {
            Edge edge2 = findEdge(n2, n1);
            if (edge2 != null) {
                edge2.setWeight(weight); // Update reverse
            } else {
                addEdge(n2, n1, weight); // Create reverse directed edge
            }
        }
    }

    /**
     * Finds a specific directed edge from source to target.
     * @return The Edge if found, otherwise null.
     */
    public Edge findEdge(Node source, Node target) {
        if (!adjacence.containsKey(source)) {
            return null;
        }
        for (Edge edge : adjacence.get(source)) {
            if (edge.getTarget().equals(target)) {
                return edge;
            }
        }
        return null;
    }

    /**
     * Removes a node and all incident edges (incoming and outgoing).
     */
    public void removeNode(Node nodeToRemove) {
        if (!nodes.contains(nodeToRemove)) return;

        // 1. Remove outgoing edges from the adjacency list and main edges list
        List<Edge> outgoing = adjacence.remove(nodeToRemove);
        if (outgoing != null) {
            edges.removeAll(outgoing);
        }

        // 2. Remove incoming edges from adjacency lists of OTHERS and main edges list
        Iterator<Edge> edgeIterator = edges.iterator();
        while (edgeIterator.hasNext()) {
            Edge edge = edgeIterator.next();
            if (edge.getTarget().equals(nodeToRemove)) {
                Node source = edge.getSource();
                if (adjacence.containsKey(source)) {
                    adjacence.get(source).remove(edge);
                }
                edgeIterator.remove(); // Remove from main edges list
            }
        }

        // 3. Remove the node itself
        nodes.remove(nodeToRemove);
    }


    /**
     * [PRIVATE] Removes a single directed edge object.
     */
    private void removeDirectedEdge(Edge edgeToRemove) {
        if (edgeToRemove == null) return;
        edges.remove(edgeToRemove);
        if (adjacence.containsKey(edgeToRemove.getSource())) {
            adjacence.get(edgeToRemove.getSource()).remove(edgeToRemove);
        }
    }

    /**
     * Removes an edge based on graph type.
     * For undirected, removes edges in both directions.
     */
    public void removeEdge(Edge edge) {
        if (edge == null) return;

        removeDirectedEdge(edge); // Remove the primary edge

        if (!isDirected) {
            // If undirected, also find and remove the reverse edge
            Edge reverseEdge = findEdge(edge.getTarget(), edge.getSource());
            removeDirectedEdge(reverseEdge);
        }
    }
    // Keep removeUndirectedEdge for compatibility? Or rename removeEdge?
    // Let's keep it simple: removeEdge handles both based on the flag.
    public void removeUndirectedEdge(Edge edge){ removeEdge(edge); }


    /**
     * Gets the list of all nodes.
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * Gets the list of all edges.
     */
    public List<Edge> getEdges() {
        return edges;
    }

    /**
     * Gets the outgoing edges from a specific node.
     */
    public List<Edge> getVoisins(Node node) { // Renamed from getNeighbors maybe? Keep Voisins
        return adjacence.getOrDefault(node, Collections.emptyList());
    }

    /**
     * NEW: Gets the INCOMING edges to a specific node. Useful for Hierarchical layout.
     * Note: This is less efficient as it iterates through all edges.
     */
    public List<Edge> getPredecesseursEdges(Node node) {
        List<Edge> incoming = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.getTarget().equals(node)) {
                incoming.add(edge);
            }
        }
        return incoming;
    }

    /**
     * NEW: Gets the predecessor NODES for a specific node.
     */
    public List<Node> getPredecesseursNodes(Node node) {
        List<Node> predecessors = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.getTarget().equals(node)) {
                predecessors.add(edge.getSource());
            }
        }
        return predecessors;
    }


    /**
     * Resets algorithm-specific data (like distance/cost, predecessor) for all nodes.
     */
    public void resetNodes() {
        for (Node node : nodes) {
            node.reset();
        }
    }

    /**
     * Clears the entire graph.
     */
    public void clear() {
        nodes.clear();
        edges.clear();
        adjacence.clear();
    }

    /**
     * Finds a node by its ID. Used by loading, combat frame etc.
     */
    public Node findNodeById(String id) {
        for (Node node : nodes) {
            if (node.getId().equals(id)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Creates a deep copy of this graph.
     */
    public Graph deepCopy() {
        Graph newGraph = new Graph(this.isDirected); // Copy the type
        Map<String, Node> newNodeMap = new HashMap<>();

        for (Node oldNode : this.nodes) {
            Node newNode = new Node(oldNode.getId(), oldNode.getX(), oldNode.getY());
            newGraph.addNode(newNode);
            newNodeMap.put(newNode.getId(), newNode);
        }

        for (Edge oldEdge : this.edges) {
            Node newSource = newNodeMap.get(oldEdge.getSource().getId());
            Node newTarget = newNodeMap.get(oldEdge.getTarget().getId());
            int weight = oldEdge.getWeight();
            if (newSource != null && newTarget != null) {
                // Use the basic addEdge which respects the directed flag now
                newGraph.addEdge(newSource, newTarget, weight);
            }
        }
        return newGraph;
    }

} // End Graph class