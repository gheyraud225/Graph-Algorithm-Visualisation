import java.awt.Dimension;
import java.util.*;

/**
 * Implements a Hierarchical (Sugiyama-style) layout.
 * Includes Cycle Removal (DFS), Layer Assignment (BFS),
 * and Crossing Reduction (Barycenter).
 */
public class HierarchicalLayout {

    private final int LAYER_SPACING = 100;
    private final int NODE_SPACING = 70;
    private final int CROSSING_REDUCTION_ITERATIONS = 8;

    // --- NEW: Store reversed edges ---
    private Set<Edge> reversedEdges = new HashSet<>();

    public void apply(Graph graph, GraphPanel panel) {
        if (graph.getNodes().isEmpty()) return;

        // --- Phase 0: Cycle Removal (NEW) ---
        // Work on a temporary list of edges to avoid modifying graph's main list during iteration
        List<Edge> originalEdges = new ArrayList<>(graph.getEdges());
        reversedEdges = removeCyclesDFS(graph, originalEdges);

        // --- Phase 1: Layer Assignment (using BFS from sources) ---
        // Now works even if the original graph had cycles, because we operate on the (conceptually) reversed edges
        Map<Node, Integer> nodeLayerMap = assignLayersBFS(graph); // This needs to respect the reversed edges conceptually
        if (nodeLayerMap.isEmpty()) {
            System.err.println("Hierarchical Layout: Layer assignment failed (unexpected error after cycle removal?). Falling back to random.");
            // Restore original edges before falling back? Or maybe the graph state is inconsistent now.
            // For simplicity, we just apply random layout to the potentially modified node positions.
            new RandomLayout().apply(graph, panel);
            // It might be better to restore the original edges before applying random
            // restoreReversedEdges(graph, reversedEdges); // Need to implement this if fallback required
            return;
        }

        // --- Organize nodes by layer ---
        int maxLayer = 0;
        // ... (Rest of layer organization is the same) ...
        for (int layer : nodeLayerMap.values()) { maxLayer = Math.max(maxLayer, layer); }
        List<List<Node>> layers = new ArrayList<>(maxLayer + 1);
        for (int i = 0; i <= maxLayer; i++) { layers.add(new ArrayList<>()); }
        Map<Node, Integer> nodePositionInLayer = new HashMap<>();
        for (Map.Entry<Node, Integer> entry : nodeLayerMap.entrySet()) {
            int layerIndex = entry.getValue();
            layers.get(layerIndex).add(entry.getKey());
            nodePositionInLayer.put(entry.getKey(), layers.get(layerIndex).size() - 1);
        }


        // --- Phase 2: Crossing Reduction (Barycenter - now uses reversed edges conceptually) ---
        applyBarycenterMethod(layers, nodePositionInLayer, graph);


        // --- Phase 3: Coordinate Assignment (Unchanged) ---
        assignCoordinates(layers, panel.getSize());

        // --- Phase 4: Restore Reversed Edges (Important!) ---
        // We modified the graph's internal edge list, restore it now
        // This step is complex if we actually modified the Graph object's edges.
        // A better approach is often to build a temporary DAG structure for layout calculations only.
        // For now, we'll assume removeCyclesDFS only *identified* edges to reverse,
        // and subsequent steps *pretend* they are reversed for calculation.
        // The actual drawing in GraphPanel will use the original edges and potentially
        // draw reversed ones differently.
        // Let's modify removeCyclesDFS to just return the set, not modify the graph.

    } // End apply


    /**
     * NEW: Detects cycles using DFS and returns the set of back edges found.
     * Does NOT modify the graph's edge list directly.
     */
    private Set<Edge> removeCyclesDFS(Graph graph, List<Edge> edgesToConsider) {
        Set<Edge> backEdges = new HashSet<>();
        Set<Node> visited = new HashSet<>(); // Nodes completely finished
        Set<Node> visiting = new HashSet<>(); // Nodes currently in the recursion stack

        for (Node node : graph.getNodes()) {
            if (!visited.contains(node)) {
                dfsVisit(node, visited, visiting, backEdges, graph);
            }
        }
        System.out.println("Cycle detection found " + backEdges.size() + " back edges.");
        return backEdges;
    }

    /** Recursive helper for DFS-based cycle detection */
    private void dfsVisit(Node u, Set<Node> visited, Set<Node> visiting, Set<Edge> backEdges, Graph graph) {
        visited.add(u);
        visiting.add(u); // Add to current path stack

        for (Edge edge : graph.getVoisins(u)) {
            // We only care about edges going OUT from u
            Node v = edge.getTarget();

            // If neighbor is currently being visited (in the stack), it's a back edge!
            if (visiting.contains(v)) {
                backEdges.add(edge); // Mark this edge for reversal (conceptually)
            }
            // If neighbor hasn't been visited at all, recurse
            else if (!visited.contains(v)) {
                dfsVisit(v, visited, visiting, backEdges, graph);
            }
            // If neighbor is in visited but not visiting, it's a cross or forward edge, ignore.
        }

        visiting.remove(u); // Remove from current path stack upon finishing
    }


    /**
     * Assigns layers using a BFS-like approach from source nodes.
     * IMPORTANT: Needs to conceptually respect the reversed edges.
     */
    private Map<Node, Integer> assignLayersBFS(Graph graph) {
        Map<Node, Integer> nodeLayer = new HashMap<>();
        Map<Node, Integer> inDegree = new HashMap<>(); // Calculate in-degree based on *non-reversed* edges
        Queue<Node> queue = new LinkedList<>();

        // Calculate in-degrees considering conceptual reversals
        for (Node node : graph.getNodes()) {
            inDegree.put(node, 0);
        }
        for (Edge edge : graph.getEdges()) {
            // If it's a reversed edge, it now conceptually goes target -> source
            if (reversedEdges.contains(edge)) {
                Node source = edge.getSource(); // The original source now acts as target for layering
                inDegree.put(source, inDegree.getOrDefault(source, 0) + 1);
            } else { // Normal edge source -> target
                Node target = edge.getTarget();
                inDegree.put(target, inDegree.getOrDefault(target, 0) + 1);
            }
        }

        // Add nodes with in-degree 0 to the queue (layer 0)
        for (Node node : graph.getNodes()) {
            if (inDegree.get(node) == 0) {
                queue.add(node);
                nodeLayer.put(node, 0);
            }
        }

        int processedNodes = 0;
        while (!queue.isEmpty()) {
            Node u = queue.poll();
            processedNodes++;
            int currentLayer = nodeLayer.get(u);

            // Iterate through conceptually FORWARD edges from u
            // 1. Original forward edges NOT reversed
            for (Edge edge : graph.getVoisins(u)) {
                if (!reversedEdges.contains(edge)) {
                    Node v = edge.getTarget();
                    processNeighborForLayering(v, u, currentLayer, nodeLayer, inDegree, queue);
                }
            }
            // 2. Original backward edges THAT WERE reversed (now conceptually u -> original_source)
            for (Edge edge : reversedEdges) { // Need efficient way to find reversed edges starting at u
                if (edge.getTarget().equals(u)) { // If u is the target of a reversed edge
                    Node originalSource = edge.getSource(); // This is the conceptual target now
                    processNeighborForLayering(originalSource, u, currentLayer, nodeLayer, inDegree, queue);
                }
            }
        }

        if (processedNodes != graph.getNodes().size()) {
            System.err.println("Layering error: Not all nodes processed ("+processedNodes+"/"+graph.getNodes().size()+"). Graph might still have issues or be disconnected.");
            return new HashMap<>();
        }

        return nodeLayer;
    }

    /** Helper for processing neighbors during layering BFS */
    private void processNeighborForLayering(Node v, Node u, int currentLayer, Map<Node, Integer> nodeLayer, Map<Node, Integer> inDegree, Queue<Node> queue) {
        nodeLayer.put(v, Math.max(nodeLayer.getOrDefault(v, -1), currentLayer + 1));
        // Decrease conceptual in-degree
        inDegree.put(v, inDegree.get(v) - 1);
        if (inDegree.get(v) == 0) {
            queue.add(v);
        }
    }


    /** Applies Barycenter method iteratively */
    private void applyBarycenterMethod(List<List<Node>> layers, Map<Node, Integer> nodePositionInLayer, Graph graph) {
        Map<Node, Double> barycenters = new HashMap<>();
        for (int iter = 0; iter < CROSSING_REDUCTION_ITERATIONS; iter++) {
            // Sweep Down
            for (int i = 1; i < layers.size(); i++) {
                calculateBarycenters(layers.get(i), layers.get(i - 1), nodePositionInLayer, graph, barycenters, true); // Use predecessors
                sortLayerByBarycenter(layers.get(i), barycenters, nodePositionInLayer);
            }
            // Sweep Up
            for (int i = layers.size() - 2; i >= 0; i--) {
                calculateBarycenters(layers.get(i), layers.get(i + 1), nodePositionInLayer, graph, barycenters, false); // Use successors
                sortLayerByBarycenter(layers.get(i), barycenters, nodePositionInLayer);
            }
        }
    }

    /** Calculates barycenter values, respecting reversed edges */
    private void calculateBarycenters(List<Node> currentLayer, List<Node> adjacentLayer, Map<Node, Integer> nodePositions, Graph graph, Map<Node, Double> barycenters, boolean usePredecessors) {
        barycenters.clear();
        for (Node u : currentLayer) {
            double sumPositions = 0;
            int countNeighbors = 0;
            // Find neighbors in the adjacent layer based on conceptual direction
            List<Node> neighbors = findNeighborsInLayer(u, adjacentLayer, graph, usePredecessors);

            if (!neighbors.isEmpty()) {
                for (Node v : neighbors) {
                    sumPositions += nodePositions.getOrDefault(v, 0);
                    countNeighbors++;
                }
                barycenters.put(u, sumPositions / countNeighbors);
            } else {
                barycenters.put(u, (double) nodePositions.getOrDefault(u, 0));
            }
        }
    }

    /** Helper to find neighbors, respecting reversed edges */
    private List<Node> findNeighborsInLayer(Node node, List<Node> targetLayer, Graph graph, boolean findPredecessors) {
        List<Node> neighbors = new ArrayList<>();
        Set<Node> targetLayerNodes = new HashSet<>(targetLayer);

        if (findPredecessors) { // Find nodes in targetLayer pointing TO node (conceptually)
            for(Node potentialPredecessor : targetLayerNodes){
                // Check original edge potentialPredecessor -> node (if not reversed)
                Edge originalEdge = graph.findEdge(potentialPredecessor, node);
                if(originalEdge != null && !reversedEdges.contains(originalEdge)){
                    neighbors.add(potentialPredecessor);
                }
                // Check reversed edge node -> potentialPredecessor (conceptually potentialPredecessor -> node)
                Edge reversedEdge = graph.findEdge(node, potentialPredecessor);
                if(reversedEdge != null && reversedEdges.contains(reversedEdge)){
                    neighbors.add(potentialPredecessor);
                }
            }
        } else { // Find nodes in targetLayer pointed FROM node (conceptually)
            for(Node potentialSuccessor : targetLayerNodes){
                // Check original edge node -> potentialSuccessor (if not reversed)
                Edge originalEdge = graph.findEdge(node, potentialSuccessor);
                if(originalEdge != null && !reversedEdges.contains(originalEdge)){
                    neighbors.add(potentialSuccessor);
                }
                // Check reversed edge potentialSuccessor -> node (conceptually node -> potentialSuccessor)
                Edge reversedEdge = graph.findEdge(potentialSuccessor, node);
                if(reversedEdge != null && reversedEdges.contains(reversedEdge)){
                    neighbors.add(potentialSuccessor);
                }
            }
        }
        return neighbors;
    }

    /** Sorts layer and updates positions */
    private void sortLayerByBarycenter(List<Node> layer, Map<Node, Double> barycenters, Map<Node, Integer> nodePositions) {
        layer.sort(Comparator.comparingDouble(node -> barycenters.getOrDefault(node, 0.0)));
        for (int i = 0; i < layer.size(); i++) {
            nodePositions.put(layer.get(i), i);
        }
    }

    /** Assigns final X, Y coordinates */
    private void assignCoordinates(List<List<Node>> layers, Dimension panelSize) {
        int currentY = 50;
        for (int i = 0; i < layers.size(); i++) {
            List<Node> currentLayerNodes = layers.get(i);
            int layerWidth = (currentLayerNodes.size() - 1) * NODE_SPACING;
            int startX = Math.max(30, (panelSize.width / 2) - (layerWidth / 2)); // Ensure margin
            for (int j = 0; j < currentLayerNodes.size(); j++) {
                Node node = currentLayerNodes.get(j);
                int x = startX + j * NODE_SPACING;
                node.setX(x);
                node.setY(currentY);
            }
            currentY += LAYER_SPACING;
        }
    }

    /** NEW: Getter for reversed edges (for GraphPanel to draw them differently) */
    public Set<Edge> getReversedEdges() {
        return reversedEdges;
    }

} // End HierarchicalLayout