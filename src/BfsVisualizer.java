import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Visualiseur pour l'algorithme Breadth-First Search (BFS).
 * Il trouve le chemin le plus court en nombre d'arêtes (ignore les poids).
 */
public class BfsVisualizer implements AlgorithmVisualizer {

    private Graph graph;
    private Node endNode;

    // BFS utilise une Queue simple (File)
    private Queue<Node> queue;
    private Set<Node> visited;

    private enum State { POLLING, PROCESSING_NEIGHBORS }
    private State currentState = State.POLLING;
    private Iterator<Edge> neighborIterator = null;

    private Map<Node, Edge> predecessorMap = new HashMap<>();

    private Node currentNode = null;
    private Edge currentEdge = null;
    private String statusMessage = "";
    private Set<Edge> pathEdges = new HashSet<>();
    private boolean algorithmFinished = false;
    private int logicTimerDelay = 0;

    public BfsVisualizer(Graph g, Node startNode, Node endNode) {
        this.graph = g;
        this.endNode = endNode;
        this.queue = new LinkedList<>(); // Utilise LinkedList comme une Queue
        this.visited = new HashSet<>();
        this.predecessorMap = new HashMap<>();

        // Pour BFS, la "distance" est le nombre d'étapes (niveau)
        startNode.setDistance(0);
        this.queue.add(startNode);
        this.visited.add(startNode); // Pour BFS, on marque comme visité dès qu'on l'ajoute
        this.statusMessage = "Initialisation. Nœud '" + startNode.getId() + "' ajouté à la file.";
    }

    @Override
    public boolean step() {
        if (algorithmFinished) return false;

        if (currentState == State.POLLING) {
            currentEdge = null;

            if (queue.isEmpty()) {
                algorithmFinished = true;
                currentNode = null;
                statusMessage = "Algorithme terminé. File vide, destination non atteinte.";
                return false;
            }

            // poll() = défiler
            currentNode = queue.poll();
            statusMessage = "[EXTRACTION] Nœud '" + currentNode.getId() + "' (niveau " + (int)currentNode.getDistance() + ") extrait.";

            // A-t-on trouvé la fin ?
            if (currentNode.equals(endNode)) {
                buildPath();
                algorithmFinished = true;
                statusMessage = "[TERMINÉ] Destination '" + endNode.getId() + "' atteinte ! Étapes : " + (int)endNode.getDistance();
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
                statusMessage = "[EXAMEN] Voisin '" + neighbor.getId() + "'.";

                // Si le voisin n'a JAMAIS été visité
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor); // On le marque
                    neighbor.setPredecessor(currentNode);
                    // La "distance" est le niveau du parent + 1
                    neighbor.setDistance(currentNode.getDistance() + 1);
                    predecessorMap.put(neighbor, edge);
                    queue.add(neighbor); // On l'ajoute à la file

                    statusMessage = "[AJOUT] Nœud '" + neighbor.getId() + "' ajouté à la file (niveau " + (int)neighbor.getDistance() + ").";
                } else {
                    statusMessage = "[IGNORÉ] Voisin '" + neighbor.getId() + "' déjà visité.";
                }

                return true;

            } else {
                statusMessage = "Tous les voisins de '" + currentNode.getId() + "' ont été traités.";
                currentState = State.POLLING;
                currentNode = null;
                currentEdge = null;
                return true;
            }
        }
        return false;
    }

    // --- Méthodes de l'interface (la plupart sont identiques à Dijkstra) ---

    @Override
    public boolean isFinished() { return algorithmFinished; }

    @Override
    public String getStatusMessage() { return statusMessage; }

    @Override
    public Node getCurrentNode() { return currentNode; }

    @Override
    public Edge getCurrentEdge() { return currentEdge; }

    @Override
    public Set<Node> getVisitedNodes() { return visited; }

    @Override
    public Set<Node> getNodesInQueue() {
        return new HashSet<>(queue); // Renvoie la file actuelle
    }

    @Override
    public Set<Edge> getSearchTreeEdges() {
        // Renvoie simplement les valeurs de la map existante
        return new HashSet<>(predecessorMap.values());
    }
    @Override
    public Set<Edge> getPathEdges() { return pathEdges; }

    @Override
    public void setLogicTimerDelay(int delay) { this.logicTimerDelay = delay; }

    @Override
    public int getLogicTimerDelay() { return this.logicTimerDelay; }

    private void buildPath() {
        pathEdges.clear();
        Node step = endNode;
        while (step.getPredecessor() != null) {
            Edge edge = predecessorMap.get(step);
            if (edge != null) {
                pathEdges.add(edge);
                step = edge.getSource();
            } else {
                break;
            }
        }
    }
    @Override
    public Set<Edge> getRejectedEdges() {
        // Cet algorithme ne rejette pas d'arêtes de cette manière
        return new HashSet<>();
    }
}