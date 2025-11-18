package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.CustomerDao;
import raven.modal.demo.model.CustomerModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.MessageUtils;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.combox.JComponentUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.Objects;

@SystemForm(name = "Customer Form", description = "Add or edit Customer information", tags = {"customer", "form"})
public class FormCustomer extends Form {

    private JTextField txtCustomerName, txtContactNo, txtEmail, txtOpeningBalance, taxPer, txtCity;
    private JTextArea txtAddress;
    private JComboBox<String> cmbIsActive;
    private JButton btnSave, btnClear;
    private JLabel titleLabel;

    private int customerId = 0;
    private final CustomerDao customerDao = new CustomerDao();

    public FormCustomer(int customerId) {
        this.customerId = customerId;
        init();
        if (this.customerId > 0) {
            loadCustomerData(this.customerId);
        }
    }

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
        txtCity = new JTextField();
        JComponentUtils.setNumberOnly(txtContactNo);
        txtEmail = new JTextField();
        txtOpeningBalance = new JTextField("0.00");
        JComponentUtils.setNumberOnly(txtOpeningBalance);
        txtAddress = new JTextArea(3, 20);
        taxPer = new JTextField("0.00");
        JComponentUtils.setNumberOnly(taxPer);
        txtAddress.setLineWrap(true);

        txtAddress.setWrapStyleWord(true);

        JScrollPane scrollAddress = new JScrollPane(txtAddress);

        txtCustomerName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter Customer name");
        txtContactNo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter contact number");
        txtEmail.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter email");
        txtEmail.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter City");
        txtOpeningBalance.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g. 1000.00");
        txtAddress.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter full address");
        taxPer.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter Customer Tax Percentage");

        panel.add(new JLabel("Customer Name *:"));
        panel.add(txtCustomerName);
        panel.add(new JLabel("Contact No:"));
        panel.add(txtContactNo);
        panel.add(new JLabel("Email:"));
        panel.add(txtEmail);
        panel.add(new JLabel("City:"));
        panel.add(txtCity);
        panel.add(new JLabel("Address:"));

        panel.add(scrollAddress, "height 60!");
        panel.add(new JLabel("Opening Balance:"));

        panel.add(txtOpeningBalance);
        panel.add(new JLabel("Tax Percentage:"));
        panel.add(taxPer);

        // shows on only edit
        if (this.customerId > 0) {
            cmbIsActive = new JComboBox<>(new String[]{"Active", "Inactive"});
            cmbIsActive.setSelectedItem("Active");
            panel.add(new JLabel("Status:"));
            panel.add(cmbIsActive);
        }

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

        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveCustomer());

        return buttonPanel;
    }

    private void loadCustomerData(int id) {
        CustomerModel customer = customerDao.getCustomerById(id);

        if (customer != null) {
            txtCustomerName.setText(customer.getCustomerName());
            txtContactNo.setText(customer.getContactNo());
            txtEmail.setText(customer.getEmail());
            txtCity.setText(customer.getCity());
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
                .city(Objects.toString(txtCity.getText(), ""))
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

        if (customerId > 0) {
            customer.setIsActive(cmbIsActive.getSelectedIndex() == 0);
            int result = customerDao.updateCustomer(customer);// Call Update
            showMessageResult(result);
            SwingUtilities.getWindowAncestor(this).dispose();
        } else {
            int result = customerDao.addCustomer(customer);// Call Add
            showMessageResult(result);
            clearForm();
        }
    }

    public void clearForm() {
        txtCustomerName.setText("");
        JComponentUtils.resetTextField(txtOpeningBalance, "0.00");
        JComponentUtils.resetTextField(taxPer, "0.00");
        txtContactNo.setText("");
        JComponentUtils.resetTextField(txtContactNo, "");
        txtAddress.setText("");
        txtEmail.setText("");
        txtCity.setText("");
        // Reset title only when in Add mode
        if (customerId == 0) {
            titleLabel.setText("Add Customer");
        }
    }

    @Override
    public void showMessageResult(int result) {
        MessageUtils.showCustomerMessage(result);
    }
}