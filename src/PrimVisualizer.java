import java.util.*;

/**
 * Visualiseur pour l'algorithme de Prim.
 * [MODIFIÉ] Utilise NodeWrapper pour la PriorityQueue.
 */
public class PrimVisualizer implements AlgorithmVisualizer {

    private Graph graph;
    private Node endNode;
    private Node startNode;// Ignoré

    // --- Wrapper for Priority Queue ---
    private class NodeWrapper implements Comparable<NodeWrapper> {
        Node node;
        double priority; // Coût pour se connecter à l'arbre

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
    private Set<Node> visited; // Nœuds dans l'arbre MST

    private enum State { POLLING, PROCESSING_NEIGHBORS }
    private State currentState = State.POLLING;
    private Iterator<Edge> neighborIterator = null;

    // predecessorMap stocke le MST
    private Map<Node, Edge> predecessorMap = new HashMap<>();

    private Node currentNode = null;
    private Edge currentEdge = null;
    private String statusMessage = "";
    private boolean algorithmFinished = false;
    private int logicTimerDelay = 0;
    private double totalCost = 0;

    public PrimVisualizer(Graph g, Node startNode, Node endNode) {
        this.graph = g;
        this.endNode = endNode; // Ignoré
        this.startNode = startNode;
        this.queue = new PriorityQueue<>(); // Utilise NodeWrapper
        this.visited = new HashSet<>();
        this.predecessorMap = new HashMap<>();

        // Reset implicite fait par Main.java
        // "distance" = coût pour se connecter
        startNode.setDistance(0);

        // --- MODIFIÉ: Ajoute le wrapper ---
        this.queue.add(new NodeWrapper(startNode, 0));
        this.statusMessage = "Initialisation Prim. Nœud '" + startNode.getId() + "' ajouté.";
    }

    @Override
    public boolean step() {
        if (algorithmFinished) return false;

        if (currentState == State.POLLING) {
            currentEdge = null;

            if (queue.isEmpty()) {
                algorithmFinished = true;
                currentNode = null;
                statusMessage = "[TERMINÉ] MST complet. Coût total : " + (int)totalCost;
                return false;
            }

            // --- MODIFIÉ: Extrait le wrapper ---
            NodeWrapper wrapper = queue.poll();
            currentNode = wrapper.node;
            statusMessage = "[EXTRACTION] Nœud '" + currentNode.getId() + "' (coût " + formatCost(wrapper.priority) + ") extrait.";


            if (visited.contains(currentNode)) {
                statusMessage = "[IGNORÉ] Nœud '" + currentNode.getId() + "' déjà dans l'arbre.";
                return true;
            }

            visited.add(currentNode);
            // Ajoute le coût de l'arête qui a ajouté ce nœud
            totalCost += wrapper.priority; // Utilise la priorité stockée dans le wrapper

            // Met à jour predecessorMap pour l'arête qui vient d'être ajoutée à l'arbre
            if (currentNode.getPredecessor() != null) {
                // Trouve l'arête correspondante (peut être redondant si step le fait déjà)
                // Recherche plus robuste:
                Edge edgeToAdd = null;
                for(Edge edge : graph.getVoisins(currentNode.getPredecessor())){
                    if(edge.getTarget().equals(currentNode) && edge.getWeight() == wrapper.priority){
                        edgeToAdd = edge;
                        break;
                    }
                }
                // Ajoute aussi l'inverse si non trouvé (cas où on vient de startNode)
                if (edgeToAdd == null) {
                    for(Edge edge : graph.getVoisins(currentNode)){
                        if(edge.getTarget().equals(currentNode.getPredecessor()) && edge.getWeight() == wrapper.priority){
                            // On stocke l'arête dans le sens Predecessor -> CurrentNode
                            edgeToAdd = graph.findEdge(currentNode.getPredecessor(), currentNode);
                            break;
                        }
                    }
                }

                if (edgeToAdd != null) {
                    predecessorMap.put(currentNode, edgeToAdd);
                } else if (!currentNode.equals(startNode)){ // Si ce n'est pas le start node, on devrait trouver une arête
                    System.err.println("Prim: Could not find edge for node " + currentNode.getId());
                }
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
                    statusMessage = "[EXAMEN] Voisin '" + neighbor.getId() + "' déjà dans l'arbre.";
                    return true;
                }

                // Logique de Prim: priorité = poids de l'arête
                double newCost = edge.getWeight();
                statusMessage = "[EXAMEN] Voisin '" + neighbor.getId() + "'. Coût connexion = " + (int)newCost;

                // getDistance() stocke le "coût minimum actuel pour connecter ce nœud"
                if (newCost < neighbor.getDistance()) {
                    neighbor.setDistance(newCost); // Met à jour le coût minimum
                    neighbor.setPredecessor(currentNode); // Met à jour qui connecte

                    // --- MODIFIÉ: Ajoute le wrapper ---
                    queue.removeIf(nw -> nw.node.equals(neighbor)); // Enlève l'ancienne priorité
                    queue.add(new NodeWrapper(neighbor, newCost)); // Ajoute avec la nouvelle

                    statusMessage = "[MAJ] Nœud '" + neighbor.getId() + "' mis à jour. Nouveau coût : " + (int)newCost;
                } else {
                    statusMessage += ". Pas meilleur que coût=" + formatCost(neighbor.getDistance());
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
    @Override public Set<Node> getVisitedNodes() { return visited; } // Nœuds dans l'arbre

    @Override public Set<Node> getNodesInQueue() {
        // --- MODIFIÉ: Extrait les nœuds des wrappers ---
        Set<Node> nodes = new HashSet<>();
        for (NodeWrapper w : queue) {
            nodes.add(w.node);
        }
        return nodes;
    }

    @Override public Set<Edge> getSearchTreeEdges() { return new HashSet<>(predecessorMap.values()); }
    @Override public Set<Edge> getPathEdges() { return new HashSet<>(); } // Pas de chemin final
    @Override public Set<Edge> getRejectedEdges() { return new HashSet<>(); }
    @Override public void setLogicTimerDelay(int delay) { this.logicTimerDelay = delay; }
    @Override public int getLogicTimerDelay() { return this.logicTimerDelay; }

    private String formatCost(double d) {
        if (d == Double.POSITIVE_INFINITY) return "∞";
        return String.format("%.0f", d);
    }
}