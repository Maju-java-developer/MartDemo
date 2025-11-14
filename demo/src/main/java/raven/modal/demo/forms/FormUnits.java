package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.UnitDao;
import raven.modal.demo.model.UnitModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

@SystemForm(name = "Unit Form", description = "Add or edit measurement unit information", tags = {"unit", "form"})
public class FormUnits extends Form {

    private JTextField txtUnitName;
    private JButton btnSave, btnClear;

    // --- NEW FIELD ---
    private int unitId = 0; // 0 means ADD mode, > 0 means EDIT mode
    private JLabel titleLabel; // To hold the header label for updating text

    // --- NEW CONSTRUCTOR (For Editing) ---
    public FormUnits(int unitId) {
        this.unitId = unitId;
        init();
        if (this.unitId > 0) {
            loadUnitData(this.unitId);
        }
    }

    // --- Original Constructor (Calls the new one) ---
    public FormUnits() {
        this(0); // Default to ADD mode
    }

    private void init() {
        setLayout(new MigLayout("wrap, fillx", "[fill]"));
        add(createHeader());
        add(createFormPanel(), "gapy 10");
    }

    private Component createHeader() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap", "[fill]"));

        // Use the new titleLabel field
        titleLabel = new JLabel(unitId > 0 ? "Edit Unit" : "Add Unit");

        JTextPane text = new JTextPane();
        text.setText("Enter the measurement unit details below.");
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder());

        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");

        panel.add(titleLabel);
        panel.add(text, "width 500");
        return panel;
    }

    // ... createFormPanel and createButtonPanel remain the same ...

    private JPanel createFormPanel() {
        // Simple form panel with a single row
        JPanel panel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", ""));
        panel.setBorder(new TitledBorder("Unit Details"));

        txtUnitName = new JTextField();

        // Apply FlatLaf placeholder style
        txtUnitName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g. Kilogram, Meter, Piece");

        // Form Row
        panel.add(new JLabel("Unit Name *:"));
        panel.add(txtUnitName);

        // Button Panel at the bottom
        panel.add(createButtonPanel(), "span 2, align center, gaptop 15");

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new MigLayout("center", "[][][]", "10[]10"));

        btnSave = new JButton(unitId > 0 ? "Update" : "Save"); // Set button text based on mode
        btnClear = new JButton("Clear");

        // FlatLaf styles for buttons
        btnSave.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:white; font:bold");
        btnClear.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        buttonPanel.add(btnSave, "gapright 10");
        buttonPanel.add(btnClear, "gapright 10");

        // Add button actions
        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveUnit());

        return buttonPanel;
    }

    // --- NEW METHOD: Load data into fields during EDIT mode ---
    private void loadUnitData(int id) {
        UnitDao unitDao = new UnitDao();
        UnitModel unit = unitDao.getUnitById(id);

        if (unit != null) {
            txtUnitName.setText(unit.getUnitName());
            titleLabel.setText("Edit Unit: " + unit.getUnitName()); // Update header title
        } else {
            // Handle case where ID is passed but record is not found
            JOptionPane.showMessageDialog(this, "Unit ID " + id + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            // Optionally revert to ADD mode or close the form
            this.unitId = 0;
            btnSave.setText("Save");
        }
    }

    public void clearForm() {
        txtUnitName.setText("");
    }

    // --- MODIFIED METHOD: Handles both Save (Add) and Update (Edit) ---
    private void saveUnit() {
        String name = txtUnitName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Unit name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UnitDao unitDao = new UnitDao();

        // Build the model
        UnitModel unitModel = UnitModel.builder()
                .unitName(name)
                .build();

        if (unitId > 0) {
            // EDIT Mode: Set the ID and call UPDATE
            unitModel.setUnitID(unitId);
            unitDao.updateUnit(unitModel);
            SwingUtilities.getWindowAncestor(this).dispose();
        } else {
            // ADD Mode: Call ADD
            unitDao.addUnit(unitModel);
        }

        clearForm();

    }
}