package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.PackingTypeDao;
import raven.modal.demo.model.PackingTypeModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.combox.JComponentUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Objects;

@SystemForm(name = "Pecking Type Form", description = "Add or edit Pecking type information", tags = {"Pecking", "form"})
public class FormPackingType extends Form {

    private JTextField txtTypeName, txtQuarterQty;
    private JComboBox<String> cmbIsActive;
    private JButton btnSave, btnClear;

    private int PeckingTypeId = 0;
    private JLabel titleLabel;
    private PackingTypeDao PackingTypeDao = new PackingTypeDao();

    public FormPackingType(int PeckingTypeId) {
        this.PeckingTypeId = PeckingTypeId;
        init();
        if (this.PeckingTypeId > 0) {
            loadPeckingTypeData(this.PeckingTypeId);
        }
    }

    public FormPackingType() {
        this(0);
    }

    private void init() {
        setLayout(new MigLayout("wrap, fillx", "[fill]"));
        add(createHeader());
        add(createFormPanel(), "gapy 10");
    }

    private Component createHeader() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap", "[fill]"));

        titleLabel = new JLabel(PeckingTypeId > 0 ? "Edit Pecking Type" : "Add New Pecking Type");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");

        JTextPane text = new JTextPane();
        text.setText("Enter Pecking type details, Quarter Qty must be at least 1.");
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder());

        panel.add(titleLabel);
        panel.add(text, "width 500");
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", ""));
        panel.setBorder(new TitledBorder("Type Details"));

        txtTypeName = new JTextField();
        txtQuarterQty = new JTextField();
        JComponentUtils.setNumberOnly(txtQuarterQty);

        cmbIsActive = new JComboBox<>(new String[]{"Active", "Inactive"});
        cmbIsActive.setSelectedItem("Active");

        // Default value for ADD mode
        if (PeckingTypeId == 0) {
            txtQuarterQty.setText("1");
        }

        txtTypeName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g., Daily, Weekly, Monthly");
        txtQuarterQty.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Minimum 1");

        panel.add(new JLabel("PeckingType Title *:"));
        panel.add(txtTypeName);

        panel.add(new JLabel("Quarter Qty *:"));
        panel.add(txtQuarterQty);

        panel.add(new JLabel("Status:"));
        panel.add(cmbIsActive);

        panel.add(createButtonPanel(), "span 2, align center, gaptop 15");

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new MigLayout("center", "[][][]", "10[]10"));

        btnSave = new JButton(PeckingTypeId > 0 ? "Update" : "Save");
        btnClear = new JButton("Clear");

        btnSave.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:white; font:bold");
        btnClear.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        buttonPanel.add(btnSave, "gapright 10");
        buttonPanel.add(btnClear, "gapright 10");

        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> savePeckingType());

        return buttonPanel;
    }

    private void loadPeckingTypeData(int id) {
        PackingTypeModel type = PackingTypeDao.getPeckingTypeById(id);

        if (type != null) {
            txtTypeName.setText(type.getPackingTypeName());
            txtQuarterQty.setText(String.valueOf(type.getQuarterQty()));
            cmbIsActive.setSelectedItem(type.isActive() ? "Active" : "Inactive");
            titleLabel.setText("Edit Pecking Type: " + type.getPackingTypeName());
        } else {
            JOptionPane.showMessageDialog(this, "Pecking Type ID " + id + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            this.PeckingTypeId = 0;
        }
    }

    public void clearForm() {
        txtTypeName.setText("");
        txtQuarterQty.setText("1"); // Reset to default 1
        cmbIsActive.setSelectedItem("Active");
    }

    private void savePeckingType() {
        String name = txtTypeName.getText().trim();
        String qtyText = txtQuarterQty.getText().trim();

        // --- Validation Logic ---
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Type Name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int quarterQty;
        try {
            quarterQty = Integer.parseInt(qtyText);
            if (quarterQty < 1) { // Check for minimum value
                JOptionPane.showMessageDialog(this, "Quarter Qty must be 1 or greater.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quarter Qty must be a valid number.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // --- End Validation ---

        boolean isActive = Objects.equals(cmbIsActive.getSelectedItem(), "Active");

        PackingTypeModel typeModel = PackingTypeModel.builder()
                .packingTypeName(name)
                .quarterQty(quarterQty)
                .isActive(isActive)
                .build();

        if (PeckingTypeId > 0) {
            typeModel.setPackingTypeId(PeckingTypeId);
            PackingTypeDao.updatePackingType(typeModel);
        } else {
            PackingTypeDao.addPackingType(typeModel);
        }

        SwingUtilities.getWindowAncestor(this).dispose();
    }
}
