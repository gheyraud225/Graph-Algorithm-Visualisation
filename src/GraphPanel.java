import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform; // For zoom/pan
import java.awt.geom.Line2D; // For edge clicking
import java.awt.geom.Point2D; // For coordinate conversion
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter; // Handles both clicks and drags
import java.util.Map;
import java.util.Set;
import java.util.HashSet; // Needed for default rejectedEdges

/**
 * Panneau de dessin principal.
 * Gère Zoom/Pan, graphes orientés/non orientés, édition, animations,
 * et affichage des arêtes inversées pour layout hiérarchique.
 */
public class GraphPanel extends JPanel {

    // --- Champs ---
    private Graph graph; // Knows if it's directed
    private static final int NODE_DIAMETER = 30;
    private JToggleButton editModeButton;
    private AlgorithmVisualizer visualizer = null;
    private Node selectedStartNode = null;
    private Node selectedEndNode = null;
    private static int nodeLetter = 'A';
    private Node nodeToDrag = null;          // Node being DRAGGED
    private Node edgeCreationStartNode = null; // Node selected for edge creation (Click-Click)
    private Point dragOffset = null;        // Offset for smooth node dragging
    private Edge lastAnimatedEdge = null;
    private long sparkStartTime = 0;
    private final int SPARK_DURATION_MS = 500;
    private final int PULSE_AMOUNT = 6;
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private Point lastPanPoint = null;
    private final double MIN_ZOOM = 0.1;
    private final double MAX_ZOOM = 5.0;
    private final double ZOOM_SENSITIVITY = 1.1;

    // --- Color Palette ---
    private final Color COLOR_NODE = Color.decode("#3498db");
    private final Color COLOR_VISITED = Color.decode("#aed6f1");
    private final Color COLOR_START = Color.decode("#2ecc71");
    private final Color COLOR_END = Color.decode("#e74c3c");
    private final Color COLOR_QUEUE = Color.decode("#a9dfbf");
    private final Color COLOR_CURRENT = Color.decode("#f1c40f");
    private final Color COLOR_SEARCH_TREE = Color.decode("#f5b041");
    private final Color COLOR_FINAL_PATH = Color.decode("#e67e22");
    private final Color COLOR_EDIT_SELECT = Color.decode("#9b59b6");
    private final Color COLOR_REJECTED_EDGE = Color.decode("#c0392b");
    private final Color COLOR_REVERSED_EDGE = Color.decode("#8e44ad"); // Purple for reversed

    // --- Strokes ---
    private final BasicStroke STROKE_NORMAL = new BasicStroke(2f);
    private final BasicStroke STROKE_FINAL_PATH = new BasicStroke(5f);
    private final BasicStroke STROKE_ACTIVE = new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
    private final BasicStroke STROKE_SEARCH_TREE = new BasicStroke(3f);
    private final BasicStroke STROKE_EDIT_SELECT = new BasicStroke(4f);
    private final BasicStroke STROKE_REJECTED = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
    private final BasicStroke STROKE_REVERSED = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0); // Dashed


    public GraphPanel(Graph graph, JToggleButton editModeButton) {
        this.graph = graph;
        this.editModeButton = editModeButton;
        setPreferredSize(new Dimension(1000, 700));
        setBackground(Color.WHITE);

        MouseInputAdapter mouseAdapter = new MouseInputAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                // System.out.println("--- mouseClicked ---"); // DEBUG
                if (visualizer != null && !visualizer.isFinished()) return;
                boolean isEditMode = editModeButton != null && editModeButton.isSelected();
                Point graphPoint = screenToGraphCoords(e.getPoint());
                Node clickedNode = getNodeAtPoint(graphPoint);
                // System.out.println(" EditMode=" + isEditMode + ", ClickedNode=" + (clickedNode != null ? clickedNode.getId() : "null") + ", EdgeStartNode (Avant)=" + (edgeCreationStartNode != null ? edgeCreationStartNode.getId() : "null")); // DEBUG

                // --- EDIT MODE ---
                if (isEditMode) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (clickedNode != null) { // Clicked on existing node
                            if (edgeCreationStartNode == null) {
                                // System.out.println(" -> Select Edge Start: " + clickedNode.getId()); // DEBUG
                                edgeCreationStartNode = clickedNode;
                            } else if (edgeCreationStartNode == clickedNode) {
                                // System.out.println(" -> Deselect Edge Start: " + clickedNode.getId()); // DEBUG
                                edgeCreationStartNode = null; // Click same node again to deselect
                            } else {
                                // System.out.println(" -> Create Edge: " + edgeCreationStartNode.getId() + " -> " + clickedNode.getId()); // DEBUG
                                askAndCreateEdge(edgeCreationStartNode, clickedNode); // Click second node to create
                                edgeCreationStartNode = null; // Reset after creation
                            }
                        } else { // Clicked on empty space
                            if (edgeCreationStartNode != null) {
                                // System.out.println(" -> Deselect Edge Start (empty click)"); // DEBUG
                                edgeCreationStartNode = null; // Deselect if clicked empty space
                            } else {
                                // System.out.println(" -> Add Node"); // DEBUG
                                graph.addNode(new Node(String.valueOf((char) nodeLetter++), graphPoint.x, graphPoint.y)); // Add new node
                            }
                        }
                        // System.out.println(" EdgeStartNode (After Left Edit Click)=" + (edgeCreationStartNode != null ? edgeCreationStartNode.getId() : "null")); // DEBUG
                        repaint();
                    }
                    // Right Click: Delete Node or Edge
                    else if (SwingUtilities.isRightMouseButton(e)) {
                        if (clickedNode != null) { // Delete Node
                            graph.removeNode(clickedNode);
                            if (clickedNode == selectedStartNode || clickedNode == selectedEndNode) reset(); // Reset algo selection if needed
                            if (clickedNode == edgeCreationStartNode) edgeCreationStartNode = null; // Reset edge creation
                        } else { // Delete Edge
                            Edge clickedEdge = getEdgeAtPoint(graphPoint);
                            if (clickedEdge != null) {
                                graph.removeEdge(clickedEdge); // Use the method that handles directed/undirected
                            }
                        }
                        repaint();
                    }
                }
                // --- ALGORITHM MODE ---
                else {
                    // Left Click: Select Start/End nodes
                    if (SwingUtilities.isLeftMouseButton(e) && clickedNode != null) {
                        if (selectedStartNode == null) selectedStartNode = clickedNode;
                        else if (selectedEndNode == null && !clickedNode.equals(selectedStartNode)) selectedEndNode = clickedNode;
                        else { selectedStartNode = clickedNode; selectedEndNode = null; }
                        repaint();
                    }
                    // Right Click: Reset algo selection
                    else if (SwingUtilities.isRightMouseButton(e)) {
                        reset();
                    }
                }
                // System.out.println("--- End mouseClicked ---"); // DEBUG
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // System.out.println("--- mousePressed ---"); // DEBUG
                if (visualizer != null && !visualizer.isFinished()) return;
                boolean isEditMode = editModeButton != null && editModeButton.isSelected();
                Point graphPoint = screenToGraphCoords(e.getPoint());
                Node pressedNode = getNodeAtPoint(graphPoint);
                // System.out.println(" EditMode=" + isEditMode + ", PressedNode=" + (pressedNode != null ? pressedNode.getId() : "null")); // DEBUG

                // Start Node Drag (Edit Mode, Left Click on Node)
                if (isEditMode && SwingUtilities.isLeftMouseButton(e) && pressedNode != null) {
                    // System.out.println(" -> Prepare Node Drag: " + pressedNode.getId()); // DEBUG
                    nodeToDrag = pressedNode;
                    dragOffset = new Point(graphPoint.x - pressedNode.getX(), graphPoint.y - pressedNode.getY());
                    // Cancel edge creation if starting a drag on the selected node
                    if(edgeCreationStartNode == nodeToDrag) {
                        edgeCreationStartNode = null;
                        repaint(); // Update visual
                    }
                }
                // Start Pan (Right Click outside Edit Mode OR Middle Click)
                else if ((!isEditMode && SwingUtilities.isRightMouseButton(e)) || SwingUtilities.isMiddleMouseButton(e)) {
                    // System.out.println(" -> Prepare Pan"); // DEBUG
                    lastPanPoint = e.getPoint(); // Screen coords
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                // System.out.println("--- End mousePressed ---"); // DEBUG
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // Node Drag
                if (nodeToDrag != null) {
                    // Dragging a node always cancels edge creation intent
                    if(edgeCreationStartNode != null) edgeCreationStartNode = null;

                    Point graphPoint = screenToGraphCoords(e.getPoint());
                    nodeToDrag.setX(graphPoint.x - (dragOffset != null ? dragOffset.x : 0));
                    nodeToDrag.setY(graphPoint.y - (dragOffset != null ? dragOffset.y : 0));
                    repaint();
                }
                // Pan
                else if (lastPanPoint != null) {
                    int dx = e.getX() - lastPanPoint.x;
                    int dy = e.getY() - lastPanPoint.y;
                    panX += dx;
                    panY += dy;
                    lastPanPoint = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // System.out.println("--- mouseReleased ---"); // DEBUG
                // End Node Drag
                if (nodeToDrag != null) {
                    // System.out.println(" -> End Node Drag"); // DEBUG
                    nodeToDrag = null;
                    dragOffset = null;
                    repaint(); // Ensure final state is drawn
                }
                // End Pan
                if (lastPanPoint != null) {
                    // System.out.println(" -> End Pan"); // DEBUG
                    lastPanPoint = null;
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
                // System.out.println("--- End mouseReleased ---"); // DEBUG
            }
        }; // End MouseInputAdapter

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);

        // --- MouseWheelListener for Zoom ---
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (visualizer != null && !visualizer.isFinished()) return; // No zoom during visualization
                int rotation = e.getWheelRotation();
                Point zoomCenterScreen = e.getPoint();
                Point zoomCenterGraphBefore = screenToGraphCoords(zoomCenterScreen); // Graph point under mouse BEFORE zoom

                double scale = (rotation < 0) ? ZOOM_SENSITIVITY : 1.0 / ZOOM_SENSITIVITY; // Zoom in or out
                double oldZoomFactor = zoomFactor;
                zoomFactor *= scale;
                zoomFactor = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomFactor)); // Clamp zoom level

                if (zoomFactor != oldZoomFactor) {
                    // Recalculate pan offset to keep the point under the mouse stationary
                    panX = zoomCenterScreen.x - zoomCenterGraphBefore.x * zoomFactor;
                    panY = zoomCenterScreen.y - zoomCenterGraphBefore.y * zoomFactor;
                    repaint();
                }
            }
        });
    } // End Constructor

    // --- Coordinate Conversion Methods ---
    public Point screenToGraphCoords(Point screenPoint) {
        int graphX = (int) ((screenPoint.x - panX) / zoomFactor);
        int graphY = (int) ((screenPoint.y - panY) / zoomFactor);
        return new Point(graphX, graphY);
    }
    public Point graphToScreenCoords(Point graphPoint) {
        int screenX = (int) (graphPoint.x * zoomFactor + panX);
        int screenY = (int) (graphPoint.y * zoomFactor + panY);
        return new Point(screenX, screenY);
    }
    // Convenience overload for Node coordinates
    public Point graphToScreenCoords(Node node) {
        return graphToScreenCoords(new Point(node.getX(), node.getY()));
    }


    // --- Utility Methods ---
    public static void setNextNodeLetter(char letter) { nodeLetter = (int) letter; }
    public static char getNextNodeLetter() { return (char) nodeLetter; }
    public static void resetNodeLetter() { nodeLetter = 'A'; }

    /** Creates/updates edge based on graph type (directed/undirected). */
    private void askAndCreateEdge(Node source, Node target) {
        // Determine message and default weight based on existing edge (if any)
        String arrow = graph.isDirected() ? " -> " : " - ";
        String message = "Poids pour " + source.getId() + arrow + target.getId() + " :";
        Edge existingEdge = graph.findEdge(source, target); // Always check directed source->target
        String defaultWeight = (existingEdge != null) ? String.valueOf(existingEdge.getWeight()) : "1";

        String weightStr = (String) JOptionPane.showInputDialog(
                this, message, "Modifier/Créer une arête", JOptionPane.PLAIN_MESSAGE,
                null, null, defaultWeight );

        if (weightStr == null || weightStr.trim().isEmpty()) { return; } // User cancelled or entered nothing

        try {
            int weight = Integer.parseInt(weightStr.trim());
            if (weight > 0) {
                // Graph.addOrUpdateEdge handles directed vs undirected logic
                graph.addOrUpdateEdge(source, target, weight);
                repaint();
            } else {
                JOptionPane.showMessageDialog(this, "Le poids doit être positif.", "Erreur", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Veuillez entrer un nombre valide pour le poids.", "Erreur", JOptionPane.WARNING_MESSAGE);
            System.err.println("Entrée invalide : " + weightStr);
        }
    }

    /** Finds edge near a graph point, adjusted for zoom. */
    private Edge getEdgeAtPoint(Point graphPoint) {
        final double CLICK_THRESHOLD = 5.0 / zoomFactor; // Click tolerance scales with zoom
        Edge closestEdge = null;
        double minDistanceSq = CLICK_THRESHOLD * CLICK_THRESHOLD;

        for (Edge edge : graph.getEdges()) {
            Node source = edge.getSource(); Node target = edge.getTarget();
            // Use graph coordinates for distance calculation
            double distanceSq = Line2D.ptSegDistSq( // Use squared distance for efficiency
                    source.getX(), source.getY(), target.getX(), target.getY(),
                    graphPoint.x, graphPoint.y );

            if (distanceSq < minDistanceSq) {
                minDistanceSq = distanceSq;
                closestEdge = edge;
            }
        }
        // If undirected, we might have found B->A when user clicked near A->B.
        // Return the one that makes sense or consistently one? Let's just return what we found.
        return closestEdge;
    }

    public void setVisualizer(AlgorithmVisualizer visualizer) { this.visualizer = visualizer; }

    /** Resets visualization and selection states. */
    public void reset() {
        selectedStartNode = null; selectedEndNode = null;
        visualizer = null; edgeCreationStartNode = null;
        nodeToDrag = null; dragOffset = null;
        lastPanPoint = null; if(getCursor().getType() != Cursor.DEFAULT_CURSOR) setCursor(Cursor.getDefaultCursor());
        lastAnimatedEdge = null;
        // Don't reset zoom/pan on standard reset
        repaint();
    }

    /** Finds node near a graph point, using fixed radius in graph coords. */
    private Node getNodeAtPoint(Point graphPoint) {
        double clickRadiusSq = (NODE_DIAMETER / 2.0) * (NODE_DIAMETER / 2.0); // Use squared radius
        Node closestNode = null;
        double minDistanceSq = Double.POSITIVE_INFINITY; // Find the absolute closest

        for (Node node : graph.getNodes()) {
            double dx = graphPoint.x - node.getX();
            double dy = graphPoint.y - node.getY();
            double distanceSq = dx * dx + dy * dy;

            if (distanceSq < minDistanceSq) {
                minDistanceSq = distanceSq;
                closestNode = node;
            }
        }
        // Return node only if it's within the clickable radius
        return (minDistanceSq <= clickRadiusSq) ? closestNode : null;
    }


    // --- Methods for CombatFrame ---
    public void setSelectedStartNodeManually(Node node) {
        // Ensure node belongs to *this* graph instance before setting
        if (node != null && this.graph != null && this.graph.getNodes().contains(node)) {
            this.selectedStartNode = node; repaint();
        } else if (node != null && this.graph != null) {
            // Try finding by ID if the object reference is different (e.g., from a copied graph)
            Node foundNode = this.graph.findNodeById(node.getId());
            if (foundNode != null) {
                this.selectedStartNode = foundNode; repaint();
            }
        }
    }
    public void setSelectedEndNodeManually(Node node) {
        if (node != null && this.graph != null && this.graph.getNodes().contains(node)) {
            this.selectedEndNode = node; repaint();
        } else if (node != null && this.graph != null) {
            Node foundNode = this.graph.findNodeById(node.getId());
            if (foundNode != null) {
                this.selectedEndNode = foundNode; repaint();
            }
        }
    }

    /** Draws centered text at graph coordinates x, y. */
    private void drawCenteredString(Graphics g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        // Calculate position to center the text
        int textX = x - textWidth / 2;
        int textY = y + (fm.getAscent() - fm.getDescent()) / 2; // Centers vertically
        g.drawString(text, textX, textY);
    }

    public Node getSelectedStartNode() { return selectedStartNode; }
    public Node getSelectedEndNode() { return selectedEndNode; }


    // ==========================================================
    // --- MAIN PAINT METHOD (Handles Transformations & Drawing) ---
    // ==========================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Save the original transformation
        AffineTransform savedTransform = g2d.getTransform();

        // Apply Pan and Zoom transformations
        g2d.translate(panX, panY);
        g2d.scale(zoomFactor, zoomFactor);
        // --- ALL DRAWING BELOW USES GRAPH COORDINATES ---

        // --- Get current visualization state ---
        Set<Edge> searchTreeEdges = null; Set<Edge> pathEdges = null;
        Node currentNode = null; Edge currentEdge = null;
        Set<Node> visitedNodes = null; Set<Node> nodesInQueue = null;
        Set<Edge> rejectedEdges = null; boolean isFinished = false;
        // Get reversed edges from Main (only relevant after Hierarchical layout)
        Set<Edge> reversedEdges = (Main.hierarchicalLayout != null) ? Main.hierarchicalLayout.getReversedEdges() : new HashSet<>();


        if (visualizer != null) {
            searchTreeEdges = visualizer.getSearchTreeEdges(); pathEdges = visualizer.getPathEdges();
            currentNode = visualizer.getCurrentNode(); currentEdge = visualizer.getCurrentEdge();
            visitedNodes = visualizer.getVisitedNodes(); nodesInQueue = visualizer.getNodesInQueue();
            rejectedEdges = visualizer.getRejectedEdges();
            isFinished = visualizer.isFinished();
            // Update spark animation trigger
            if (currentEdge != null && currentEdge != lastAnimatedEdge) {
                lastAnimatedEdge = currentEdge; sparkStartTime = System.currentTimeMillis();
            }
        } else {
            lastAnimatedEdge = null; // No animation if no visualizer
        }

        // --- 1. Draw Edges ---
        // Calculate stroke widths scaled by zoom factor
        float scaledStrokeWidth = Math.max(0.5f, STROKE_NORMAL.getLineWidth() / (float)zoomFactor);
        BasicStroke scaledNormalStroke = new BasicStroke(scaledStrokeWidth);
        BasicStroke scaledFinalPathStroke = new BasicStroke(Math.max(1f, STROKE_FINAL_PATH.getLineWidth() / (float)zoomFactor));
        BasicStroke scaledSearchTreeStroke = new BasicStroke(Math.max(1f, STROKE_SEARCH_TREE.getLineWidth() / (float)zoomFactor));
        float dashPhase = 0f;
        float dash[] = {Math.max(1f, 9f / (float)zoomFactor)}; // Scale dash length
        BasicStroke scaledActiveStroke = new BasicStroke(Math.max(1f, STROKE_ACTIVE.getLineWidth() / (float)zoomFactor), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dash, dashPhase);
        float rejectedDash[] = {Math.max(1f, 5f / (float)zoomFactor)}; // Scale dash length
        BasicStroke scaledRejectedStroke = new BasicStroke(Math.max(0.5f, STROKE_REJECTED.getLineWidth() / (float)zoomFactor), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, rejectedDash, dashPhase);
        float reversedDash[] = {Math.max(1f, 4f / (float)zoomFactor), Math.max(1f, 4f / (float)zoomFactor)}; // Scale dash length
        BasicStroke scaledReversedStroke = new BasicStroke(Math.max(0.5f, STROKE_REVERSED.getLineWidth() / (float)zoomFactor), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, reversedDash, dashPhase);


        for (Edge edge : graph.getEdges()) {
            Color edgeColor = Color.LIGHT_GRAY; // Default color
            BasicStroke edgeStroke = scaledNormalStroke; // Default stroke
            boolean isReversed = reversedEdges.contains(edge); // Check if edge was reversed by layout

            // Determine color and stroke based on algorithm state AND reversed status (priority order matters)
            if (isReversed) { // Highest priority: Show reversed edges clearly
                edgeColor = COLOR_REVERSED_EDGE; edgeStroke = scaledReversedStroke;
            }
            else if (edge.equals(currentEdge) && !isFinished) { // Active edge during algo
                edgeColor = COLOR_CURRENT; edgeStroke = scaledActiveStroke;
            }
            else if (isFinished && pathEdges != null && !pathEdges.isEmpty() && pathEdges.contains(edge)) { // Final path
                edgeColor = COLOR_FINAL_PATH; edgeStroke = scaledFinalPathStroke;
            }
            else if (searchTreeEdges != null && searchTreeEdges.contains(edge)) { // Part of search tree or MST
                if (isFinished && (pathEdges == null || pathEdges.isEmpty())) { // MST final result
                    edgeColor = COLOR_FINAL_PATH; edgeStroke = scaledFinalPathStroke;
                } else if (!isFinished) { // Search tree in progress
                    edgeColor = COLOR_SEARCH_TREE; edgeStroke = scaledSearchTreeStroke;
                }
            }
            else if (rejectedEdges != null && rejectedEdges.contains(edge)) { // Rejected edge (Kruskal)
                edgeColor = COLOR_REJECTED_EDGE; edgeStroke = scaledRejectedStroke;
            }
            // else: Use default gray / normal stroke

            g2d.setColor(edgeColor);
            g2d.setStroke(edgeStroke);

            Node source = edge.getSource(); Node target = edge.getTarget();
            int x1 = source.getX(); int y1 = source.getY();
            int x2 = target.getX(); int y2 = target.getY();

            g2d.drawLine(x1, y1, x2, y2);

            // --- Draw Arrowhead if directed ---
            // Calculate node radius scaled for zoom (used for offset)
            double scaledRadius = Math.max(2, NODE_DIAMETER / (2.0 * zoomFactor));
            if (graph.isDirected()) {
                drawArrowHead(g2d, x1, y1, x2, y2, scaledRadius, isReversed); // Pass isReversed
            } else if (!graph.isDirected() && isReversed){
                // Optional: Draw something on undirected reversed edges? Maybe not needed.
            }


            // --- Draw Edge Weight (once per pair if undirected) ---
            boolean shouldDrawWeight = graph.isDirected() || (source.getId().compareTo(target.getId()) < 0);
            if (shouldDrawWeight) {
                String weight = String.valueOf(edge.getWeight());
                int midX = (x1 + x2) / 2; int midY = (y1 + y2) / 2;
                double dx = x2 - x1; double dy = y2 - y1;
                if (dx != 0 || dy != 0) { // Avoid division by zero
                    double angle = Math.atan2(dy, dx);
                    int scaledOffsetAmount = (int)Math.max(3, 10 / zoomFactor); // Offset scaled
                    int offsetX = (int) (scaledOffsetAmount * Math.sin(angle));
                    int offsetY = (int) (-scaledOffsetAmount * Math.cos(angle)); // Y is inverted

                    Font originalFont = g2d.getFont();
                    Font scaledFont = originalFont.deriveFont(Math.max(8f, originalFont.getSize2D() / (float)zoomFactor)); // Font scales
                    g2d.setFont(scaledFont);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(weight);
                    int textHeight = fm.getAscent();

                    // Draw white background for clarity
                    g2d.setColor(new Color(255, 255, 255, 180)); // White semi-transparent
                    g2d.fillRect(midX + offsetX - textWidth / 2 - 1, midY + offsetY - textHeight + fm.getDescent() -1 , textWidth + 2, textHeight +2 );

                    g2d.setColor(Color.BLACK);
                    g2d.drawString(weight, midX + offsetX - textWidth / 2, midY + offsetY);
                    g2d.setFont(originalFont); // Restore font
                }
            }
        } // End edge loop

        // --- 1b. Draw Spark Animation ---
        if (lastAnimatedEdge != null && !isFinished && visualizer != null) {
            long elapsedTime = System.currentTimeMillis() - sparkStartTime;
            int actualSparkDuration = SPARK_DURATION_MS; // Use fixed duration for spark
            // Adjust duration based on timer delay only if timer delay is shorter
            if (visualizer.getLogicTimerDelay() > 0 && visualizer.getLogicTimerDelay() - 50 < SPARK_DURATION_MS) {
                actualSparkDuration = Math.max(50, visualizer.getLogicTimerDelay() - 50); // Ensure minimum duration
            }

            if (elapsedTime < actualSparkDuration) {
                double progress = Math.min((double) elapsedTime / actualSparkDuration, 1.0);
                Node source = lastAnimatedEdge.getSource(); Node target = lastAnimatedEdge.getTarget();
                // Calculate position using graph coordinates
                int sparkX = (int) (source.getX() + (target.getX() - source.getX()) * progress);
                int sparkY = (int) (source.getY() + (target.getY() - source.getY()) * progress);
                // Scale spark size with zoom
                int haloSize = (int)Math.max(4, 12 / zoomFactor);
                int coreSize = (int)Math.max(3, 8 / zoomFactor);
                g2d.setColor(new Color(255, 255, 255, 200)); // White halo
                g2d.fillOval(sparkX - haloSize/2, sparkY - haloSize/2, haloSize, haloSize);
                g2d.setColor(COLOR_CURRENT); // Yellow core
                g2d.fillOval(sparkX - coreSize/2, sparkY - coreSize/2, coreSize, coreSize);
            } else {
                lastAnimatedEdge = null; // Animation finished for this edge
            }
        }

        // --- 2. Draw Nodes ---
        for (Node node : graph.getNodes()) {
            // Node size scales with zoom
            int scaledDiameter = (int) Math.max(5, NODE_DIAMETER / zoomFactor); // Min size 5px
            int scaledPulseAmount = (int) Math.max(1, PULSE_AMOUNT / zoomFactor); // Pulse scales

            // Apply pulse if current node
            if (node.equals(currentNode) && !isFinished) {
                double pulse = (Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0; // Sin wave 0..1
                scaledDiameter += (int) (pulse * scaledPulseAmount);
            }

            int radius = scaledDiameter / 2;
            // Use graph coordinates for node center
            int nodeCenterX = node.getX();
            int nodeCenterY = node.getY();
            int topLeftX = nodeCenterX - radius;
            int topLeftY = nodeCenterY - radius;

            // Determine node color
            Color nodeColor = COLOR_NODE; Color textColor = Color.WHITE;
            if (visualizer != null) {
                if (nodesInQueue != null && nodesInQueue.contains(node)) { nodeColor = COLOR_QUEUE; textColor = Color.BLACK; }
                if (visitedNodes != null && visitedNodes.contains(node)) { nodeColor = COLOR_VISITED; textColor = Color.BLACK; } // Visited is light blue now
                if (node.equals(currentNode)) { nodeColor = COLOR_CURRENT; textColor = Color.BLACK; }
            }
            if (node.equals(selectedStartNode)) { nodeColor = COLOR_START; textColor = Color.WHITE; }
            if (node.equals(selectedEndNode)) { nodeColor = COLOR_END; textColor = Color.WHITE; }

            // Draw node fill
            g2d.setColor(nodeColor);
            g2d.fillOval(topLeftX, topLeftY, scaledDiameter, scaledDiameter);

            // Draw node border (scaled)
            BasicStroke scaledEditStroke = new BasicStroke(Math.max(1f, STROKE_EDIT_SELECT.getLineWidth() / (float)zoomFactor));
            int borderOffset = (int)Math.max(1, 2 / zoomFactor); // Offset scales
            if (node.equals(edgeCreationStartNode)) { // Edit selection highlight
                g2d.setColor(COLOR_EDIT_SELECT);
                g2d.setStroke(scaledEditStroke);
                g2d.drawOval(topLeftX - borderOffset, topLeftY - borderOffset, scaledDiameter + 2*borderOffset, scaledDiameter + 2*borderOffset);
            } else { // Normal border
                g2d.setColor(Color.BLACK);
                g2d.setStroke(scaledNormalStroke); // Use scaled normal stroke
                g2d.drawOval(topLeftX, topLeftY, scaledDiameter, scaledDiameter);
            }

            // Draw Text (ID and Costs), font scales with zoom
            Font originalFont = g2d.getFont();
            Font scaledFont = originalFont.deriveFont(Math.max(8f, originalFont.getSize2D() / (float)zoomFactor)); // Min font size 8pt
            g2d.setFont(scaledFont);
            FontMetrics fm = g2d.getFontMetrics(); // Use metrics from scaled font

            // Draw Node ID
            g2d.setColor(textColor);
            drawCenteredString(g2d, node.getId(), nodeCenterX, nodeCenterY); // Use center coords

            // Draw Costs (A* or others)
            // Use getDistance() which maps to gCost now
            if (visualizer != null && node.getDistance() < Double.POSITIVE_INFINITY) {
                int textHeightOffset = fm.getAscent();
                // Offset below scaled node radius
                int distOffsetY = radius + (int)Math.max(3, 5 / zoomFactor);

                // A* specific costs (g, h, f)
                if (visualizer instanceof AStarVisualizer && node.getFCost() < Double.POSITIVE_INFINITY) {
                    String gStr = String.format("g=%.0f", node.getGCost());
                    String hStr = String.format("h=%.0f", node.getHCost());
                    String fStr = String.format("f=%.0f", node.getFCost());
                    int gWidth = fm.stringWidth(gStr); int hWidth = fm.stringWidth(hStr); int fWidth = fm.stringWidth(fStr);
                    // Offset from scaled border
                    int sideOffset = radius + borderOffset + (int)Math.max(1, 2 / zoomFactor);

                    // Draw g below
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawString(gStr, nodeCenterX - gWidth/2, nodeCenterY + distOffsetY);
                    // Draw h left
                    g2d.setColor(Color.BLUE.darker());
                    g2d.drawString(hStr, nodeCenterX - sideOffset - hWidth, nodeCenterY + fm.getAscent() / 2 - fm.getDescent()/2 );
                    // Draw f right
                    g2d.setColor(Color.RED.darker());
                    g2d.drawString(fStr, nodeCenterX + sideOffset, nodeCenterY + fm.getAscent() / 2 - fm.getDescent()/2);

                } else if (!(visualizer instanceof AStarVisualizer)) { // Other algorithms show distance/gCost below
                    String distStr = String.format("%.0f", node.getDistance());
                    int textWidth = fm.stringWidth(distStr);
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawString(distStr, nodeCenterX - textWidth/2, nodeCenterY + distOffsetY);
                }
            }
            g2d.setFont(originalFont); // Restore original font
        } // End node loop

        // Restore original transformation
        g2d.setTransform(savedTransform);
        // --- ALL DRAWING BELOW USES SCREEN COORDINATES ---

    } // End paintComponent


    /**
     * Utility method to draw an arrowhead.
     * @param g2d Graphics context
     * @param x1 Source X (graph coords)
     * @param y1 Source Y (graph coords)
     * @param x2 Target X (graph coords)
     * @param y2 Target Y (graph coords)
     * @param nodeRadius Scaled radius of the target node (for offsetting the arrow tip)
     * @param isReversed If true, draw the arrowhead differently (e.g., hollow)
     */
    private void drawArrowHead(Graphics2D g2d, int x1, int y1, int x2, int y2, double nodeRadius, boolean isReversed) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) return; // Avoid NaN
        double angle = Math.atan2(dy, dx);
        double len = Math.sqrt(dx*dx + dy*dy);

        // Don't draw if edge is too short (shorter than radius)
        if (len < nodeRadius * 1.1) return; // Add small buffer

        // Arrow size scales with zoom
        double arrowSize = Math.max(5, 15 / zoomFactor); // Min size 5px screen equiv
        double arrowAngle = Math.toRadians(20); // Angle of arrow wings

        // Calculate the point on the edge just outside the target node's radius
        // Use scaled radius for offset calculation
        double endX = x2 - nodeRadius * Math.cos(angle);
        double endY = y2 - nodeRadius * Math.sin(angle);

        // Calculate the points for the arrow wings relative to the end point
        Point2D p1 = new Point2D.Double(
                endX - arrowSize * Math.cos(angle + arrowAngle),
                endY - arrowSize * Math.sin(angle + arrowAngle)
        );
        Point2D p2 = new Point2D.Double(
                endX - arrowSize * Math.cos(angle - arrowAngle),
                endY - arrowSize * Math.sin(angle - arrowAngle)
        );

        // Create the polygon for the arrowhead
        Polygon arrowHead = new Polygon();
        arrowHead.addPoint((int) endX, (int) endY);
        arrowHead.addPoint((int) p1.getX(), (int) p1.getY());
        arrowHead.addPoint((int) p2.getX(), (int) p2.getY());

        // Use the current edge color (set before calling this method)
        Color previousColor = g2d.getColor(); // Remember color
        // If reversed, maybe use the reversed color? Or keep edge color? Let's keep edge color.
        // g2d.setColor(isReversed ? COLOR_REVERSED_EDGE : previousColor);

        if (isReversed) {
            // Optional: Make reversed arrows visually distinct (e.g., hollow)
            Stroke previousStroke = g2d.getStroke(); // Remember stroke
            g2d.setStroke(new BasicStroke(Math.max(0.5f, 1f / (float)zoomFactor))); // Thin outline stroke
            g2d.draw(arrowHead); // Draw outline only
            g2d.setStroke(previousStroke); // Restore stroke
        } else {
            g2d.fill(arrowHead); // Fill normally
        }
        // g2d.setColor(previousColor); // Restore color if changed
    }


} // End GraphPanel class