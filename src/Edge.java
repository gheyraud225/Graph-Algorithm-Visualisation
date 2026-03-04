public class Edge {
    private Node source;
    private Node target;
    private int weight;

    public Edge(Node source, Node target, int weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    // Getters
    public Node getSource() { return source; }
    public Node getTarget() { return target; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) {
        this.weight = weight;
    }
}