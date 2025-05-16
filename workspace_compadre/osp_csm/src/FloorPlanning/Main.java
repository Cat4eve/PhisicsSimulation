package FloorPlanning;

import javax.swing.*;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Test Case 1: 3 small circles and 1 large
//            new CircleUI(Arrays.asList(10, 15, 20, 50));

            // You can test other configurations by commenting/uncommenting:
            // Test Case 2: uniform size
//             new CircleUI(Arrays.asList(20, 20, 20, 20, 20));

            // Test Case 3: gradually increasing sizes
             new CircleUI(Arrays.asList(10, 20, 30, 40, 50, 60));

            // Test Case 4: one very small, one very large
//             new CircleUI(Arrays.asList(5, 100));
        });
    }
}
