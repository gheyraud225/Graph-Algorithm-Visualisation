import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.List; // Import List

/**
 * Implements a force-directed graph layout algorithm (simplified Fruchterman-Reingold).
 * Moves nodes iteratively based on spring-like edge forces and repulsive node forces.
 */
public class ForceDirectedLayout {

    private Graph graph;
    private GraphPanel panel; // To get dimensions

    // Simulation parameters (tune these!)
    private double stiffness = 100.0;   // Spring constant for edges (lower = weaker springs)
    private double repulsion = 15000.0; // Repulsion strength between nodes (higher = more spread out)
    private double damping = 0.7;       // Slows down movement over time (0 to 1)
    private double timeStep = 0.5;      // Simulation step size (larger = faster but less stable)
    private double threshold = 0.5;     // Movement threshold to stop simulation

    // Node state for simulation
    private Map<Node, Point2D.Double> velocities;
    private Map<Node, Point2D.Double> forces;

    private boolean running = false;
    private int maxIterations = 500; // Safety break
    private int currentIteration = 0;

    public ForceDirectedLayout(Graph graph, GraphPanel panel) {
        this.graph = graph;
        this.panel = panel;
        this.velocities = new HashMap<>();
        this.forces = new HashMap<>();
    }

    /** Initializes or resets the simulation state. */
    public void start() {
        velocities.clear();
        forces.clear();
        for (Node node : graph.getNodes()) {
            velocities.put(node, new Point2D.Double(0, 0));
            forces.put(node, new Point2D.Double(0, 0));
        }
        running = true;
        currentIteration = 0;
        System.out.println("Starting force-directed layout...");
    }

    /** Stops the simulation. */
    public void stop() {
        running = false;
        System.out.println("Layout stopped after " + currentIteration + " iterations.");
    }

    public boolean isRunning() {
        return running;
    }

    /** Performs one step of the physics simulation. */
    public boolean step() {
        if (!running || currentIteration >= maxIterations) {
            running = false;
            return false;
        }
        currentIteration++;

        // --- 1. Calculate Repulsive Forces ---
        // Every node repels every other node
        List<Node> nodes = graph.getNodes(); // Get a snapshot
        for (int i = 0; i < nodes.size(); i++) {
            Node u = nodes.get(i);
            // Reset force for this iteration
            forces.put(u, new Point2D.Double(0, 0));

            for (int j = i + 1; j < nodes.size(); j++) {
                Node v = nodes.get(j);
                double dx = u.getX() - v.getX();
                double dy = u.getY() - v.getY();
                double distanceSq = dx * dx + dy * dy;

                if (distanceSq < 1.0) distanceSq = 1.0; // Avoid division by zero

                double distance = Math.sqrt(distanceSq);
                double forceMagnitude = repulsion / distanceSq;

                // Calculate force components
                double fx = forceMagnitude * dx / distance;
                double fy = forceMagnitude * dy / distance;

                // Apply force to both nodes (opposite directions)
                Point2D.Double forceU = forces.get(u);
                Point2D.Double forceV = forces.get(v);
                forceU.x += fx;
                forceU.y += fy;
                forceV.x -= fx;
                forceV.y -= fy;
            }
        }

        // --- 2. Calculate Attractive Forces (Springs) ---
        // Edges pull connected nodes together
        for (Edge edge : graph.getEdges()) {
            Node u = edge.getSource();
            Node v = edge.getTarget();

            double dx = v.getX() - u.getX();
            double dy = v.getY() - u.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Ideal distance is somewhat arbitrary, let's use a base value
            double idealDistance = 200.0;
            double displacement = distance - idealDistance;
            // NOUVEAU: La force dépend du poids de l'arête
            double weightFactor = Math.max(1.0, edge.getWeight()); // Utilise le poids (minimum 1)
            double forceMagnitude = (stiffness * weightFactor / 10.0) * displacement / 1000.0; // Ajuste la force avec le poids

            if (distance > 1.0) { // Avoid issues if nodes overlap
                double fx = forceMagnitude * dx / distance;
                double fy = forceMagnitude * dy / distance;

                // Apply force to both nodes
                forces.get(u).x += fx;
                forces.get(u).y += fy;
                forces.get(v).x -= fx;
                forces.get(v).y -= fy;
            }
        }

        // --- 3. Update Velocities and Positions ---
        double totalMovement = 0;
        Dimension panelSize = panel.getSize(); // Get current panel size

        for (Node node : nodes) {
            Point2D.Double velocity = velocities.get(node);
            Point2D.Double force = forces.get(node);

            // Update velocity: v = (v + force * dt) * damping
            velocity.x = (velocity.x + force.x * timeStep) * damping;
            velocity.y = (velocity.y + force.y * timeStep) * damping;

            // Update position: p = p + v * dt
            double newX = node.getX() + velocity.x * timeStep;
            double newY = node.getY() + velocity.y * timeStep;

            // Keep nodes within bounds (simple clamping)
            int margin = 30;
            newX = Math.max(margin, Math.min(panelSize.width - margin, newX));
            newY = Math.max(margin, Math.min(panelSize.height - margin, newY));


            // Calculate movement distance for convergence check
            double moveDist = Math.sqrt(Math.pow(newX - node.getX(), 2) + Math.pow(newY - node.getY(), 2));
            totalMovement += moveDist;

            node.setX((int) newX);
            node.setY((int) newY);
        }

        // --- 4. Check for Convergence ---
        if (totalMovement < threshold * nodes.size()) {
            System.out.println("Layout converged after " + currentIteration + " iterations.");
            running = false;
            return false; // Stop
        }

        return true; // Continue
    }
}