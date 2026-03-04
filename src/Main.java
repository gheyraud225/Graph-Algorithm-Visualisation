import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class Main {

    private static Graph graph; // Now instantiated after user choice
    private static GraphPanel graphPanel;
    private static AlgorithmVisualizer visualizer;

    private static Timer logicTimer;
    private static Timer repaintTimer;
    private static Timer layoutTimer;
    private static final int LOGIC_DELAY_MS = 1000;
    private static final int REPAINT_DELAY_MS = 16;
    private static final int LAYOUT_DELAY_MS = 20;

    private static JTextArea logTextArea;
    private static JSlider speedSlider;
    private static JToggleButton editModeButton;
    private static JComboBox<String> algorithmSelector;

    private static ComplexGraphGenerator complexGenerator;
    private static JSpinner nodesSpinner;
    private static JSpinner edgesSpinner;
    private static JCheckBox weightedCheckBox;
    private static JSpinner maxWeightSpinner;

    private static ForceDirectedLayout forceLayout;
    private static CircularLayout circularLayout;
    private static RandomLayout randomLayout;
    public static HierarchicalLayout hierarchicalLayout; // Assume you added this
    private static JComboBox<String> layoutSelector;

    public static void main(String[] args) {

        // --- NOUVEAU : Demander le type de graphe ---
        Object[] options = {"Non Orienté", "Orienté"};
        int choice = JOptionPane.showOptionDialog(null, // Parent (null for center screen)
                "Quel type de graphe voulez-vous utiliser ?", // Message
                "Type de Graphe",                             // Titre
                JOptionPane.YES_NO_OPTION,                    // Type (Yes = Non Orienté, No = Orienté)
                JOptionPane.QUESTION_MESSAGE,                 // Icone
                null,                                         // Icone perso
                options,                                      // Texte boutons
                options[0]);                                  // Défaut

        // Si l'utilisateur ferme la boîte de dialogue
        if (choice == JOptionPane.CLOSED_OPTION) {
            System.exit(0); // Quitte l'application
        }

        boolean isDirectedGraph = (choice == JOptionPane.NO_OPTION);
        // --- FIN Demande type ---


        SwingUtilities.invokeLater(() -> {

            // --- NOUVEAU : Initialise le graphe avec le type choisi ---
            graph = new Graph(isDirectedGraph);

            editModeButton = new JToggleButton("Mode Édition (OFF)");
            complexGenerator = new ComplexGraphGenerator();

            // createGraph() n'est plus appelé ici, on commence vide ou charge
            // createGraph(); // A commenter ou supprimer
            graphPanel = new GraphPanel(graph, editModeButton); // Passe le graphe (qui connaît son type)

            // Initialise les layouts (ont besoin du graph et panel)
            forceLayout = new ForceDirectedLayout(graph, graphPanel);
            circularLayout = new CircularLayout();
            randomLayout = new RandomLayout();
            hierarchicalLayout = new HierarchicalLayout(); // Assume you created this

            // --- Configuration des Timers ---
            logicTimer = new Timer(LOGIC_DELAY_MS, e -> { /* ... (logic timer unchanged) ... */ });
            logicTimer = new Timer(LOGIC_DELAY_MS, e -> {
                if (visualizer != null) {
                    visualizer.setLogicTimerDelay(logicTimer.getDelay());
                    boolean isRunning = visualizer.step();
                    String message = visualizer.getStatusMessage();
                    logTextArea.append(message + "\n");
                    logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
                    if (!isRunning) {
                        logicTimer.stop();
                    }
                }
            });
            repaintTimer = new Timer(REPAINT_DELAY_MS, e -> {
                if (graphPanel != null) { graphPanel.repaint(); }
            });
            repaintTimer.start();
            layoutTimer = new Timer(LAYOUT_DELAY_MS, e -> {
                if (forceLayout != null && forceLayout.isRunning()) {
                    boolean stillRunning = forceLayout.step();
                    if (!stillRunning) {
                        layoutTimer.stop();
                        logTextArea.append("Layout Force-Directed terminé.\n");
                    }
                } else { layoutTimer.stop(); }
            });

            // ============================================
            // --- Configuration de l'Interface (GUI) ---
            // ============================================

            // --- 1. Panneau Algo ---
            JButton startButton = new JButton("Lancer");
            JButton resetButton = new JButton("Réinitialiser");
            String[] algorithms = {
                    "--- Algorithmes de Chemin ---",
                    "Dijkstra (Chemin le plus court)", "A* (A-Star Search)",
                    "BFS (Parcours en largeur)", "DFS (Parcours en profondeur)",
                    "--- Algorithmes de MST ---",
                    "Prim (Arbre couvrant min.)", "Kruskal (Arbre couvrant min.)",
                    "--- Générateurs ---",
                    "Générer Labyrinthe"
            };
            algorithmSelector = new JComboBox<>(algorithms);
            algorithmSelector.setRenderer(new DefaultListCellRenderer());
            algorithmSelector.addActionListener(e -> { /* ... (skip titles listener unchanged) ... */ });
            algorithmSelector.addActionListener(e -> {
                if (algorithmSelector.getSelectedItem() != null &&
                        algorithmSelector.getSelectedItem().toString().startsWith("---")) {
                    int currentIndex = algorithmSelector.getSelectedIndex();
                    if (currentIndex < algorithmSelector.getItemCount() - 1) {
                        algorithmSelector.setSelectedIndex(currentIndex + 1);
                    } else if (currentIndex > 0) {
                        algorithmSelector.setSelectedIndex(currentIndex - 1);
                    }
                }
                // NOUVEAU: Griser les algos MST si orienté
                updateAlgorithmSelectionAvailability();
            });


            JPanel algoControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            algoControlPanel.add(algorithmSelector);
            algoControlPanel.add(startButton);
            algoControlPanel.add(resetButton);

            // --- 2. Panneau Édition / Fichier / ... ---
            JButton saveButton = new JButton("Sauvegarder");
            JButton loadButton = new JButton("Charger");
            JButton combatButton = new JButton("Mode Combat");
            String[] layoutOptions = {"Appliquer Layout:", "Force-Directed (Animé)", "Hiérarchique (Simple)", "Circulaire", "Aléatoire"};
            layoutSelector = new JComboBox<>(layoutOptions);

            JPanel fileControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            fileControlPanel.add(editModeButton);
            fileControlPanel.add(saveButton);
            fileControlPanel.add(loadButton);
            fileControlPanel.add(combatButton);
            fileControlPanel.add(layoutSelector);

            // --- 3. Panneau Générateur Complexe ---
            nodesSpinner = new JSpinner(new SpinnerNumberModel(20, 2, 500, 1));
            edgesSpinner = new JSpinner(new SpinnerNumberModel(40, 0, 20000, 5));
            weightedCheckBox = new JCheckBox("Poids", false);
            maxWeightSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 1));
            maxWeightSpinner.setEnabled(false);
            weightedCheckBox.addActionListener(e -> maxWeightSpinner.setEnabled(weightedCheckBox.isSelected()));
            JButton generateComplexButton = new JButton("Générer Graphe Complexe");
            JPanel generatorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            generatorPanel.setBorder(BorderFactory.createTitledBorder("Génération Complexe"));
            // ... (Add components to generatorPanel) ...
            generatorPanel.add(new JLabel("Nœuds:")); generatorPanel.add(nodesSpinner);
            generatorPanel.add(new JLabel("Arêtes:")); generatorPanel.add(edgesSpinner);
            generatorPanel.add(weightedCheckBox);
            generatorPanel.add(new JLabel("Poids Max:")); generatorPanel.add(maxWeightSpinner);
            generatorPanel.add(generateComplexButton);


            // --- 4. Assemblage Panneaux Supérieurs ---
            JPanel topControlPanel = new JPanel(new GridLayout(0, 1));
            topControlPanel.add(algoControlPanel);
            topControlPanel.add(fileControlPanel);
            topControlPanel.add(generatorPanel);

            // --- 5. Slider Vitesse ---
            speedSlider = new JSlider(0, 2000, LOGIC_DELAY_MS);
            // ... (slider config) ...
            speedSlider.setMajorTickSpacing(500); speedSlider.setMinorTickSpacing(100);
            speedSlider.setPaintTicks(true); speedSlider.setPaintLabels(true);
            speedSlider.setInverted(true);
            JPanel sliderPanel = new JPanel(new BorderLayout());
            sliderPanel.add(new JLabel("Vitesse Algo (plus rapide en haut)", JLabel.CENTER), BorderLayout.NORTH);
            sliderPanel.add(speedSlider, BorderLayout.CENTER);

            // --- 6. Panneau Aide ---
            JPanel helpPanel = new JPanel(new GridLayout(0, 1));
            // ... (help panel config) ...
            helpPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
            helpPanel.add(new JLabel("--- Mode Algorithme ---", JLabel.CENTER));
            helpPanel.add(new JLabel("Clic gauche (Nœud) : Sélectionner Départ/Arrivée"));
            helpPanel.add(new JLabel("Clic droit : Réinitialiser la sélection"));
            helpPanel.add(new JLabel("--- Mode Édition ---", JLabel.CENTER));
            helpPanel.add(new JLabel("Clic gauche (Vide) : Ajouter un nœud"));
            helpPanel.add(new JLabel("Clic (Nœud A) -> Clic (Nœud B) : Créer Arête" + (isDirectedGraph ? " (Orientée)" : ""))); // Indique type arête
            helpPanel.add(new JLabel("Glisser (Nœud) : Déplacer le nœud"));
            helpPanel.add(new JLabel("Clic droit (Nœud/Arête) : Supprimer"));

            // --- 7. Panneau Contrôle Principal ---
            JPanel mainControlPanel = new JPanel(new BorderLayout());
            // ... (add panels) ...
            mainControlPanel.add(topControlPanel, BorderLayout.NORTH);
            mainControlPanel.add(sliderPanel, BorderLayout.CENTER);
            mainControlPanel.add(helpPanel, BorderLayout.SOUTH);
            mainControlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // --- 8. Log ---
            logTextArea = new JTextArea(10, 30);
            // ... (log config) ...
            logTextArea.setEditable(false);
            logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            logTextArea.setLineWrap(true); logTextArea.setWrapStyleWord(true);
            JScrollPane logScrollPane = new JScrollPane(logTextArea);

            // --- 9. Fenêtre ---
            JFrame frame = new JFrame("Visualiseur de Graphe " + (isDirectedGraph ? "Orienté" : "Non Orienté")); // Titre indique type
            // ... (frame config) ...
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(graphPanel, BorderLayout.CENTER);
            frame.add(mainControlPanel, BorderLayout.SOUTH);
            frame.add(logScrollPane, BorderLayout.EAST);
            frame.pack();
            frame.setLocationRelativeTo(null);

            // NOUVEAU: Met à jour la dispo des algos après création GUI
            updateAlgorithmSelectionAvailability();

            frame.setVisible(true); // Rend visible APRES setup complet


            // ============================================
            // --- Logique des Composants ---
            // ============================================
            editModeButton.addActionListener(e -> { /* ... (inchangé) ... */ });
            editModeButton.addActionListener(e -> {
                if (editModeButton.isSelected()) {
                    editModeButton.setText("Mode Édition (ON)"); resetVisualization();
                } else {
                    editModeButton.setText("Mode Édition (OFF)");
                }
            });
            startButton.addActionListener(e -> startVisualization());
            resetButton.addActionListener(e -> resetVisualization());
            speedSlider.addChangeListener(e -> { /* ... (inchangé) ... */ });
            speedSlider.addChangeListener(e -> {
                if(logicTimer != null) { logicTimer.setDelay(speedSlider.getValue()); }
            });
            saveButton.addActionListener(e -> { /* ... (inchangé) ... */ });
            saveButton.addActionListener(e -> {
                resetVisualization(); GraphIO.saveGraph(graph, graphPanel);
            });
            loadButton.addActionListener(e -> { // Doit gérer le type de graphe chargé?
                resetVisualization();
                // GraphIO.loadGraph doit peut-être retourner le type ou on assume qu'il correspond?
                // Pour l'instant, on assume que l'utilisateur charge un graphe du bon type
                GraphIO.loadGraph(graph, graphPanel); // Recharge dans le graphe EXISTANT
                if (graphPanel != null) graphPanel.repaint();
                logTextArea.setText("Graphe chargé. Type: " + (graph.isDirected() ? "Orienté" : "Non Orienté") + "\n");
                updateAlgorithmSelectionAvailability(); // Met à jour dispo après chargement
            });
            combatButton.addActionListener(e -> { /* ... (inchangé) ... */ });
            combatButton.addActionListener(e -> {
                if (graph == null || graph.getNodes().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Graphe vide.", "Erreur", JOptionPane.WARNING_MESSAGE); return;
                }
                resetVisualization();
                Graph graphCopy = graph.deepCopy();
                CombatFrame combatFrame = new CombatFrame(graphCopy);
                combatFrame.setVisible(true);
            });
            generateComplexButton.addActionListener(e -> { /* ... (inchangé) ... */ });
            generateComplexButton.addActionListener(e -> {
                resetVisualization();
                if (editModeButton.isSelected()) editModeButton.doClick();
                int numNodes = (int) nodesSpinner.getValue();
                int numEdges = (int) edgesSpinner.getValue();
                boolean weighted = weightedCheckBox.isSelected();
                int maxWeight = (int) maxWeightSpinner.getValue();
                complexGenerator.generate(graph, numNodes, numEdges, weighted, maxWeight, graphPanel.getWidth(), graphPanel.getHeight());
                graphPanel.repaint();
                logTextArea.setText("Graphe complexe généré !\n");
            });
            layoutSelector.addActionListener(e -> { /* ... (inchangé) ... */ });
            layoutSelector.addActionListener(e -> {
                String selectedLayout = (String) layoutSelector.getSelectedItem();
                if (selectedLayout == null || selectedLayout.equals("Appliquer Layout:")) return;
                if (graph == null || graph.getNodes().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Graphe vide.", "Erreur", JOptionPane.WARNING_MESSAGE);
                    layoutSelector.setSelectedIndex(0); return;
                }
                resetVisualization();
                if (editModeButton.isSelected()) editModeButton.doClick();
                logTextArea.append("Application layout: " + selectedLayout + "...\n");
                switch (selectedLayout) {
                    case "Force-Directed (Animé)": forceLayout.start(); layoutTimer.start(); break;
                    case "Hiérarchique (Simple)": // Keep name simple for now
                        // Warning if undirected is still good
                        if (!graph.isDirected()) {
                            JOptionPane.showMessageDialog(frame, "Layout hiérarchique fonctionne mieux sur graphes orientés (cycles seront ignorés).", "Avertissement", JOptionPane.INFORMATION_MESSAGE);
                        }
                        hierarchicalLayout.apply(graph, graphPanel);
                        graphPanel.repaint();
                        logTextArea.append("Layout Hiérarchique appliqué.\n");
                        break;
                    case "Circulaire": circularLayout.apply(graph, graphPanel); graphPanel.repaint(); logTextArea.append("Layout Circulaire appliqué.\n"); break;
                    case "Aléatoire": randomLayout.apply(graph, graphPanel); graphPanel.repaint(); logTextArea.append("Layout Aléatoire appliqué.\n"); break;
                }
                layoutSelector.setSelectedIndex(0); // Reset selector
            });
            // --- Fin Listeners ---

        }); // Fin SwingUtilities.invokeLater
    } // Fin main

    /** Lance la visualisation */
    private static void startVisualization() {
        if (layoutTimer != null && layoutTimer.isRunning()) {
            layoutTimer.stop(); if (forceLayout != null) forceLayout.stop(); logTextArea.append("Layout interrompu.\n");
        }
        if (editModeButton.isSelected()) { editModeButton.doClick(); }
        if (logicTimer != null && logicTimer.isRunning()) return;

        String selectedAlgo = (String) algorithmSelector.getSelectedItem();
        if (selectedAlgo == null || selectedAlgo.startsWith("---")) return;

        // Validation (adaptée)
        boolean isGenerator = selectedAlgo.startsWith("Générer");
        boolean isMST = selectedAlgo.startsWith("Prim") || selectedAlgo.startsWith("Kruskal");

        if (isGenerator) {
            logTextArea.setText("");
            visualizer = new MazeGeneratorVisualizer(graph, graphPanel.getWidth(), graphPanel.getHeight());
        } else if (isMST) {
            if (graph.isDirected()) { // Vérifie compatibilité MST
                JOptionPane.showMessageDialog(graphPanel, "Prim/Kruskal requièrent un graphe NON ORIENTÉ.", "Erreur", JOptionPane.WARNING_MESSAGE); return;
            }
            if (selectedAlgo.startsWith("Kruskal")) {
                logTextArea.setText(""); graph.resetNodes(); visualizer = new KruskalVisualizer(graph);
            } else { // Prim
                Node start = graphPanel.getSelectedStartNode();
                if (start == null) { JOptionPane.showMessageDialog(graphPanel, "Prim nécessite un NŒUD DE DÉPART.", "Erreur", JOptionPane.WARNING_MESSAGE); return; }
                logTextArea.setText(""); graph.resetNodes(); visualizer = new PrimVisualizer(graph, start, null);
            }
        } else { // Algorithmes de chemin
            Node start = graphPanel.getSelectedStartNode();
            Node end = graphPanel.getSelectedEndNode();
            if (start == null) { JOptionPane.showMessageDialog(graphPanel, "NŒUD DE DÉPART requis.", "Erreur", JOptionPane.WARNING_MESSAGE); return; }
            if (end == null) { JOptionPane.showMessageDialog(graphPanel, "NŒUD D'ARRIVÉE requis.", "Erreur", JOptionPane.WARNING_MESSAGE); return; }

            logTextArea.setText("");
            graph.resetNodes();
            if (selectedAlgo.startsWith("Dijkstra")) { visualizer = new DijkstraVisualizer(graph, start, end); }
            else if (selectedAlgo.startsWith("A*")) { visualizer = new AStarVisualizer(graph, start, end); }
            else if (selectedAlgo.startsWith("BFS")) { visualizer = new BfsVisualizer(graph, start, end); }
            else if (selectedAlgo.startsWith("DFS")) { visualizer = new DfsVisualizer(graph, start, end); }
            else { return; }
        }

        // Lancement
        graphPanel.setVisualizer(visualizer);
        if (logicTimer != null) logicTimer.start();
    }

    /** Reset */
    private static void resetVisualization() {
        if (logicTimer != null) logicTimer.stop();
        if (layoutTimer != null) layoutTimer.stop();
        visualizer = null;
        if (forceLayout != null) forceLayout.stop();
        if (graphPanel != null) graphPanel.reset();
        // Log non effacé ici
    }

    /** Crée graphe initial (maintenant vide, car type choisi au début) */
    private static void createGraph() {
        // Le graphe est créé dans main() après le choix de l'utilisateur
        // On peut laisser cette méthode vide ou pour charger un exemple par défaut SI NON ORIENTÉ
        if (graph != null && !graph.isDirected()) {
            // Optionnel : Charger le graphe A-F par défaut si non orienté
            Node a = new Node("A", 100, 300); Node b = new Node("B", 300, 150);
            Node c = new Node("C", 300, 450); Node d = new Node("D", 500, 300);
            Node e = new Node("E", 700, 150); Node f = new Node("F", 700, 450);
            graph.addNode(a); graph.addNode(b); graph.addNode(c);
            graph.addNode(d); graph.addNode(e); graph.addNode(f);
            graph.addOrUpdateEdge(a, b, 7); graph.addOrUpdateEdge(a, c, 14); // Utilise addOrUpdateEdge
            graph.addOrUpdateEdge(a, d, 20); graph.addOrUpdateEdge(b, d, 4);
            graph.addOrUpdateEdge(c, d, 2); graph.addOrUpdateEdge(b, e, 10);
            graph.addOrUpdateEdge(d, f, 11); graph.addOrUpdateEdge(e, f, 5);
            GraphPanel.setNextNodeLetter('G');
        } else if (graph != null && graph.isDirected()) {
            // Optionnel : Charger un exemple orienté par défaut si orienté
            Node a = new Node("A", 100, 200); Node b = new Node("B", 300, 100);
            Node c = new Node("C", 300, 300); Node d = new Node("D", 500, 200);
            graph.addNode(a); graph.addNode(b); graph.addNode(c); graph.addNode(d);
            graph.addOrUpdateEdge(a, b, 1); graph.addOrUpdateEdge(a, c, 1);
            graph.addOrUpdateEdge(b, d, 1); graph.addOrUpdateEdge(c, d, 1);
            GraphPanel.setNextNodeLetter('E');
        }
    }

    /** NOUVEAU: Met à jour l'état activé/désactivé des algos MST dans le JComboBox */
    private static void updateAlgorithmSelectionAvailability() {
        if (algorithmSelector == null || graph == null) return;

        boolean mstAllowed = !graph.isDirected();
        // Le rendu personnalisé va gérer l'apparence grisée
        // Ici on pourrait sélectionner un algo par défaut si l'actuel est désactivé
        String currentSelection = (String) algorithmSelector.getSelectedItem();
        if (currentSelection != null && !isAlgorithmCompatible(currentSelection, graph.isDirected())) {
            algorithmSelector.setSelectedIndex(1); // Sélectionne Dijkstra par défaut
        }
        // Force le rendu à se mettre à jour
        algorithmSelector.repaint();
    }

    /** NOUVEAU: Vérifie si un algo est compatible avec le type de graphe */
    private static boolean isAlgorithmCompatible(String algoName, boolean isDirected) {
        if (algoName == null || algoName.startsWith("---")) return false; // Titres non compatibles
        boolean isMST = algoName.startsWith("Prim") || algoName.startsWith("Kruskal");
        if (isMST && isDirected) return false; // MST interdit si orienté
        // Générateur Labyrinthe est ok pour les deux types (il génère non-orienté)
        // Les autres (Chemin) sont ok pour les deux types
        return true;
    }


    // --- Classe interne pour le rendu du JComboBox ---
    static class DefaultListCellRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null) {
                String text = value.toString();
                if (text.startsWith("---")) {
                    c.setBackground(Color.LIGHT_GRAY);
                    c.setForeground(Color.BLACK);
                    c.setEnabled(false); // Rend non sélectionnable
                } else {
                    // Vérifie la compatibilité avec le type de graphe actuel
                    boolean compatible = (graph == null) || isAlgorithmCompatible(text, graph.isDirected());
                    c.setEnabled(compatible); // Grise si incompatible
                    c.setBackground(isSelected && compatible ? list.getSelectionBackground() : list.getBackground());
                    c.setForeground(isSelected && compatible ? list.getSelectionForeground() : (compatible ? list.getForeground() : Color.GRAY));
                }
            }
            return c;
        }
    } // Fin DefaultListCellRenderer

} // Fin Main