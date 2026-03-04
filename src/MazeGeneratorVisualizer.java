import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

/**
 * Visualiseur pour l'algorithme "Randomized DFS" de génération de labyrinthe.
 * Il implémente AlgorithmVisualizer pour être animé pas à pas.
 * [CORRIGÉ] Utilise graph.addOrUpdateEdge
 */
public class MazeGeneratorVisualizer implements AlgorithmVisualizer {

    private Graph graph;
    private Node[][] grid;
    private int rows, cols;
    private Random random = new Random();

    private Stack<Node> stack;
    private Set<Node> visited;

    private Map<Node, Edge> predecessorMap; // Stocke le labyrinthe (les arêtes)

    private Node currentNode = null;
    private Edge currentEdge = null;
    private String statusMessage = "";
    private boolean algorithmFinished = false;
    private int logicTimerDelay = 0;

    // Poids par défaut pour chaque "couloir"
    private static final int EDGE_WEIGHT = 1;
    // Taille des cellules (doit correspondre au ComplexGenerator si on veut réutiliser)
    private static final int CELL_SIZE = 70;

    public MazeGeneratorVisualizer(Graph g, int panelWidth, int panelHeight) {
        this.graph = g;

        // 1. Préparation (effacer l'ancien graphe)
        // Le graphe est déjà passé, on ne le vide PAS ici, c'est fait par Main
        // this.graph.clear(); // NE PAS FAIRE ICI
        GraphPanel.resetNodeLetter(); // Réinitialise 'A', 'B', 'C'...

        // 2. Calculer la taille de la grille
        this.cols = (panelWidth - CELL_SIZE) / CELL_SIZE;
        this.rows = (panelHeight - CELL_SIZE) / CELL_SIZE;

        if (this.rows <= 0 || this.cols <= 0) {
            this.statusMessage = "Erreur : Espace insuffisant pour générer.";
            this.algorithmFinished = true;
            // Vider le graphe si la génération échoue au début
            if(this.graph != null) this.graph.clear();
            return;
        }

        // Vider le graphe *après* avoir validé la taille
        if(this.graph != null) this.graph.clear();

        this.grid = new Node[rows][cols];
        this.stack = new Stack<>();
        this.visited = new HashSet<>();
        this.predecessorMap = new HashMap<>();

        // 3. Étape 1 : Créer tous les nœuds de la grille et les ajouter au graphe
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = c * CELL_SIZE + (CELL_SIZE / 2);
                int y = r * CELL_SIZE + (CELL_SIZE / 2);
                Node node = new Node(r + "-" + c, x, y);
                grid[r][c] = node;
                graph.addNode(node); // Ajoute au graphe vide
            }
        }

        // 4. Étape 2 : Initialiser l'algorithme (Randomized DFS)
        Node startNode = grid[0][0];
        stack.push(startNode);
        visited.add(startNode);
        currentNode = startNode;
        this.statusMessage = "Démarrage de la génération du labyrinthe...";
    }

    @Override
    public boolean step() {
        if (algorithmFinished) return false;

        // Vérifie si l'initialisation a échoué (ex: panel trop petit)
        if (grid == null) {
            algorithmFinished = true;
            return false;
        }

        if (stack.isEmpty()) {
            algorithmFinished = true;
            currentNode = null;
            currentEdge = null;
            statusMessage = "Labyrinthe terminé !";
            return false;
        }

        currentNode = stack.peek();

        // Trouver les voisins non visités
        List<Node> unvisitedNeighbors = getUnvisitedNeighbors(currentNode);

        if (!unvisitedNeighbors.isEmpty()) {
            // 1. Choisir un voisin au hasard
            Node neighbor = unvisitedNeighbors.get(random.nextInt(unvisitedNeighbors.size()));

            // 2. "Abattre le mur" (Ajouter une arête)
            // --- CORRECTION ICI ---
            graph.addOrUpdateEdge(currentNode, neighbor, EDGE_WEIGHT);
            // --- FIN CORRECTION ---


            // 3. Mémoriser l'arête pour l'animation et le dessin
            // Trouve l'arête qui vient d'être ajoutée (ou mise à jour)
            currentEdge = graph.findEdge(currentNode, neighbor);
            // Stocke l'arête dans la map (important pour le dessin de l'arbre)
            if(currentEdge != null) { // Vérification
                predecessorMap.put(neighbor, currentEdge);
            }


            // 4. Pousser sur la pile et avancer
            visited.add(neighbor);
            stack.push(neighbor);
            statusMessage = "Creuse de " + currentNode.getId() + " à " + neighbor.getId();
        } else {
            // 5. Plus de voisins ? Revenir en arrière (Backtrack)
            stack.pop();
            currentEdge = null; // Pas d'étincelle
            statusMessage = "Retour arrière depuis " + currentNode.getId();
            // Le currentNode pour la pulsation devient le nouveau sommet de la pile (s'il existe)
            if (!stack.isEmpty()) {
                currentNode = stack.peek();
            } else {
                currentNode = null; // La pile est vide, l'algo va finir
            }
        }

        return true;
    }

    /**
     * Méthode utilitaire pour trouver les voisins valides dans la grille.
     */
    private List<Node> getUnvisitedNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        if (node == null || grid == null) return neighbors; // Safety check

        String[] parts = node.getId().split("-");
        // Vérifie le format de l'ID
        if (parts.length != 2) return neighbors;

        int r, c;
        try {
            r = Integer.parseInt(parts[0]);
            c = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return neighbors; // ID invalide
        }


        // HAUT
        if (r > 0 && !visited.contains(grid[r-1][c])) {
            neighbors.add(grid[r-1][c]);
        }
        // BAS
        if (r < rows - 1 && !visited.contains(grid[r+1][c])) {
            neighbors.add(grid[r+1][c]);
        }
        // GAUCHE
        if (c > 0 && !visited.contains(grid[r][c-1])) {
            neighbors.add(grid[r][c-1]);
        }
        // DROITE
        if (c < cols - 1 && !visited.contains(grid[r][c+1])) {
            neighbors.add(grid[r][c+1]);
        }

        return neighbors;
    }

    // --- Méthodes de l'interface ---

    @Override public boolean isFinished() { return algorithmFinished; }
    @Override public String getStatusMessage() { return statusMessage; }
    @Override public Node getCurrentNode() { return currentNode; } // Pour la pulsation
    @Override public Edge getCurrentEdge() { return currentEdge; } // Pour l'étincelle
    @Override public Set<Node> getVisitedNodes() { return visited; } // Colore le chemin creusé
    @Override public Set<Node> getNodesInQueue() { return new HashSet<>(stack); } // Colore la pile
    @Override public Set<Edge> getSearchTreeEdges() { return new HashSet<>(predecessorMap.values()); } // Dessine l'arbre
    @Override public Set<Edge> getPathEdges() { return new HashSet<>(); } // Pas de "chemin final"
    @Override public Set<Edge> getRejectedEdges() { return new HashSet<>(); } // Ne rejette pas
    @Override public void setLogicTimerDelay(int delay) { this.logicTimerDelay = delay; }
    @Override public int getLogicTimerDelay() { return this.logicTimerDelay; }
}