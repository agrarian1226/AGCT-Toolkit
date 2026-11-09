package org.uaf.agct.ui;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public final class MainFrame extends JFrame {

    private static final String CARD_GFF = "gff";
    private static final String CARD_SEQ = "seq";

    private final CardLayout cards       = new CardLayout();
    private final JPanel     cardHolder  = new JPanel(cards);
    private final LogPanel   logPanel    = new LogPanel();

    private final GffExtractPanel gffPanel;
    private final SeqExtractPanel seqPanel;

    public MainFrame() {
        super("AGCT — Agri Genomics Toolkit (v0.41)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(880, 680));
        setSize(960, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Menu Bar ---
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About AGCT");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        // Panels
        gffPanel = new GffExtractPanel(this);
        seqPanel = new SeqExtractPanel(this);

        cardHolder.add(gffPanel, CARD_GFF);
        cardHolder.add(seqPanel, CARD_SEQ);

        ModeSelectorPanel selector = new ModeSelectorPanel(this::switchMode);

        add(selector,   BorderLayout.NORTH);
        add(cardHolder, BorderLayout.CENTER);
        add(logPanel,   BorderLayout.SOUTH);

        logPanel.append("AGCT v0.41 ready.");
        logPanel.append("Choose a mode above, then pick your input files.");
    }

    private void switchMode(ModeSelectorPanel.Mode mode) {
        switch (mode) {
            case GFF -> cards.show(cardHolder, CARD_GFF);
            case SEQUENCE -> cards.show(cardHolder, CARD_SEQ);
        }
    }

    private void showAbout() {
        String aboutText = "<html><div style='width:400px; text-align:center;'>"
                + "<h2>AGCT — Agri Genomics Toolkit</h2>"
                + "<b>Version 0.41</b><br><br>"
                + "<b>Dr. Rana Muhammad Atif</b><br>"
                + "Plant Genomics Lab,<br>"
                + "Center for Advanced Studies in Agriculture &amp; Food Security,<br>"
                + "University of Agriculture Faisalabad.<br><br>"
                + "<b>Contact:</b> <a href='mailto:dratif@uaf.edu.pk'>dratif@uaf.edu.pk</a><br><br>"
                + "<b>Profiles:</b><br>"
                + "<a href='https://www.linkedin.com/in/rana-muhammad-atif-12909830/'>LinkedIn</a> | "
                + "<a href='https://github.com/agrarian1226'>GitHub</a> | "
                + "<a href='https://scholar.google.com/citations?user=Ky4qxa8AAAAJ&hl=en'>Google Scholar</a><br><br>"
                + "<i>A desktop toolkit for extracting genomic feature annotations "
                + "and sequence records from GFF3 and FASTA files.</i>"
                + "</div></html>";
        JOptionPane.showMessageDialog(this, aboutText, "About AGCT", JOptionPane.INFORMATION_MESSAGE);
    }

    public void log(String line)              { logPanel.append(line); }
    public void clearLog()                    { logPanel.clear(); }
    public void setProgress(int pct)          { logPanel.setProgress(pct); }
    public void setStatus(String status)      { logPanel.setStatus(status); }
    public void setLastOutputFolder(Path dir) { logPanel.setLastOutputFolder(dir); }
}