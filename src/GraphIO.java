import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Gère la sauvegarde et le chargement du graphe dans un fichier.
 */
public class GraphIO {

    /**
     * Ouvre une boîte de dialogue pour sauvegarder le graphe actuel dans un fichier.
     */
    public static void saveGraph(Graph graph, GraphPanel panel) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Sauvegarder le graphe");
        fc.setFileFilter(new FileNameExtensionFilter("Fichier Graphe (*.txt)", "txt"));

        int userSelection = fc.showSaveDialog(panel);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fc.getSelectedFile();
            // S'assurer que le fichier a l'extension .txt
            if (!fileToSave.getAbsolutePath().endsWith(".txt")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".txt");
            }

            try (PrintWriter out = new PrintWriter(fileToSave)) {

                // 1. Écrire les Nœuds
                out.println("[NODES]");
                for (Node node : graph.getNodes()) {
                    out.println(node.getId() + "," + node.getX() + "," + node.getY());
                }

                // 2. Écrire les Arêtes (une seule direction suffit pour un graphe non-orienté)
                out.println("[EDGES]");
                for (Edge edge : graph.getEdges()) {
                    // Pour éviter les doublons (A->B et B->A), on ne sauvegarde
                    // que si l'ID de la source est "plus petit" que celui de la cible.
                    if (edge.getSource().getId().compareTo(edge.getTarget().getId()) < 0) {
                        out.println(edge.getSource().getId() + "," + edge.getTarget().getId() + "," + edge.getWeight());
                    }
                }

                // 3. Écrire la prochaine lettre de nœud
                out.println("[NEXT_LETTER]");
                out.println(GraphPanel.getNextNodeLetter());

                System.out.println("Graphe sauvegardé dans : " + fileToSave.getAbsolutePath());

            } catch (Exception e) {
                JOptionPane.showMessageDialog(panel, "Erreur lors de la sauvegarde du graphe : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Ouvre une boîte de dialogue pour charger un graphe depuis un fichier.
     */
    public static void loadGraph(Graph graph, GraphPanel panel) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Charger un graphe");
        fc.setFileFilter(new FileNameExtensionFilter("Fichier Graphe (*.txt)", "txt"));

        int userSelection = fc.showOpenDialog(panel);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fc.getSelectedFile();
            // On crée une map temporaire pour retrouver les nœuds par leur ID
            Map<String, Node> nodeMap = new HashMap<>();

            // On efface le graphe actuel
            graph.clear();

            try (BufferedReader br = new BufferedReader(new FileReader(fileToLoad))) {
                String line;
                String section = "";

                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (line.equals("[NODES]")) { section = "NODES"; continue; }
                    if (line.equals("[EDGES]")) { section = "EDGES"; continue; }
                    if (line.equals("[NEXT_LETTER]")) { section = "NEXT_LETTER"; continue; }

                    String[] parts = line.split(",");

                    if (section.equals("NODES") && parts.length == 3) {
                        String id = parts[0];
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        Node node = new Node(id, x, y);
                        graph.addNode(node);
                        nodeMap.put(id, node); // Stocke pour les arêtes
                    }
                    else if (section.equals("EDGES") && parts.length == 3) {
                        Node source = nodeMap.get(parts[0]);
                        Node target = nodeMap.get(parts[1]);
                        int weight = Integer.parseInt(parts[2]);
                        if (source != null && target != null) {
                            // On charge en non-orienté
                            graph.addOrUpdateEdge(source, target, weight);
                        }
                    }
                    else if (section.equals("NEXT_LETTER") && parts.length == 1) {
                        GraphPanel.setNextNodeLetter(parts[0].charAt(0));
                    }
                }

                // Terminé
                panel.repaint();
                System.out.println("Graphe chargé depuis : " + fileToLoad.getAbsolutePath());

            } catch (Exception e) {
                graph.clear(); // En cas d'erreur, on repart à zéro
                JOptionPane.showMessageDialog(panel, "Erreur lors du chargement du graphe : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}