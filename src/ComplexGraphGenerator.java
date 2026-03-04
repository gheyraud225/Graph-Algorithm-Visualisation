import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Generates random complex graphs based on specified parameters.
 */
public class ComplexGraphGenerator {

    private Random random = new Random();
    private static final int NODE_DIAMETER = 30;
    /**
     * Clears the current graph and generates a new random one.
     *
     * @param graph The Graph object to modify.
     * @param numNodes Number of nodes to create.
     * @param numEdges Number of edges to create.
     * @param weighted If true, edges get random weights; otherwise, weight is 1.
     * @param maxWeight Maximum weight for edges (if weighted).
     * @param panelWidth Width of the drawing area for node placement.
     * @param panelHeight Height of the drawing area for node placement.
     */
    public void generate(Graph graph, int numNodes, int numEdges, boolean weighted, int maxWeight, int panelWidth, int panelHeight) {

        // --- 1. Validation and Preparation ---
        if (numNodes <= 1) return; // Need at least 2 nodes for edges

        // Max possible edges in an undirected graph without self-loops
        long maxPossibleEdges = (long) numNodes * (numNodes - 1) / 2;
        if (numEdges > maxPossibleEdges) {
            numEdges = (int) maxPossibleEdges; // Cap at maximum
            System.out.println("Warning: Requested edges exceed maximum possible. Capping at " + numEdges);
        }
        if (numEdges < 0) numEdges = 0;

        graph.clear();
        GraphPanel.resetNodeLetter(); // Reset naming if needed, though we'll use numbers

        List<Node> nodes = new ArrayList<>(numNodes);
        // Use a set to efficiently check for existing edges
        Set<String> existingEdges = new HashSet<>();

        // --- 2. Create Nodes (Grid Layout with Jitter) ---
        // Place nodes somewhat randomly, avoiding edges of the panel and overlaps
        int margin = 30;
        int availableWidth = panelWidth - 2 * margin;
        int availableHeight = panelHeight - 2 * margin;

        // Calculate approximate grid dimensions
        int gridCols = (int) Math.ceil(Math.sqrt(numNodes));
        int gridRows = (int) Math.ceil((double) numNodes / gridCols);

        // Calculate cell size
        double cellWidth = (double) availableWidth / gridCols;
        double cellHeight = (double) availableHeight / gridRows;

        // Ensure minimum cell size to prevent extreme closeness if many nodes
        cellWidth = Math.max(cellWidth, NODE_DIAMETER * 1.5); // Min 1.5 node diameter width
        cellHeight = Math.max(cellHeight, NODE_DIAMETER * 1.5); // Min 1.5 node diameter height

        // Adjust grid size if cells became larger
        gridCols = (int) (availableWidth / cellWidth);
        gridRows = (int) (availableHeight / cellHeight);
        if (gridCols == 0 || gridRows == 0) { // Safety check if panel is tiny
            System.err.println("Panel too small for grid layout.");
            return;
        }


        int nodeIndex = 0;
        nodeLoop: // Label to break outer loop
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                if (nodeIndex >= numNodes) {
                    break nodeLoop; // Stop if we've placed all requested nodes
                }

                // Calculate center of the cell
                double centerX = margin + c * cellWidth + cellWidth / 2;
                double centerY = margin + r * cellHeight + cellHeight / 2;

                // Add random jitter within the cell (e.g., +/- 25% of cell size)
                double jitterX = (random.nextDouble() - 0.5) * (cellWidth * 0.5);
                double jitterY = (random.nextDouble() - 0.5) * (cellHeight * 0.5);

                int x = (int) (centerX + jitterX);
                int y = (int) (centerY + jitterY);

                // Ensure node stays within panel bounds after jitter
                x = Math.max(margin, Math.min(panelWidth - margin, x));
                y = Math.max(margin, Math.min(panelHeight - margin, y));


                // Use numbers as IDs for simplicity with many nodes
                Node node = new Node(String.valueOf(nodeIndex), x, y);
                graph.addNode(node);
                nodes.add(node);
                nodeIndex++;
            }
        }
        // If grid was too small, place remaining nodes randomly (less ideal)
        while(nodeIndex < numNodes) {
            int x = margin + random.nextInt(panelWidth - 2 * margin);
            int y = margin + random.nextInt(panelHeight - 2 * margin);
            Node node = new Node(String.valueOf(nodeIndex), x, y);
            graph.addNode(node);
            nodes.add(node);
            nodeIndex++;
        }
        // --- End Create Nodes ---
        // --- 3. Create Edges ---
        int edgesCreated = 0;
        // Safety break to prevent infinite loops if something goes wrong
        int maxAttempts = numEdges * 10;
        int attempts = 0;

        while (edgesCreated < numEdges && attempts < maxAttempts) {
            attempts++;
            // Pick two distinct random nodes
            Node u = nodes.get(random.nextInt(numNodes));
            Node v = nodes.get(random.nextInt(numNodes));

            if (u.equals(v)) {
                continue; // Skip self-loops
            }

            // Create a unique ID for the edge pair (order doesn't matter)
            String edgeId = u.getId().compareTo(v.getId()) < 0 ? u.getId() + "-" + v.getId() : v.getId() + "-" + u.getId();

            // Check if edge already exists
            if (!existingEdges.contains(edgeId)) {
                int weight = weighted ? (1 + random.nextInt(maxWeight)) : 1;
                graph.addOrUpdateEdge(u, v, weight);
                existingEdges.add(edgeId);
                edgesCreated++;
            }
        }
        if (edgesCreated < numEdges) {
            System.out.println("Warning: Could only create " + edgesCreated + "/" + numEdges + " unique edges after " + maxAttempts + " attempts.");
        }


        // --- 4. (Optional but recommended) Ensure Connectivity ---
        // This is a more advanced step. A simple approach:
        // Run a BFS/DFS from node 0. Find all reachable nodes.
        // If not all nodes are reachable, pick a random edge between
        // a reachable node and an unreachable node, add it, and repeat BFS/DFS.
        // For now, we'll skip this to keep it simpler, but be aware the
        // generated graph might not be fully connected.

        System.out.println("Generated graph with " + graph.getNodes().size() + " nodes and " + edgesCreated + " edges.");
    }
}