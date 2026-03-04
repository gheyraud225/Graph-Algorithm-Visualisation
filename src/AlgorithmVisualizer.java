import java.util.Map;
import java.util.Set;

/**
 * Interface pour tous les visualiseurs d'algorithmes de graphe.
 * Définit le "contrat" que Main et GraphPanel utiliseront pour
 * communiquer avec n'importe quel algorithme (Dijkstra, BFS, DFS, etc.)
 */
public interface AlgorithmVisualizer {

    /** Fait avancer l'algorithme d'une seule étape. */
    boolean step();

    /** L'algorithme est-il terminé ? */
    boolean isFinished();

    /** Récupère le message de statut pour le log. */
    String getStatusMessage();

    /** Récupère le nœud en cours de traitement (pour la pulsation). */
    Node getCurrentNode();

    /** Récupère l'arête en cours d'examen (pour l'étincelle). */
    Edge getCurrentEdge();

    /** Récupère les nœuds déjà finalisés (couleur "visité"). */
    Set<Node> getVisitedNodes();

    /** Récupère les nœuds dans la "frontière" (couleur "file d'attente"). */
    Set<Node> getNodesInQueue();

    /** Récupère les arêtes qui font partie de l'arbre de recherche/MST actuel. */
    Set<Edge> getSearchTreeEdges();

    /** Récupère le chemin final une fois l'algo terminé. */
    Set<Edge> getPathEdges();

    /** (NÉCESSAIRE POUR L'ANIMATION) Permet au visualiseur de connaître la vitesse du slider. */
    void setLogicTimerDelay(int delay);

    /** (NÉCESSAIRE POUR L'ANIMATION) Renvoie la vitesse actuelle. */
    int getLogicTimerDelay();
    /** Récupère les arêtes qui ont été examinées mais rejetées. */
    Set<Edge> getRejectedEdges();
}