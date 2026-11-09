package org.uaf.agct.ui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.Preferences;

public final class FileChooserUtils {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(FileChooserUtils.class);

    private FileChooserUtils() {}

    public static void applyDragAndDrop(JTextField field, boolean isDirectory) {
        field.setToolTipText("Drag & drop file here, or click Browse");
        
        field.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) 
                    || support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    String path = null;
                    Transferable t = support.getTransferable();

                    if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        if (files != null && !files.isEmpty()) {
                            path = files.get(0).getAbsolutePath();
                        }
                    } else if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        path = (String) t.getTransferData(DataFlavor.stringFlavor);
                    }

                    if (path != null) {
                        path = path.trim();
                        if (path.matches("^[A-Za-z]:\\\\.*")) {
                            char drive = Character.toLowerCase(path.charAt(0));
                            path = "/mnt/" + drive + path.substring(2).replace('\\', '/');
                        }
                        if (path.startsWith("\"") && path.endsWith("\"")) {
                            path = path.substring(1, path.length() - 1);
                        }
                        
                        field.setText(path);
                        return true;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return false;
            }
        });
    }

    public static Path chooseFile(JComponent parent, String prefsKey,
                                  String dialogTitle, String description,
                                  String... extensions) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(dialogTitle);
        if (extensions.length > 0) {
            fc.setFileFilter(new FileNameExtensionFilter(description, extensions));
        }
        applyLastDir(fc, prefsKey);

        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        rememberDir(prefsKey, f.toPath().getParent());
        return f.toPath();
    }

    public static Path chooseDirectory(JComponent parent, String prefsKey,
                                       String dialogTitle) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(dialogTitle);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        applyLastDir(fc, prefsKey);

        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        Path dir = fc.getSelectedFile().toPath();
        rememberDir(prefsKey, dir);
        return dir;
    }

    private static void applyLastDir(JFileChooser fc, String key) {
        String saved = PREFS.get(key, null);
        if (saved != null) {
            Path p = Path.of(saved);
            if (Files.isDirectory(p)) {
                fc.setCurrentDirectory(p.toFile());
                return;
            }
        }
        Path wslMnt = Path.of("/mnt");
        if (Files.exists(wslMnt)) {
            fc.setCurrentDirectory(wslMnt.toFile());
        }
    }

    private static void rememberDir(String key, Path dir) {
        if (dir != null) PREFS.put(key, dir.toAbsolutePath().toString());
    }
}