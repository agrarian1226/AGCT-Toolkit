package org.uaf.agct.ui;

import org.uaf.agct.core.FastaParser;
import org.uaf.agct.core.SequenceExtractor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SeqExtractPanel extends JPanel {
    private final MainFrame frame;
    private final JTextField genesField = new JTextField();
    private final JTextField outDirField = new JTextField();
    private final JTextField prefixField = new JTextField("AthRLK");
    private final JCheckBox fastaOutBox = new JCheckBox("Also write combined FASTA per type");
    private final JButton runBtn = new JButton("▶ Run Extraction");

    private final SeqFileSlot cdsSlot = new SeqFileSlot("CDS", false);
    private final SeqFileSlot protSlot = new SeqFileSlot("Protein", false);
    private final SeqFileSlot cdnaSlot = new SeqFileSlot("cDNA", false);
    private final SeqFileSlot promSlot = new SeqFileSlot("Promoter", false);
    private final SeqFileSlot customSlot = new SeqFileSlot("Other", true);

    public SeqExtractPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1.0;

        // Inputs
        c.gridx = 0; c.gridy = 0; c.gridwidth = 1; c.weightx = 0;
        form.add(new JLabel("Gene list:"), c);
        c.gridx = 1; c.weightx = 1.0;
        form.add(genesField, c);
        c.gridx = 2; c.weightx = 0;
        JButton browseGenes = new JButton("Browse");
        browseGenes.addActionListener(e -> {
            Path p = FileChooserUtils.chooseFile(this, "dir.genes", "Choose Gene List", "Text", "txt", "tsv", "csv");
            if (p != null) genesField.setText(p.toString());
        });
        form.add(browseGenes, c);

        // Slots
        c.gridx = 0; c.gridy = 1; c.gridwidth = 3;
        JPanel slotsPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        slotsPanel.setBorder(BorderFactory.createTitledBorder("Sequence files (fill any)"));
        slotsPanel.add(cdsSlot);
        slotsPanel.add(protSlot);
        slotsPanel.add(cdnaSlot);
        slotsPanel.add(promSlot);
        slotsPanel.add(customSlot);
        form.add(slotsPanel, c);

        // Output
        c.gridy = 2; c.gridwidth = 1; c.weightx = 0;
        form.add(new JLabel("Output folder:"), c);
        c.gridx = 1; c.weightx = 1.0;
        form.add(outDirField, c);
        c.gridx = 2; c.weightx = 0;
        JButton browseDir = new JButton("Browse");
        browseDir.addActionListener(e -> {
            Path p = FileChooserUtils.chooseDirectory(this, "dir.out", "Choose Output Folder");
            if (p != null) outDirField.setText(p.toString());
        });
        form.add(browseDir, c);

        c.gridx = 0; c.gridy = 3; c.weightx = 0;
        form.add(new JLabel("File prefix:"), c);
        c.gridx = 1; c.gridwidth = 2; c.weightx = 1.0;
        form.add(prefixField, c);

        c.gridx = 1; c.gridy = 4; c.gridwidth = 2;
        form.add(fastaOutBox, c);

        add(form, BorderLayout.NORTH);

        // Run Button
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        runBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        runBtn.setPreferredSize(new Dimension(200, 35));
        bottom.add(runBtn);
        add(bottom, BorderLayout.SOUTH);

        runBtn.addActionListener(e -> runExtraction());
    }

    private void runExtraction() {
        Path genes = getPath(genesField);
        Path outDir = getPath(outDirField);
        String prefix = prefixField.getText().trim();

        if (genes == null || outDir == null || prefix.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill Gene list, Output folder, and Prefix.", "Missing Info", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<SeqFileSlot> activeSlots = new ArrayList<>();
        for (SeqFileSlot slot : new SeqFileSlot[]{cdsSlot, protSlot, cdnaSlot, promSlot, customSlot}) {
            if (slot.getPath() != null) activeSlots.add(slot);
        }

        if (activeSlots.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one FASTA file.", "Missing Info", JOptionPane.WARNING_MESSAGE);
            return;
        }

        frame.clearLog();
        runBtn.setEnabled(false);
        frame.setProgress(-1);

        new SwingWorker<Void, String>() {
            @Override protected Void doInBackground() {
                try {
                    List<String> ids = Files.readAllLines(genes).stream()
                            .map(String::trim).filter(s -> !s.isEmpty() && !s.startsWith("#"))
                            .map(s -> s.split("\\s+")[0]).toList();

                    for (SeqFileSlot slot : activeSlots) {
                        publish("Parsing " + slot.getLabel() + " FASTA...");
                        FastaParser.FastaData fasta = FastaParser.parse(slot.getPath());
                        
                        String outName = prefix + "_" + slot.getLabel() + ".tsv";
                        Path outTsv = outDir.resolve(outName);
                        Path outFasta = fastaOutBox.isSelected() ? outDir.resolve(prefix + "_" + slot.getLabel() + ".fasta") : null;
                        
                        publish("Extracting " + slot.getLabel() + " sequences...");
                        SequenceExtractor.extract(fasta, ids, outTsv, outFasta, this::publish);
                    }
                    return null;
                } catch (Exception ex) {
                    publish("ERROR: " + ex.getMessage());
                    return null;
                }
            }
            @Override protected void process(List<String> chunks) {
                for (String s : chunks) frame.log(s);
            }
            @Override protected void done() {
                runBtn.setEnabled(true);
                frame.setProgress(100);
                frame.setStatus("Done");
                frame.setLastOutputFolder(outDir);
                JOptionPane.showMessageDialog(SeqExtractPanel.this, "Extraction complete!\nFiles saved in:\n" + outDir, "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    private Path getPath(JTextField f) {
        String t = f.getText().trim();
        return t.isEmpty() ? null : Path.of(t);
    }
}