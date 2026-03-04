import java.awt.Dimension;
import java.util.List;
import java.util.Random;

/**
 * Arranges graph nodes randomly within the panel bounds.
 */
public class RandomLayout {

    private Random random = new Random();

    public void apply(Graph graph, GraphPanel panel) {
        List<Node> nodes = graph.getNodes();
        Dimension panelSize = panel.getSize();
        int margin = 30;
        int width = panelSize.width;
        int height = panelSize.height;

        // Ensure bounds are valid
        if (width <= 2 * margin || height <= 2 * margin) return;

        for (Node node : nodes) {
            int x = margin + random.nextInt(width - 2 * margin);
            int y = margin + random.nextInt(height - 2 * margin);
            node.setX(x);
            node.setY(y);
        }
    }
}