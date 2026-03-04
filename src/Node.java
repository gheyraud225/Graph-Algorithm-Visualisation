import java.util.Objects;

// [MODIFIED] Implements Comparable based on fCost
public class Node implements Comparable<Node> {

    private String id;
    private int x;
    private int y;

    // --- A* Costs ---
    private double gCost = Double.POSITIVE_INFINITY; // Cost from start (Dijkstra's distance)
    private double hCost = 0;                        // Heuristic cost to end
    private double fCost = Double.POSITIVE_INFINITY; // f = g + h
    // ---

    private Node predecessor = null;

    // (Constructor unchanged)
    public Node(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    // --- Getters ---
    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public double getGCost() { return gCost; }
    public double getHCost() { return hCost; }
    public double getFCost() { return fCost; }
    public Node getPredecessor() { return predecessor; }
    // Backwards compatibility for other algorithms using getDistance()
    public double getDistance() { return gCost; }


    // --- Setters ---
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setGCost(double gCost) { this.gCost = gCost; }
    public void setHCost(double hCost) { this.hCost = hCost; }
    public void setFCost(double fCost) { this.fCost = fCost; }
    public void setPredecessor(Node predecessor) { this.predecessor = predecessor; }
    // Backwards compatibility
    public void setDistance(double distance) { this.gCost = distance; }


    // --- Reset ---
    public void reset() {
        this.gCost = Double.POSITIVE_INFINITY;
        this.hCost = 0;
        this.fCost = Double.POSITIVE_INFINITY;
        this.predecessor = null;
    }

    // --- Heuristic Calculation ---
    public double calculateHeuristic(Node target) {
        // Euclidean distance
        double dx = this.x - target.getX();
        double dy = this.y - target.getY();
        return Math.sqrt(dx * dx + dy * dy);
        // Alternative: Manhattan distance (good for grids)
        // return Math.abs(dx) + Math.abs(dy);
    }

    // --- Comparable based on fCost ---
    @Override
    public int compareTo(Node other) {
        return Double.compare(this.fCost, other.fCost);
    }

    // (equals, hashCode, toString unchanged)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return id.equals(node.id);
    }
    @Override
    public int hashCode() { return Objects.hash(id); }
    @Override
    public String toString() { return "Node(" + id + ")"; }
}