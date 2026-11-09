package org.uaf.agct;

import com.formdev.flatlaf.FlatLightLaf;
import org.uaf.agct.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** AGCT — Agri Genomics Toolkit. Entry point. */
public final class Main {
    public static void main(String[] args) {
        // Install FlatLaf before any Swing component is created
        FlatLightLaf.setup();
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}