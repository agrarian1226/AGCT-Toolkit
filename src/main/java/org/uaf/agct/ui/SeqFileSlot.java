package org.uaf.agct.ui;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/** Reusable UI row for a FASTA file input (Label + Path + Browse + Clear). */
public final class SeqFileSlot extends JPanel {
    private final JTextField pathField = new JTextField();
    private final JTextField labelField = new JTextField(10);
    private final String typeName;
    private final boolean isCustom;

    public SeqFileSlot(String typeName, boolean isCustom) {
        this.typeName = typeName;
        this.isCustom = isCustom;
        setLayout(new BorderLayout(5, 0));

        JLabel label = new JLabel(String.format("%-10s", typeName + ":"));
        label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        
        JPanel left = new JPanel(new BorderLayout());
        left.add(label, BorderLayout.WEST);
        
        if (isCustom) {
            labelField.setText("custom");
            left.add(labelField, BorderLayout.CENTER);
            left.setPreferredSize(new Dimension(120, 25));
        } else {
            left.setPreferredSize(new Dimension(80, 25));
        }

        JButton browse = new JButton("Browse");
        JButton clear = new JButton("×");
        clear.setMargin(new Insets(2, 5, 2, 5));

        JPanel right = new JPanel(new BorderLayout(5, 0));
        right.add(clear, BorderLayout.EAST);
        right.add(browse, BorderLayout.CENTER);

        add(left, BorderLayout.WEST);
        add(pathField, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        browse.addActionListener(e -> {
            Path p = FileChooserUtils.chooseFile(this, "dir.fasta." + typeName, 
                    "Select " + typeName + " FASTA", "FASTA files", "fasta", "fa", "fna", "txt");
            if (p != null) pathField.setText(p.toString());
        });

        clear.addActionListener(e -> pathField.setText(""));
    }

    public Path getPath() {
        String t = pathField.getText().trim();
        return t.isEmpty() ? null : Path.of(t);
    }

    public String getLabel() {
        return isCustom ? labelField.getText().trim() : typeName.toLowerCase();
    }
}