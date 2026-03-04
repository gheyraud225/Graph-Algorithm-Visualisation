import java.util.*;

/**
 * Visualiseur pour l'algorithme A* (A-Star).
 * Utilise une heuristique (distance euclidienne) pour guider la recherche.
 */
public class AStarVisualizer implements AlgorithmVisualizer {

    private Graph graph;
    private Node startNode; // Garde une référence pour recalculer fCost si besoin
    private Node endNode;

    // A* utilise une PriorityQueue basée sur fCost (gCost + hCost)
    private PriorityQueue<Node> openSet; // La "frontière"
    private Set<Node> closedSet; // Les nœuds déjà traités

    // predecessorMap stocke le chemin trouvé
    private Map<Node, Edge> predecessorMap = new HashMap<>();

    private Node currentNode = null;
    private Edge currentEdge = null;
    private String statusMessage = "";
    private boolean algorithmFinished = false;
    private int logicTimerDelay = 0;
    private Set<Edge> pathEdges = new HashSet<>();

    public AStarVisualizer(Graph g, Node startNode, Node endNode) {
        this.graph = g;
        this.startNode = startNode;
        this.endNode = endNode;
        this.openSet = new PriorityQueue<>(); // Node compare par fCost maintenant
        this.closedSet = new HashSet<>();

        // Initialisation du nœud de départ
        startNode.setGCost(0);
        startNode.setHCost(startNode.calculateHeuristic(endNode));
        startNode.setFCost(startNode.getGCost() + startNode.getHCost());

        this.openSet.add(startNode);
        this.statusMessage = "Initialisation A*. Nœud '" + startNode.getId() + "' ajouté (f=" + formatCost(startNode.getFCost()) + ")";
    }

    @Override
    public boolean step() {
        if (algorithmFinished) return false;
        currentEdge = null;

        if (openSet.isEmpty()) {
            algorithmFinished = true;
            currentNode = null;
            statusMessage = "[TERMINÉ] Open Set vide. Destination non atteinte.";
            return false;
        }

        // 1. Extraire le nœud avec le plus petit fCost
        currentNode = openSet.poll();
        statusMessage = "[EXTRACTION] Nœud '" + currentNode.getId() + "' (f=" + formatCost(currentNode.getFCost()) + ", g=" + formatCost(currentNode.getGCost()) + ") extrait.";

        // 2. Vérifie si c'est la destination
        if (currentNode.equals(endNode)) {
            buildPath();
            algorithmFinished = true;
            statusMessage = "[TERMINÉ] Destination '" + endNode.getId() + "' atteinte ! Coût G: " + formatCost(endNode.getGCost());
            return false;
        }

        // 3. Le déplacer vers le Closed Set
        closedSet.add(currentNode);

        // 4. Examiner les voisins
        for (Edge edge : graph.getVoisins(currentNode)) {
            Node neighbor = edge.getTarget();
            currentEdge = edge; // Pour l'étincelle

            // Ignore les voisins déjà traités
            if (closedSet.contains(neighbor)) {
                statusMessage = "[EXAMEN] Voisin '" + neighbor.getId() + "' déjà traité.";
                continue; // On continue avec le voisin suivant (important !)
            }

            // 5. Calculer le nouveau gCost (coût depuis le départ via currentNode)
            double tentativeGCost = currentNode.getGCost() + edge.getWeight();
            statusMessage = "[EXAMEN] Voisin '" + neighbor.getId() + "'. Coût G via '" + currentNode.getId() + "' = " + formatCost(tentativeGCost);


            boolean isNewPathBetter = false;
            // Si le voisin n'est pas encore dans l'Open Set, c'est un nouveau chemin
            if (!openSet.contains(neighbor)) {
                isNewPathBetter = true;
                openSet.add(neighbor); // Ajoute à la frontière
                statusMessage = "[AJOUT] Nœud '" + neighbor.getId() + "' ajouté à l'Open Set.";
            } else if (tentativeGCost < neighbor.getGCost()) {
                // S'il est déjà dans l'Open Set, vérifie si ce chemin est meilleur
                isNewPathBetter = true;
                statusMessage += ". Meilleur chemin trouvé !";
            } else {
                statusMessage += ". Pas meilleur que g=" + formatCost(neighbor.getGCost());
            }

            // 6. Mettre à jour si c'est un meilleur chemin
            if (isNewPathBetter) {
                neighbor.setPredecessor(currentNode);
                neighbor.setGCost(tentativeGCost);
                neighbor.setHCost(neighbor.calculateHeuristic(endNode)); // Calcule l'heuristique
                neighbor.setFCost(neighbor.getGCost() + neighbor.getHCost()); // f = g + h

                predecessorMap.put(neighbor, edge); // Met à jour l'arbre

                // Important : Si on met à jour un nœud déjà dans la PriorityQueue,
                // il faut la forcer à se réorganiser. Le plus simple est remove/add.
                openSet.remove(neighbor); // Enlève l'ancienne version
                openSet.add(neighbor);    // Ajoute la nouvelle avec la bonne priorité fCost

                statusMessage = "[MAJ] Nœud '" + neighbor.getId() + "' mis à jour. g=" + formatCost(neighbor.getGCost()) + ", h=" + formatCost(neighbor.getHCost()) + ", f=" + formatCost(neighbor.getFCost());
            }
            // Important: A* doit examiner TOUS les voisins avant de retourner.
            // Ne pas mettre de 'return true;' dans cette boucle for.
        }
        // Après avoir traité tous les voisins
        statusMessage = "Tous les voisins de '" + currentNode.getId() + "' traités.";
        return true; // Continue l'algorithme
    }

    // --- Méthodes de l'interface ---
    @Override public boolean isFinished() { return algorithmFinished; }
    @Override public String getStatusMessage() { return statusMessage; }
    @Override public Node getCurrentNode() { return currentNode; }
    @Override public Edge getCurrentEdge() { return currentEdge; }
    @Override public Set<Node> getVisitedNodes() { return closedSet; } // "Visited" = Closed Set
    @Override public Set<Node> getNodesInQueue() { return new HashSet<>(openSet); } // "Queue" = Open Set
    @Override public Set<Edge> getSearchTreeEdges() { return new HashSet<>(predecessorMap.values()); }
    @Override public Set<Edge> getPathEdges() { return pathEdges; }
    @Override public Set<Edge> getRejectedEdges() { return new HashSet<>(); } // A* ne rejette pas
    @Override public void setLogicTimerDelay(int delay) { this.logicTimerDelay = delay; }
    @Override public int getLogicTimerDelay() { return this.logicTimerDelay; }

    // (buildPath est identique aux autres)
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
    // Formatteur pour les coûts
    private String formatCost(double d) {
        if (d == Double.POSITIVE_INFINITY) return "∞";
        return String.format("%.1f", d); // Affiche une décimale pour f/h
    }
}