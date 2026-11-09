package org.uaf.agct.ui;

import org.uaf.agct.core.GeneInfoExtractor;
import org.uaf.agct.core.GffParser;
import org.uaf.agct.model.Column;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GffExtractPanel extends JPanel {
    private final MainFrame frame;
    private final JTextField gffField = new JTextField();
    private final JTextField genesField = new JTextField();
    private final JTextField outDirField = new JTextField();
    private final JTextField outNameField = new JTextField("gene_info.tsv");
    private final JButton runBtn = new JButton("▶ Run Extraction");
    private final Map<Column, JCheckBox> colBoxes = new LinkedHashMap<>();

    public GffExtractPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 4, 4);

        // Inputs
        addRow(form, c, 0, "GFF3 file:", gffField, "Choose GFF3", "gff", "gff3");
        addRow(form, c, 1, "Gene list:", genesField, "Choose Gene List", "txt", "tsv", "csv");
        
        // Columns checkboxes
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        JPanel checkPanel = new JPanel(new GridLayout(0, 4, 5, 5));
        checkPanel.setBorder(BorderFactory.createTitledBorder("Columns to extract (IDs are mandatory)"));
        for (Column col : Column.values()) {
            JCheckBox cb = new JCheckBox(col.getDisplayName(), true);
            if (col.isMandatory()) {
                cb.setEnabled(false);
                cb.setToolTipText("This column is required to link the data.");
            }
            colBoxes.put(col, cb);
            checkPanel.add(cb);
        }
        form.add(checkPanel, c);

        // Output
        addRow(form, c, 3, "Output folder:", outDirField, "Choose Folder", null);
        c.gridx = 0; c.gridy = 4; c.gridwidth = 1; form.add(new JLabel("Filename:"), c);
        c.gridx = 1; form.add(outNameField, c);

        add(form, BorderLayout.NORTH);

        // Run Button
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        runBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        runBtn.setPreferredSize(new Dimension(200, 35));
        bottom.add(runBtn);
        add(bottom, BorderLayout.SOUTH);

        runBtn.addActionListener(e -> runExtraction());
    }

    private void addRow(JPanel p, GridBagConstraints c, int y, String label, JTextField field, String btnTxt, String... ext) {
        c.gridx = 0; c.gridy = y; c.gridwidth = 1; c.weightx = 0;
        p.add(new JLabel(label), c);
        
        c.gridx = 1; c.weightx = 1.0;
        p.add(field, c);
        
        c.gridx = 2; c.weightx = 0;
        if (btnTxt != null) {
            JButton b = new JButton("Browse");
            b.addActionListener(ev -> {
                Path path = btnTxt.equals("Choose Folder") 
                        ? FileChooserUtils.chooseDirectory(this, "dir." + label, btnTxt)
                        : FileChooserUtils.chooseFile(this, "file." + label, btnTxt, "Files", ext);
                if (path != null) field.setText(path.toString());
            });
            p.add(b, c);
        }
    }

    private void runExtraction() {
        Path gff = getPath(gffField);
        Path genes = getPath(genesField);
        Path outDir = getPath(outDirField);
        String outName = outNameField.getText().trim();

        if (gff == null || genes == null || outDir == null || outName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all input/output fields.", "Missing Info", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Column> selected = new ArrayList<>();
        for (var entry : colBoxes.entrySet()) {
            if (entry.getValue().isSelected()) selected.add(entry.getKey());
        }

        frame.clearLog();
        runBtn.setEnabled(false);
        frame.setProgress(-1);
        Path outPath = outDir.resolve(outName);

        new SwingWorker<Void, String>() {
            @Override protected Void doInBackground() {
                try {
                    publish("Parsing GFF3 file...");
                    GffParser.GffData gffData = GffParser.parse(gff);
                    publish("GFF parsed. Running extraction...");
                    
                    List<String> ids = Files.readAllLines(genes).stream()
                            .map(String::trim).filter(s -> !s.isEmpty() && !s.startsWith("#"))
                            .map(s -> s.split("\\s+")[0]).toList();

                    GeneInfoExtractor.extract(gffData, ids, selected, outPath, this::publish);
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
                JOptionPane.showMessageDialog(GffExtractPanel.this, "Extraction complete!\nFile saved to:\n" + outPath, "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    private Path getPath(JTextField f) {
        String t = f.getText().trim();
        return t.isEmpty() ? null : Path.of(t);
    }
}