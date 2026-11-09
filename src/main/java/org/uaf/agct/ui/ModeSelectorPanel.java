package org.uaf.agct.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public final class ModeSelectorPanel extends JPanel {

    public enum Mode { GFF, SEQUENCE }

    private final JToggleButton btnGff  = makeButton(
            "\uD83E\uDDEC  GFF3 Feature Extraction",
            "Locations, strand, exons, introns, gene length");
    private final JToggleButton btnSeq  = makeButton(
            "\uD83D\uDD24  Sequence Extraction",
            "CDS, protein, cDNA, promoter or custom FASTA");

    private final ButtonGroup group = new ButtonGroup();
    private Mode current = Mode.GFF;

    public ModeSelectorPanel(Consumer<Mode> onModeChanged) {
        setLayout(new GridLayout(1, 2, 10, 0));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        group.add(btnGff);
        group.add(btnSeq);
        add(btnGff);
        add(btnSeq);

        btnGff.setSelected(true);

        btnGff.addActionListener(e -> {
            current = Mode.GFF;
            onModeChanged.accept(Mode.GFF);
        });
        btnSeq.addActionListener(e -> {
            current = Mode.SEQUENCE;
            onModeChanged.accept(Mode.SEQUENCE);
        });
    }

    public Mode getCurrentMode() { return current; }

    private static JToggleButton makeButton(String title, String subtitle) {
        JToggleButton b = new JToggleButton(
                "<html><div style='text-align:center'>"
                        + "<div style='font-size:14px;font-weight:bold'>" + title + "</div>"
                        + "<div style='font-size:11px;color:gray'>" + subtitle + "</div>"
                        + "</div></html>");
        b.setPreferredSize(new Dimension(240, 60));
        b.setFocusPainted(false);
        return b;
    }
}