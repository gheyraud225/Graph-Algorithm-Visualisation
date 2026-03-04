import java.util.*;

/**
 * Visualiseur pour l'algorithme de Dijkstra.
 * [MODIFIÉ] Utilise NodeWrapper pour la PriorityQueue.
 */
public class DijkstraVisualizer implements AlgorithmVisualizer {

    private Graph graph;
    private Node endNode;

    // --- Wrapper for Priority Queue ---
    private class NodeWrapper implements Comparable<NodeWrapper> {
        Node node;
        double priority; // This will be the gCost (distance)

        NodeWrapper(Node node, double priority) {
            this.node = node;
            this.priority = priority;
        }

        @Override
        public int compareTo(NodeWrapper other) {
            return Double.compare(this.priority, other.priority);
        }
    }
    // ---

    // --- MODIFIÉ: Utilise NodeWrapper ---
    private PriorityQueue<NodeWrapper> queue;
    private Set<Node> visited; // Closed set

    private enum State { POLLING, PROCESSING_NEIGHBORS }
    private State currentState = State.POLLING;
    private Iterator<Edge> neighborIterator = null;

    private Map<Node, Edge> predecessorMap = new HashMap<>();

    private Node currentNode = null;
    private Edge currentEdge = null;
    private String statusMessage = "";
    private boolean algorithmFinished = false;
    private int logicTimerDelay = 0;
    private Set<Edge> pathEdges = new HashSet<>();

    public DijkstraVisualizer(Graph g, Node startNode, Node endNode) {
        this.graph = g;
        this.endNode = endNode;
        this.queue = new PriorityQueue<>(); // Utilise NodeWrapper
        this.visited = new HashSet<>();
        this.predecessorMap = new HashMap<>();

        // Reset implicite fait par Main.java avant l'instanciation
        startNode.setGCost(0); // Utilise gCost

        // --- MODIFIÉ: Ajoute le wrapper ---
        this.queue.add(new NodeWrapper(startNode, 0));
        this.statusMessage = "Initialisation Dijkstra. Nœud '" + startNode.getId() + "' ajouté.";
    }

    @Override
    public boolean step() {
        if (algorithmFinished) return false;

        if (currentState == State.POLLING) {
            currentEdge = null;

            if (queue.isEmpty()) {
                algorithmFinished = true;
                currentNode = null;
                statusMessage = "[TERMINÉ] File vide. Destination non atteinte.";
                return false;
            }

            // --- MODIFIÉ: Extrait le wrapper ---
            NodeWrapper wrapper = queue.poll();
            currentNode = wrapper.node;
            statusMessage = "[EXTRACTION] Nœud '" + currentNode.getId() + "' (g=" + formatCost(currentNode.getGCost()) + ") extrait.";


            if (visited.contains(currentNode)) {
                statusMessage = "[IGNORÉ] Nœud '" + currentNode.getId() + "' déjà visité.";
                return true;
            }

            visited.add(currentNode);

            if (currentNode.equals(endNode)) {
                buildPath();
                algorithmFinished = true;
                statusMessage = "[TERMINÉ] Destination '" + endNode.getId() + "' atteinte ! Coût G: " + formatCost(endNode.getGCost());
                return false;
            }

            neighborIterator = graph.getVoisins(currentNode).iterator();
            currentState = State.PROCESSING_NEIGHBORS;
        }

        if (currentState == State.PROCESSING_NEIGHBORS) {
            if (neighborIterator.hasNext()) {
                Edge edge = neighborIterator.next();
                currentEdge = edge;
                Node neighbor = edge.getTarget();

                if (visited.contains(neighbor)) {
                    statusMessage = "[EXAMEN] Voisin '" + neighbor.getId() + "' déjà visité.";
                    return true; // Passe au voisin suivant
                }

                double tentativeGCost = currentNode.getGCost() + edge.getWeight();
                statusMessage = "[EXAMEN] Voisin '" + neighbor.getId() + "'. Coût G via '" + currentNode.getId() + "' = " + formatCost(tentativeGCost);

                if (tentativeGCost < neighbor.getGCost()) {
                    neighbor.setPredecessor(currentNode);
                    neighbor.setGCost(tentativeGCost);
                    predecessorMap.put(neighbor, edge);

                    // --- MODIFIÉ: Ajoute le wrapper ---
                    // Enlève l'ancienne version si elle existe (important pour PriorityQueue)
                    queue.removeIf(nw -> nw.node.equals(neighbor));
                    queue.add(new NodeWrapper(neighbor, neighbor.getGCost()));

                    statusMessage = "[MAJ] Nœud '" + neighbor.getId() + "' mis à jour. Nouveau g=" + formatCost(neighbor.getGCost());
                } else {
                    statusMessage += ". Pas meilleur que g=" + formatCost(neighbor.getGCost());
                }

                return true; // Traite un voisin par step

            } else {
                statusMessage = "Tous les voisins de '" + currentNode.getId() + "' traités.";
                currentState = State.POLLING;
                currentNode = null;
                currentEdge = null;
                return true;
            }
        }
        return false;
    }

    // --- Méthodes de l'interface ---
    @Override public boolean isFinished() { return algorithmFinished; }
    @Override public String getStatusMessage() { return statusMessage; }
    @Override public Node getCurrentNode() { return currentNode; }
    @Override public Edge getCurrentEdge() { return currentEdge; }
    @Override public Set<Node> getVisitedNodes() { return visited; }

    @Override public Set<Node> getNodesInQueue() {
        // --- MODIFIÉ: Extrait les nœuds des wrappers ---
        Set<Node> nodes = new HashSet<>();
        for (NodeWrapper w : queue) {
            nodes.add(w.node);
        }
        return nodes;
    }

    @Override public Set<Edge> getSearchTreeEdges() { return new HashSet<>(predecessorMap.values()); }
    @Override public Set<Edge> getPathEdges() { return pathEdges; }
    @Override public Set<Edge> getRejectedEdges() { return new HashSet<>(); }
    @Override public void setLogicTimerDelay(int delay) { this.logicTimerDelay = delay; }
    @Override public int getLogicTimerDelay() { return this.logicTimerDelay; }

    private void buildPath() {
        pathEdges.clear();
        Node step = endNode;
        while (step != null && step.getPredecessor() != null) {
            Edge edge = predecessorMap.get(step);
            if (edge != null) {
                pathEdges.add(edge);
                step = edge.getSource();
            } else { break; }
        }
    }
    private String formatCost(double d) {
        if (d == Double.POSITIVE_INFINITY) return "∞";
        // Dijkstra n'a pas besoin de décimales en général
        return String.format("%.0f", d);
    }
}