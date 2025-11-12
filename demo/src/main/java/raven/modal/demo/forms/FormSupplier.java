package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.SupplierDao;
import raven.modal.demo.model.SupplierModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.time.LocalDateTime;
import java.util.Objects;

@SystemForm(name = "Vendor Form", description = "Add or edit vendor information", tags = {"vendor", "form"})
public class FormSupplier extends Form {

    private JTextField txtSupplierName, txtContactNo, txtEmail, txtOpeningBalance;
    private JTextArea txtAddress;
    private JButton btnSave, btnClear;
    private JLabel titleLabel; // To update title in edit mode

    private int supplierId = 0; // 0 for ADD mode
    private final SupplierDao supplierDao = new SupplierDao();

    public FormSupplier(int supplierId) {
        this.supplierId = supplierId;
        init();
        if (this.supplierId > 0) {
            loadSupplierData(this.supplierId);
        }
    }

    public FormSupplier() {
        this(0);
    }

    private void init() {
        setLayout(new MigLayout("wrap, fillx", "[fill]"));
        titleLabel = createHeader();
        add(titleLabel);
        add(createFormPanel(), "gapy 10");
    }

    private JLabel createHeader() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap", "[fill]"));
        // Dynamic Title
        JLabel title = new JLabel(supplierId > 0 ? "Edit Vendor" : "Add Vendor");

        JTextPane text = new JTextPane();
        text.setText("Enter vendor details below. Fields marked with * are mandatory.");
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder());
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");

        panel.add(title);
        panel.add(text, "width 500");
        add(panel);
        return title; // Return the dynamic label
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", ""));
        panel.setBorder(new TitledBorder("Vendor Information"));

        txtSupplierName = new JTextField();
        txtContactNo = new JTextField();
        txtEmail = new JTextField();
        txtOpeningBalance = new JTextField("0.00");
        txtAddress = new JTextArea(3, 20);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        JScrollPane scrollAddress = new JScrollPane(txtAddress);

        // Apply FlatLaf style to all fields
        txtSupplierName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter vendor name");
        txtContactNo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter contact number");
        txtEmail.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter email");
        txtOpeningBalance.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g. 1000.00");
        txtAddress.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter full address");

        panel.add(new JLabel("Vendor Name *:"));
        panel.add(txtSupplierName);

        panel.add(new JLabel("Contact No:"));
        panel.add(txtContactNo);

        panel.add(new JLabel("Email:"));
        panel.add(txtEmail);

        panel.add(new JLabel("Address:"));
        panel.add(scrollAddress, "height 60!");

        panel.add(new JLabel("Opening Balance:"));
        panel.add(txtOpeningBalance);

        panel.add(createButtonPanel(), "span 2, align center, gaptop 15");

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new MigLayout("center", "[][][]", "10[]10"));

        // Dynamic Button Text
        btnSave = new JButton(supplierId > 0 ? "Update" : "Save");
        btnClear = new JButton("Clear");

        btnSave.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:white; font:bold");
        btnClear.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        buttonPanel.add(btnSave, "gapright 10");
        buttonPanel.add(btnClear, "gapright 10");

        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveSupplier());
        return buttonPanel;
    }

    private void loadSupplierData(int id) {
        SupplierModel supplier = supplierDao.getSupplierById(id);

        if (supplier != null) {
            txtSupplierName.setText(supplier.getSupplierName());
            txtContactNo.setText(supplier.getContactNo());
            txtEmail.setText(supplier.getEmail());
            txtAddress.setText(supplier.getAddress());
            txtOpeningBalance.setText(String.format("%.2f", supplier.getOpeningBalance()));

            titleLabel.setText("Edit Vendor: " + supplier.getSupplierName());
        } else {
            JOptionPane.showMessageDialog(this, "Vendor ID " + id + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            this.supplierId = 0;
            titleLabel.setText("Add Vendor");
        }
    }

    private void clearForm() {
        txtSupplierName.setText("");
        txtContactNo.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        txtOpeningBalance.setText("0.00");
    }

    private void saveSupplier() {
        String name = txtSupplierName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Supplier name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Use default value for number fields if empty
        double openingBalance = 0.00;
        try {
            openingBalance = Double.parseDouble(txtOpeningBalance.getText().trim());
        } catch (NumberFormatException ignored) {}

        SupplierModel supplierModel = SupplierModel.builder()
                .supplierID(supplierId) // Set ID if editing, 0 if adding
                .supplierName(name)
                .contactNo(Objects.toString(txtContactNo.getText(), ""))
                .address(Objects.toString(txtAddress.getText(), ""))
                .email(Objects.toString(txtEmail.getText(), ""))
                .openingBalance(openingBalance)
                .createdDate(LocalDateTime.now()) // Use current time for simplicity or fetch existing for update
                .build();

        if (supplierId > 0) {
            supplierDao.updateSupplier(supplierModel);
            JOptionPane.showMessageDialog(this, supplierId > 0 ? "Vendor updated successfully!" : "Vendor added successfully!");
            // Close the modal window upon success
            SwingUtilities.getWindowAncestor(this).dispose();
        } else {
            supplierDao.addSupplier(supplierModel);
            JOptionPane.showMessageDialog(this, supplierId > 0 ? "Vendor updated successfully!" : "Vendor added successfully!");
        }
    }
}