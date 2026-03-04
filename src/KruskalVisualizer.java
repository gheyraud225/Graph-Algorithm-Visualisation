import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Visualiseur pour l'algorithme de Kruskal.
 * Trouve un arbre couvrant minimal (MST) en triant toutes les arêtes
 * et en utilisant une structure Union-Find pour détecter les cycles.
 * [CORRIGÉ] Utilise un Set pour stocker les arêtes du MST.
 */
public class KruskalVisualizer implements AlgorithmVisualizer {

    private Graph graph;
    private Iterator<Edge> sortedEdgeIterator;
    private UnionFind uf;
    private Map<Node, Integer> nodeToIndexMap;

    // --- Utilisation d'un Set pour le MST ---
    private Set<Edge> mstEdges = new HashSet<>();
    private Set<Edge> rejectedEdges = new HashSet<>();

    private Edge currentEdge = null;
    private String statusMessage = "";
    private boolean algorithmFinished = false;
    private int logicTimerDelay = 0;
    private double totalCost = 0;
    private int edgesAdded = 0;
    private int totalNodes;

    /**
     * Structure de données interne pour Union-Find (Disjoint Set Union).
     */
    private class UnionFind {
        private int[] parent;
        private int[] rank;

        public UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i; // Chaque nœud est son propre parent
                rank[i] = 0;
            }
        }

        // Trouve le représentant (racine) du set
        public int find(int i) {
            if (parent[i] == i)
                return i;
            // Compression de chemin
            return parent[i] = find(parent[i]);
        }

        // Fait l'union de two sets. Renvoie false s'ils sont déjà unis.
        public boolean union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);

            if (rootI != rootJ) {
                // Union par rang
                if (rank[rootI] < rank[rootJ]) {
                    parent[rootI] = rootJ;
                } else if (rank[rootI] > rank[rootJ]) {
                    parent[rootJ] = rootI;
                } else {
                    parent[rootJ] = rootI;
                    rank[rootI]++;
                }
                return true; // Une fusion a eu lieu
            }
            return false; // Déjà dans le même set
        }
    } // Fin de la classe interne UnionFind

    public KruskalVisualizer(Graph g) {
        this.graph = g;
        this.totalNodes = g.getNodes().size();

        // 1. Initialiser Union-Find
        this.nodeToIndexMap = new HashMap<>();
        int i = 0;
        for (Node node : g.getNodes()) {
            nodeToIndexMap.put(node, i++);
        }
        this.uf = new UnionFind(totalNodes);

        // 2. Créer une liste d'arêtes unique et triée
        List<Edge> uniqueEdges = new ArrayList<>();
        Set<String> processedEdges = new HashSet<>();
        for (Edge edge : g.getEdges()) {
            String id1 = edge.getSource().getId();
            String id2 = edge.getTarget().getId();
            String edgeId = id1.compareTo(id2) < 0 ? id1 + "-" + id2 : id2 + "-" + id1;

            if (!processedEdges.contains(edgeId)) {
                uniqueEdges.add(edge);
                processedEdges.add(edgeId);
            }
        }

        // 3. Trier la liste par poids
        uniqueEdges.sort(Comparator.comparingInt(Edge::getWeight));

        // 4. Créer l'itérateur que step() utilisera
        this.sortedEdgeIterator = uniqueEdges.iterator();

        this.statusMessage = "Initialisation. " + uniqueEdges.size() + " arêtes triées par poids.";
    }

    @Override
    public boolean step() {
        if (algorithmFinished) return false;

        if (!sortedEdgeIterator.hasNext()) {
            algorithmFinished = true;
            statusMessage = "[TERMINÉ] Plus d'arêtes à examiner. Coût total du MST : " + (int)totalCost;
            return false;
        }

        // 1. Prendre la prochaine arête la moins chère
        currentEdge = sortedEdgeIterator.next();
        Node u = currentEdge.getSource();
        Node v = currentEdge.getTarget();

        // Vérification de sécurité (si les nœuds n'existent plus)
        if (!nodeToIndexMap.containsKey(u) || !nodeToIndexMap.containsKey(v)) {
            statusMessage = "[IGNORÉE] Arête " + u.getId() + "-" + v.getId() + " (nœud supprimé).";
            return true; // Passe à l'arête suivante
        }

        int indexU = nodeToIndexMap.get(u);
        int indexV = nodeToIndexMap.get(v);

        statusMessage = "[EXAMEN] Arête " + u.getId() + "-" + v.getId() + " (poids " + currentEdge.getWeight() + ")";

        // 2. Vérifier si l'ajout crée un cycle (en utilisant Union-Find)
        if (uf.union(indexU, indexV)) {
            // 3a. SUCCÈS : Pas de cycle. On ajoute l'arête au MST.
            mstEdges.add(currentEdge); // Ajoute au Set
            totalCost += currentEdge.getWeight();
            edgesAdded++;
            statusMessage = "[ACCEPTÉE] Arête " + u.getId() + "-" + v.getId() + ".";
        } else {
            // 3b. ÉCHEC : L'arête crée un cycle.
            rejectedEdges.add(currentEdge);
            statusMessage = "[REJETÉE] Arête " + u.getId() + "-" + v.getId() + " (crée un cycle).";
        }

        // 4. Condition d'arrêt (MST complet ou graphe non connexe)
        // Pour un graphe potentiellement non connexe, on ne s'arrête que quand toutes les arêtes sont traitées.
        // if (edgesAdded == totalNodes - 1) {
        //     algorithmFinished = true;
        //     statusMessage = "[TERMINÉ] MST complet (" + edgesAdded + " arêtes). Coût total : " + (int)totalCost;
        // }
        // On laisse l'itérateur aller au bout

        return true;
    }

    // --- Méthodes de l'interface ---

    @Override
    public Set<Edge> getSearchTreeEdges() { return mstEdges; } // Renvoie le Set

    @Override
    public Set<Edge> getRejectedEdges() { return rejectedEdges; }

    @Override
    public Set<Edge> getPathEdges() { return new HashSet<>(); } // Pas de chemin final

    @Override
    public boolean isFinished() { return algorithmFinished; }

    @Override
    public String getStatusMessage() { return statusMessage; }

    @Override
    public Node getCurrentNode() { return null; } // Kruskal n'a pas de "nœud actuel"

    @Override
    public Edge getCurrentEdge() { return currentEdge; }

    @Override
    public Set<Node> getVisitedNodes() { return new HashSet<>(); } // Pas de "visités"

    @Override
    public Set<Node> getNodesInQueue() { return new HashSet<>(); } // Pas de "file"

    @Override
    public void setLogicTimerDelay(int delay) { this.logicTimerDelay = delay; }

    @Override
    public int getLogicTimerDelay() { return this.logicTimerDelay; }
}