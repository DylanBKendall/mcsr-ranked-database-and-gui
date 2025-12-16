package ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;

public class UiUtils {

    public static void scaleUI(Component root, float factor) {
        if (root == null || factor == 1.0f) return;

        Font f = root.getFont();
        if (f != null) {
            root.setFont(f.deriveFont(f.getSize2D() * factor));
        }

        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                scaleUI(child, factor);
            }
        }
    }

    public static String formatMillis(Number n) {
        if (n == null) return "N/A";
        long ms = n.longValue();
        if (ms < 0) return "N/A";

        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }
}