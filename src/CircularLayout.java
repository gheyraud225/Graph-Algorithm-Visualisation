import java.awt.Dimension;
import java.util.List;

/**
 * Arranges graph nodes in a circle.
 */
public class CircularLayout {

    public void apply(Graph graph, GraphPanel panel) {
        List<Node> nodes = graph.getNodes();
        int n = nodes.size();
        if (n == 0) return;

        Dimension panelSize = panel.getSize();
        int centerX = panelSize.width / 2;
        int centerY = panelSize.height / 2;

        // Calculate radius based on panel size, leave margin
        int margin = 50;
        int radius = Math.min(centerX, centerY) - margin;
        if (radius <= 0) radius = Math.min(centerX, centerY) / 2; // Fallback for small panels

        double angleStep = 2 * Math.PI / n;

        for (int i = 0; i < n; i++) {
            Node node = nodes.get(i);
            double angle = i * angleStep;
            int x = centerX + (int) (radius * Math.cos(angle));
            int y = centerY + (int) (radius * Math.sin(angle));
            node.setX(x);
            node.setY(y);
        }
    }
}