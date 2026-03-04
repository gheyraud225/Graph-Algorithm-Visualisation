import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList; // (On l'utilise pour le getNodesInQueue)
import java.util.Map;
import java.util.Set;
import java.util.Stack; // NOUVEL IMPORT

/**
 * Visualiseur pour l'algorithme Depth-First Search (DFS).
 * Explore un chemin jusqu'au bout avant de revenir en arrière.
 * (Ignore les poids).
 */
public class DfsVisualizer implements AlgorithmVisualizer {

    private Graph graph;
    private Node endNode;

    // DFS utilise une Pile (Stack)
    private Stack<Node> stack;
    private Set<Node> visited;

    // On utilise un état pour simuler la récursion (tunnel/retour)
    private enum State { EXPLORING, BACKTRACKING }
    private State currentState = State.EXPLORING;
    private Iterator<Edge> neighborIterator = null;

    private Map<Node, Edge> predecessorMap = new HashMap<>();

    private Node currentNode = null;
    private Edge currentEdge = null;
    private String statusMessage = "";
    private Set<Edge> pathEdges = new HashSet<>();
    private boolean algorithmFinished = false;
    private int logicTimerDelay = 0;

    public DfsVisualizer(Graph g, Node startNode, Node endNode) {
        this.graph = g;
        this.endNode = endNode;
        this.stack = new Stack<>();
        this.visited = new HashSet<>();
        this.predecessorMap = new HashMap<>();

        startNode.setDistance(0);
        this.stack.push(startNode); // On "pousse" sur la pile
        this.visited.add(startNode); // On visite dès qu'on pousse
        this.statusMessage = "Initialisation. Nœud '" + startNode.getId() + "' ajouté à la pile.";
    }

    @Override
    public boolean step() {
        if (algorithmFinished) return false;

        // On prend le nœud au sommet de la pile SANS le retirer
        // C'est le nœud qu'on "explore"
        if (stack.isEmpty()) {
            algorithmFinished = true;
            currentNode = null;
            statusMessage = "Algorithme terminé. Pile vide.";
            return false;
        }

        currentNode = stack.peek(); // On regarde le sommet

        // A-t-on trouvé la fin ?
        if (currentNode.equals(endNode)) {
            buildPath();
            algorithmFinished = true;
            statusMessage = "[TERMINÉ] Destination '" + endNode.getId() + "' atteinte !";
            return false;
        }

        // Si on commence l'exploration d'un nœud
        if (currentState == State.EXPLORING) {
            statusMessage = "[EXPLORATION] Nœud '" + currentNode.getId() + "'. Cherche voisins non visités...";
            neighborIterator = graph.getVoisins(currentNode).iterator();
            currentState = State.BACKTRACKING; // On suppose qu'on va revenir (Backtrack)
        }

        // On cherche un voisin où "creuser"
        while (neighborIterator.hasNext()) {
            Edge edge = neighborIterator.next();
            currentEdge = edge;
            Node neighbor = edge.getTarget();
            statusMessage = "[EXAMEN] Voisin '" + neighbor.getId() + "'.";

            // Si le voisin n'a JAMAIS été visité
            if (!visited.contains(neighbor)) {
                visited.add(neighbor); // On le marque
                neighbor.setPredecessor(currentNode);
                neighbor.setDistance(currentNode.getDistance() + 1);
                predecessorMap.put(neighbor, edge);
                stack.push(neighbor); // On "pousse" le voisin (on creuse)

                statusMessage = "[AJOUT] Nœud '" + neighbor.getId() + "' ajouté à la pile. On continue...";
                currentState = State.EXPLORING; // Le prochain step explorera ce nouveau nœud
                return true; // On a trouvé un tunnel, on sort
            }
            // (Si le voisin est déjà visité, on continue la boucle while)
        }

        // Si on arrive ici, c'est qu'on n'a trouvé aucun voisin non visité
        // C'est l'état de BACKTRACKING (Retour arrière)
        statusMessage = "[RETOUR] Tous les voisins de '" + currentNode.getId() + "' visités. On dépile.";
        stack.pop(); // On retire le nœud de la pile
        currentEdge = null; // Pas d'arête active pendant le retour
        currentState = State.EXPLORING; // Le prochain step explorera le nouveau sommet de la pile

        return true;
    }

    // --- Méthodes de l'interface (la plupart sont identiques à Dijkstra/BFS) ---

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
        return new HashSet<>(stack); // Renvoie la pile actuelle
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