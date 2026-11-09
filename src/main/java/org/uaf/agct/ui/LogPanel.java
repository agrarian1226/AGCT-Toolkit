package org.uaf.agct.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.nio.file.Path;

public final class LogPanel extends JPanel {

    private final JTextArea logArea    = new JTextArea(8, 80);
    private final JProgressBar bar     = new JProgressBar(0, 100);
    private final JButton openFolder   = new JButton("\uD83D\uDCC2  Open output folder");
    private Path lastOutputFolder      = null;

    public LogPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(new EmptyBorder(6, 8, 8, 8));

        logArea.setEditable(false);
        logArea.setLineWrap(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Log"));

        bar.setStringPainted(true);
        bar.setString("Ready");

        openFolder.setEnabled(false);
        openFolder.addActionListener(e -> openLastFolder());

        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.add(bar, BorderLayout.CENTER);
        bottom.add(openFolder, BorderLayout.EAST);

        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void append(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> logArea.setText(""));
    }

    public void setProgress(int pct) {
        SwingUtilities.invokeLater(() -> {
            if (pct < 0) {
                bar.setIndeterminate(true);
                bar.setString("Working…");
            } else {
                bar.setIndeterminate(false);
                bar.setValue(pct);
                bar.setString(pct + "%");
            }
        });
    }

    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> {
            bar.setIndeterminate(false);
            bar.setString(text);
        });
    }

    public void setLastOutputFolder(Path folder) {
        this.lastOutputFolder = folder;
        SwingUtilities.invokeLater(() -> openFolder.setEnabled(folder != null));
    }

    private void openLastFolder() {
        if (lastOutputFolder == null) return;
        try {
            // WSL FIX: Use wslpath and explorer.exe to open folders from Linux
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", 
                "explorer.exe $(wslpath -w \"" + lastOutputFolder.toAbsolutePath() + "\")");
            pb.start();
        } catch (Exception ex) {
            append("Failed to open folder: " + ex.getMessage());
        }
    }
}