import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class Dijkstra {

    public static void run(Graph graph, Node source) {
        // 1. Réinitialiser le graphe
        graph.resetNodes();
        source.setDistance(0);

        // 2. Créer la file de priorité et le set des visités
        PriorityQueue<Node> queue = new PriorityQueue<>();
        Set<Node> visited = new HashSet<>();

        // 3. Ajouter la source
        queue.add(source);

        while (!queue.isEmpty()) {
            // 4. Extraire le nœud avec la plus petite distance
            Node u = queue.poll();

            if (visited.contains(u)) {
                continue; // Déjà traité
            }
            visited.add(u);

            // 5. Parcourir tous les voisins (v) du nœud (u)
            List<Edge> voisins = graph.getVoisins(u);
            for (Edge edge : voisins) {
                Node v = edge.getTarget();

                // On ne traite pas les nœuds déjà finalisés
                if (visited.contains(v)) {
                    continue;
                }

                // 6. "Relaxation" de l'arête
                double newDist = u.getDistance() + edge.getWeight();

                if (newDist < v.getDistance()) {
                    // Mise à jour de la distance et du prédécesseur
                    v.setDistance(newDist);
                    v.setPredecessor(u);

                    // (Ré)-insérer dans la file avec la nouvelle priorité
                    // On le ré-ajoute même s'il est déjà présent, 
                    // car sa priorité (distance) a changé.
                    queue.add(v);
                }
            }
        }
    }
}