package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.system.Form;

import javax.swing.*;
import java.io.File;
import java.time.LocalDate;

public class FormBackupRestore extends Form {

    private final BackupRestoreManager manager = new BackupRestoreManager();
    private JButton btnBackup, btnRestore;

    public FormBackupRestore() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap, fill, insets 30 50", "[fill]"));

        JLabel title = new JLabel("Database Security and Recovery");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        add(title, "gapy 0 15, align center");

        // --- Backup Section ---
        add(new JLabel("Backup Database (Encrypted)"), "gapy 10");
        add(new JLabel("Creates a secure, encrypted copy of your current database. You must keep the encryption key safe."), "w 400, gapy 0 5");
        btnBackup = new JButton("CREATE ENCRYPTED BACKUP (.enc)");
        btnBackup.putClientProperty(FlatClientProperties.STYLE, "background: #4CAF50; foreground: #FFFFFF");
        btnBackup.addActionListener(e -> handleBackupAction());
        add(btnBackup, "h 40!, w 350");

        // --- Separator ---
        JSeparator separator = new JSeparator();
        add(separator, "gapy 15 15");

        // --- Restore Section ---
        add(new JLabel("Restore Database"), "gapy 10");
        add(new JLabel("WARNING: Restoring will overwrite ALL current data with data from the backup file."), "w 400, gapy 0 5");
        btnRestore = new JButton("RESTORE FROM ENCRYPTED FILE (.enc)");
        btnRestore.putClientProperty(FlatClientProperties.STYLE, "background: #F44336; foreground: #FFFFFF");
        btnRestore.addActionListener(e -> handleRestoreAction());
        add(btnRestore, "h 40!, w 350");
    }

    // --- Action Handlers ---
    private void handleBackupAction() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Encrypted Database Backup");
        fileChooser.setSelectedFile(new File("db_backup_" + LocalDate.now() + ".enc"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            // Ensure file extension is .enc
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".enc")) {
                fileToSave = new File(filePath + ".enc");
            }

            // Disable buttons during process
            setButtonsEnabled(false);

            // Run backup on a separate thread to prevent freezing the UI
            File finalFileToSave = fileToSave;
            new Thread(() -> {
                boolean success = manager.createEncryptedBackup(finalFileToSave);
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(this, "Encrypted Backup Successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                    setButtonsEnabled(true);
                });
            }).start();
        }
    }

    private void handleRestoreAction() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "WARNING: Restoring the database will overwrite ALL current data. Proceed?",
                "Confirm Database Restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Encrypted Backup File (.enc)");

        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToRestore = fileChooser.getSelectedFile();

            // Disable buttons during process
            setButtonsEnabled(false);

            // Run restore on a separate thread
            new Thread(() -> {
                boolean success = manager.restoreEncryptedBackup(fileToRestore);
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(this, "Database Restore Successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                    setButtonsEnabled(true);
                });
            }).start();
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        btnBackup.setEnabled(enabled);
        btnRestore.setEnabled(enabled);
        // Optionally show a JProgressBar or JLayeredPane with loading GIF here
    }
}