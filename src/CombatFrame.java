import java.awt.BorderLayout;
import java.awt.Font; // Import Font
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Fenêtre pour le mode "Combat".
 * [MODIFIÉ] Ajout de A* aux options.
 */
public class CombatFrame extends JFrame {

    // --- Fields (Graph, Panel, Visualizers, Timers, etc.) ---
    private Graph graphLeft, graphRight;
    private GraphPanel panelLeft, panelRight;
    private AlgorithmVisualizer visualizerLeft, visualizerRight;
    private Timer combatLogicTimer, combatRepaintTimer;
    private JComboBox<String> selectorLeft, selectorRight;
    private JTextArea logArea;
    private JLabel statusLabelLeft, statusLabelRight;
    private JButton startCombatButton;
    private JSlider speedSliderCombat;
    private double resultLeft = Double.POSITIVE_INFINITY;
    private double resultRight = Double.POSITIVE_INFINITY;
    private int stepsLeft = 0;
    private int stepsRight = 0;
    private Map<String, String> complexityMap;
    private final int COMBAT_LOGIC_DELAY = 500;
    private final int COMBAT_REPAINT_DELAY = 16;
    // --- End Fields ---

    public CombatFrame(Graph originalGraph) {
        super("Mode Combat !");
        this.graphLeft = originalGraph.deepCopy();
        this.graphRight = originalGraph.deepCopy();

        initComplexityMap();
        initComponents();
        setupTimers();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { stopCombat(); }
        });

        setSize(1600, 800); // Adjusted size slightly if needed
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    // --- MODIFIÉ : Ajout complexité A* ---
    private void initComplexityMap() {
        complexityMap = new HashMap<>();
        // Complexités typiques (peuvent varier selon l'implémentation exacte)
        // V = Nœuds (Vertices), E = Arêtes (Edges)
        complexityMap.put("Dijkstra", "O(E log V)"); // Avec file de priorité
        complexityMap.put("A*", "O(E log V)");       // [AJOUTÉ] Similaire à Dijkstra avec heuristique
        complexityMap.put("BFS", "O(V + E)");
        complexityMap.put("DFS", "O(V + E)");
        complexityMap.put("Prim", "O(E log V)");   // Avec file de priorité
        complexityMap.put("Kruskal", "O(E log E)"); // Tri des arêtes dominant
        // Le générateur n'est plus dans cette liste pour le combat
    }

    private void initComponents() {
        panelLeft = new GraphPanel(graphLeft, null);
        panelRight = new GraphPanel(graphRight, null);
        JSplitPane graphSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft, panelRight);
        graphSplitPane.setResizeWeight(0.5);

        // --- MODIFIÉ : Ajout de A* à la liste ---
        String[] algorithms = {
                "Dijkstra",
                "A*", // [AJOUTÉ]
                "BFS",
                "DFS",
                "Prim",
                "Kruskal"
                // On exclut le Générateur du combat
        };
        selectorLeft = new JComboBox<>(algorithms);
        selectorRight = new JComboBox<>(algorithms);
        startCombatButton = new JButton("Lancer le Combat !");
        speedSliderCombat = new JSlider(0, 1000, COMBAT_LOGIC_DELAY);
        speedSliderCombat.setMajorTickSpacing(250); speedSliderCombat.setMinorTickSpacing(50);
        speedSliderCombat.setPaintTicks(true); speedSliderCombat.setPaintLabels(true);
        speedSliderCombat.setInverted(true);

        JPanel controlPanelTop = new JPanel();
        controlPanelTop.add(new JLabel("Algo 1:")); controlPanelTop.add(selectorLeft);
        controlPanelTop.add(new JLabel("Algo 2:")); controlPanelTop.add(selectorRight);
        controlPanelTop.add(startCombatButton);

        JPanel controlPanelBottom = new JPanel(new BorderLayout());
        controlPanelBottom.add(new JLabel("Vitesse", JLabel.CENTER), BorderLayout.NORTH);
        controlPanelBottom.add(speedSliderCombat, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        controlPanel.add(controlPanelTop, BorderLayout.NORTH);
        controlPanel.add(controlPanelBottom, BorderLayout.CENTER);

        statusLabelLeft = new JLabel("Algo 1: Prêt");
        statusLabelRight = new JLabel("Algo 2: Prêt");
        JPanel statusPanel = new JPanel(new GridLayout(1, 2));
        statusPanel.add(statusLabelLeft);
        statusPanel.add(statusLabelRight);

        logArea = new JTextArea(8, 40);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusPanel, BorderLayout.NORTH);
        bottomPanel.add(logScrollPane, BorderLayout.CENTER);
        // Pas de label gagnant ici, on utilise la pop-up

        setLayout(new BorderLayout());
        add(graphSplitPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.SOUTH);

        startCombatButton.addActionListener(e -> startCombat());
        speedSliderCombat.addChangeListener(e -> {
            if (combatLogicTimer != null) combatLogicTimer.setDelay(speedSliderCombat.getValue());
        });
    }

    private void setupTimers() {
        combatLogicTimer = new Timer(speedSliderCombat.getValue(), e -> runCombatStep());
        combatRepaintTimer = new Timer(COMBAT_REPAINT_DELAY, e -> {
            panelLeft.repaint();
            panelRight.repaint();
        });
    }

    private void startCombat() {
        if (combatLogicTimer.isRunning()) return;
        if (graphLeft.getNodes().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Graphe vide.", "Erreur", JOptionPane.WARNING_MESSAGE); return;
        }

        stopCombat();
        graphLeft.resetNodes(); graphRight.resetNodes();
        panelLeft.reset(); panelRight.reset();
        logArea.setText("");
        statusLabelLeft.setText("Algo 1: En cours...");
        statusLabelRight.setText("Algo 2: En cours...");
        resultLeft = Double.POSITIVE_INFINITY;
        resultRight = Double.POSITIVE_INFINITY;
        stepsLeft = 0;
        stepsRight = 0;

        // Définit start/end (si applicable)
        Node startNodeRef = graphLeft.getNodes().isEmpty() ? null : graphLeft.getNodes().get(0);
        Node endNodeRef = graphLeft.getNodes().isEmpty() ? null : graphLeft.getNodes().get(graphLeft.getNodes().size() - 1);
        Node startNodeLeft = (startNodeRef == null) ? null : graphLeft.findNodeById(startNodeRef.getId());
        Node endNodeLeft = (endNodeRef == null) ? null : graphLeft.findNodeById(endNodeRef.getId());
        Node startNodeRight = (startNodeRef == null) ? null : graphRight.findNodeById(startNodeRef.getId());
        Node endNodeRight = (endNodeRef == null) ? null : graphRight.findNodeById(endNodeRef.getId());

        if (startNodeLeft != null && endNodeLeft != null) {
            panelLeft.setSelectedStartNodeManually(startNodeLeft);
            panelLeft.setSelectedEndNodeManually(endNodeLeft);
        }
        if (startNodeRight != null && endNodeRight != null) {
            panelRight.setSelectedStartNodeManually(startNodeRight);
            panelRight.setSelectedEndNodeManually(endNodeRight);
        }

        String algo1 = (String) selectorLeft.getSelectedItem();
        String algo2 = (String) selectorRight.getSelectedItem();

        visualizerLeft = createVisualizer(graphLeft, algo1, startNodeLeft, endNodeLeft);
        visualizerRight = createVisualizer(graphRight, algo2, startNodeRight, endNodeRight);

        // Validation (si createVisualizer a échoué ou si start/end manquant pour algos requis)
        boolean error = false;
        if (requiresStartEnd(algo1) && (startNodeLeft == null || endNodeLeft == null)) error = true;
        if (requiresStartEnd(algo2) && (startNodeRight == null || endNodeRight == null)) error = true;
        if (requiresStartOnly(algo1) && startNodeLeft == null) error = true;
        if (requiresStartOnly(algo2) && startNodeRight == null) error = true;


        if (error) {
            JOptionPane.showMessageDialog(this, "Nœuds Start/End requis ou erreur.", "Erreur", JOptionPane.ERROR_MESSAGE);
            stopCombat();
            return;
        }
        if (visualizerLeft == null || visualizerRight == null) {
            JOptionPane.showMessageDialog(this, "Erreur instanciation algos.", "Erreur", JOptionPane.ERROR_MESSAGE);
            stopCombat();
            return;
        }

        panelLeft.setVisualizer(visualizerLeft);
        panelRight.setVisualizer(visualizerRight);

        combatLogicTimer.setDelay(speedSliderCombat.getValue());
        combatLogicTimer.start();
        if (!combatRepaintTimer.isRunning()) combatRepaintTimer.start();
    }

    // --- MODIFIÉ : Ajout gestion A* ---
    private AlgorithmVisualizer createVisualizer(Graph graph, String algoName, Node start, Node end) {
        if (algoName == null) return null;
        if (algoName.startsWith("Dijkstra")) return new DijkstraVisualizer(graph, start, end);
        if (algoName.startsWith("A*")) return new AStarVisualizer(graph, start, end); // [AJOUTÉ]
        if (algoName.startsWith("BFS")) return new BfsVisualizer(graph, start, end);
        if (algoName.startsWith("DFS")) return new DfsVisualizer(graph, start, end);
        if (algoName.startsWith("Prim")) return new PrimVisualizer(graph, start, end);
        if (algoName.startsWith("Kruskal")) return new KruskalVisualizer(graph);
        return null;
    }

    // Helper methods for validation
    private boolean requiresStartEnd(String algoName){
        return algoName.startsWith("Dijkstra") || algoName.startsWith("A*") ||
                algoName.startsWith("BFS") || algoName.startsWith("DFS");
    }
    private boolean requiresStartOnly(String algoName){
        return algoName.startsWith("Prim"); // Kruskal needs nothing, Pathfinders need both
    }


    private void runCombatStep() {
        boolean runningLeft = visualizerLeft != null && !visualizerLeft.isFinished();
        boolean runningRight = visualizerRight != null && !visualizerRight.isFinished();

        if (runningLeft) stepsLeft++;
        if (runningRight) stepsRight++;

        if (runningLeft) {
            visualizerLeft.setLogicTimerDelay(combatLogicTimer.getDelay());
            runningLeft = visualizerLeft.step();
            logArea.append("[G] " + visualizerLeft.getStatusMessage() + "\n");
            if (!runningLeft) {
                updateFinalStatus(visualizerLeft, statusLabelLeft, 1, (String) selectorLeft.getSelectedItem());
            }
        }

        if (runningRight) {
            visualizerRight.setLogicTimerDelay(combatLogicTimer.getDelay());
            runningRight = visualizerRight.step();
            logArea.append("[D] " + visualizerRight.getStatusMessage() + "\n");
            if (!runningRight) {
                updateFinalStatus(visualizerRight, statusLabelRight, 2, (String) selectorRight.getSelectedItem());
            }
        }

        logArea.setCaretPosition(logArea.getDocument().getLength());

        if (!runningLeft && !runningRight) {
            combatLogicTimer.stop();
            showWinnerDialog();
        }
    }

    private void updateFinalStatus(AlgorithmVisualizer viz, JLabel label, int algoNum, String algoName) {
        String resultText = "Terminé. ";
        double finalResult = Double.POSITIVE_INFINITY;
        String complexity = complexityMap.getOrDefault(algoName, "N/A");
        int steps = (algoNum == 1) ? stepsLeft : stepsRight;

        Node endNodeRef = panelLeft.getSelectedEndNode(); // Use left selection as reference
        // Check if it's an algorithm that finds a path cost/steps
        boolean isPathfindingAlgo = viz instanceof DijkstraVisualizer || viz instanceof AStarVisualizer ||
                viz instanceof BfsVisualizer || viz instanceof DfsVisualizer;

        if (isPathfindingAlgo && endNodeRef != null) {
            Node actualEndNode = (algoNum == 1) ? graphLeft.findNodeById(endNodeRef.getId()) : graphRight.findNodeById(endNodeRef.getId());
            // For pathfinders, the result is the gCost (distance)
            if(actualEndNode != null && actualEndNode.getGCost() != Double.POSITIVE_INFINITY){ // Use gCost
                finalResult = actualEndNode.getGCost();
                resultText += (algoName.startsWith("BFS") || algoName.startsWith("DFS") ? "Étapes: " : "Coût: ") + (int)finalResult;
            } else {
                resultText += "(Non Atteint)";
                finalResult = Double.POSITIVE_INFINITY;
            }
        } else if (viz instanceof PrimVisualizer || viz instanceof KruskalVisualizer) { // For MST
            String finalMsg = viz.getStatusMessage();
            String costPrefix = "Coût total du MST : "; String costPrefix2 = "Coût : ";
            int costIndex = finalMsg.indexOf(costPrefix);
            if (costIndex == -1) costIndex = finalMsg.indexOf(costPrefix2);
            if(costIndex != -1){
                try {
                    String costStr = finalMsg.substring(costIndex + (finalMsg.contains(costPrefix)? costPrefix.length() : costPrefix2.length())).trim();
                    finalResult = Double.parseDouble(costStr);
                    resultText += "Coût Total: " + (int)finalResult;
                } catch (Exception e) { finalResult = Double.POSITIVE_INFINITY; resultText += "(Erreur coût)"; }
            } else { finalResult = Double.POSITIVE_INFINITY; resultText += "(Erreur coût)"; }
        } else { // For Generator or others without a specific cost metric
            finalResult = steps; // The 'score' is just the number of steps
            resultText += "(Terminé)";
        }

        label.setText(String.format("Algo %d (%s): %s [%d étapes]", algoNum, complexity, resultText, steps));

        if (algoNum == 1) resultLeft = finalResult;
        else resultRight = finalResult;
    }

    private void showWinnerDialog() {
        String winnerMessage = "Résultat du Combat :\n\n";
        winnerMessage += statusLabelLeft.getText() + "\n";
        winnerMessage += statusLabelRight.getText() + "\n\n";
        winnerMessage += "Gagnant : ";

        boolean leftFailed = resultLeft == Double.POSITIVE_INFINITY;
        boolean rightFailed = resultRight == Double.POSITIVE_INFINITY;
        String algo1Name = selectorLeft.getSelectedItem().toString();
        String algo2Name = selectorRight.getSelectedItem().toString();

        if (leftFailed && rightFailed) {
            winnerMessage += "Aucun (les deux ont échoué ou N/A)";
        } else if (leftFailed) {
            winnerMessage += algo2Name + " (Algo 1 a échoué)";
        } else if (rightFailed) {
            winnerMessage += algo1Name + " (Algo 2 a échoué)";
        } else {
            // Compare results (lower is better for cost/steps)
            if (resultLeft < resultRight) {
                winnerMessage += algo1Name + " !";
            } else if (resultRight < resultLeft) {
                winnerMessage += algo2Name + " !";
            } else {
                // Tie-breaker using steps
                winnerMessage += "Égalité sur le score ! Tie-breaker par étapes : ";
                if (stepsLeft < stepsRight) {
                    winnerMessage += algo1Name + " gagne (" + stepsLeft + " vs " + stepsRight + " étapes)";
                } else if (stepsRight < stepsLeft) {
                    winnerMessage += algo2Name + " gagne (" + stepsRight + " vs " + stepsLeft + " étapes)";
                } else {
                    winnerMessage += "Égalité parfaite ! (" + stepsLeft + " étapes)";
                }
            }
        }

        Object[] options = {"Recommencer", "Fermer"};
        int choice = JOptionPane.showOptionDialog(
                this, winnerMessage, "Combat Terminé",
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0] );

        if (choice == JOptionPane.YES_OPTION) {
            startCombat();
        } else {
            stopCombat();
            this.dispose();
        }
    }

    private void stopCombat() {
        if (combatLogicTimer != null) combatLogicTimer.stop();
        if (combatRepaintTimer != null) combatRepaintTimer.stop();
        visualizerLeft = null;
        visualizerRight = null;
        if(panelLeft != null) panelLeft.reset();
        if(panelRight != null) panelRight.reset();
    }
}