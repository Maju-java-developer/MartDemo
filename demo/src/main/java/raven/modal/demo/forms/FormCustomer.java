package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.CustomerDao;
import raven.modal.demo.model.CustomerModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.Objects;

@SystemForm(name = "Customer Form", description = "Add or edit Customer information", tags = {"customer", "form"})
public class FormCustomer extends Form {

    private JTextField txtCustomerName, txtContactNo, txtEmail, txtOpeningBalance, taxPer;
    private JTextArea txtAddress;
    private JButton btnSave, btnClear;
    private JLabel titleLabel; // Added to hold the title

    private int customerId = 0;
    private final CustomerDao customerDao = new CustomerDao();

    public FormCustomer(int customerId) {
        this.customerId = customerId;
        init();
        if (this.customerId > 0) {
            loadCustomerData(this.customerId);
        }
    }

    // Default constructor for Add mode
    public FormCustomer() {
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
        JLabel title = new JLabel(customerId > 0 ? "Edit Customer" : "Add Customer");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");

        JTextPane text = new JTextPane();
        text.setText("Enter customer details below. Fields marked with * are mandatory.");
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder());

        panel.add(title);
        panel.add(text, "width 500");
        add(panel);
        return title; // Return the dynamic label
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", ""));
        panel.setBorder(new TitledBorder("Customer Information"));

        txtCustomerName = new JTextField();
        txtContactNo = new JTextField();
        txtEmail = new JTextField();
        txtOpeningBalance = new JTextField("0.00");
        txtAddress = new JTextArea(3, 20);
        taxPer = new JTextField("0.00");
        txtAddress.setLineWrap(true);

        txtAddress.setWrapStyleWord(true);

        JScrollPane scrollAddress = new JScrollPane(txtAddress);

        txtCustomerName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter Customer name");
        txtContactNo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter contact number");
        txtEmail.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter email");
        txtOpeningBalance.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g. 1000.00");
        txtAddress.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter full address");
        taxPer.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter Customer Tax Percentage");

        panel.add(new JLabel("Customer Name *:"));
        panel.add(txtCustomerName);
        panel.add(new JLabel("Contact No:"));
        panel.add(txtContactNo);
        panel.add(new JLabel("Email:"));
        panel.add(txtEmail);
        panel.add(new JLabel("Address:"));

        panel.add(scrollAddress, "height 60!");
        panel.add(new JLabel("Opening Balance:"));

        panel.add(txtOpeningBalance);
        panel.add(new JLabel("Tax Percentage:"));
        panel.add(taxPer);
        panel.add(createButtonPanel(), "span 2, align center, gaptop 15");
        return panel;
    }


    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new MigLayout("center", "[][][]", "10[]10"));

        // Dynamic Button Text
        btnSave = new JButton(customerId > 0 ? "Update" : "Save");
        btnClear = new JButton("Clear");

        btnSave.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:white; font:bold");
        btnClear.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        buttonPanel.add(btnSave, "gapright 10");
        buttonPanel.add(btnClear, "gapright 10");

        btnClear.addActionListener(e -> saveCustomer());
        btnSave.addActionListener(e -> saveCustomer());

        return buttonPanel;
    }

    private void loadCustomerData(int id) {
        CustomerModel customer = customerDao.getCustomerById(id);

        if (customer != null) {
            txtCustomerName.setText(customer.getCustomerName());
            txtContactNo.setText(customer.getContactNo());
            txtEmail.setText(customer.getEmail());
            txtAddress.setText(customer.getAddress());
            // Use DecimalFormat or similar for cleaner presentation, or just String.valueOf
            txtOpeningBalance.setText(String.format("%.2f", customer.getOpeningBalance()));
            taxPer.setText(String.format("%.2f", customer.getTaxPer()));

            titleLabel.setText("Edit Customer: " + customer.getCustomerName());
        } else {
            JOptionPane.showMessageDialog(this, "Customer ID " + id + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            this.customerId = 0;
            titleLabel.setText("Add Customer");
        }
    }


    private void saveCustomer() {
        if (txtCustomerName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Customer name is required!");
            return;
        }

        String name = txtCustomerName.getText();

        CustomerModel customer = CustomerModel.builder()
                .customerId(customerId)
                .customerName(name)
                .contactNo(Objects.toString(txtContactNo.getText(), ""))
                .address(Objects.toString(txtAddress.getText(), ""))
                .email(Objects.toString(txtEmail.getText(), ""))
                .build();

        // Handle number parsing and default values
        try {
            customer.setOpeningBalance(Double.parseDouble(txtOpeningBalance.getText().trim()));
        } catch (NumberFormatException e) {
            customer.setOpeningBalance(0.00);
        }
        try {
            customer.setTaxPer(Double.parseDouble(taxPer.getText().trim()));
        } catch (NumberFormatException e) {
            customer.setTaxPer(0.00);
        }

        boolean success;
        if (customerId > 0) {
            customerDao.updateCustomer(customer); // Call Update
            JOptionPane.showMessageDialog(this, "Customer updated successfully!");
            // Close the modal window upon success
            SwingUtilities.getWindowAncestor(this).dispose();
        } else {
            customerDao.addCustomer(customer); // Call Add
            JOptionPane.showMessageDialog(this, customerId > 0 ? "Customer updated successfully!" : "Customer added successfully!");
        }
    }

}