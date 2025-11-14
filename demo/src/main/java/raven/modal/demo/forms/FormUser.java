package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.UserDAO;
import raven.modal.demo.model.User;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDateTime;

@SystemForm(name = "User Form", description = "Add or edit system user information", tags = {"user", "form"})
public class FormUser extends Form {

    private JTextField txtFullName, txtEmail, txtContactNo, txtCNIC;
    private JTextArea txtAddress;
    private JButton btnSave, btnClear;

    public FormUser() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap, fillx", "[fill]"));
        add(createHeader());
        add(createFormPanel(), "gapy 10");
    }

    private Component createHeader() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap", "[fill]"));
        JLabel title = new JLabel("Add New User");
        JTextPane text = new JTextPane();
        text.setText("Enter user details below. Full Name is mandatory.");
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder());
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");

        panel.add(title);
        panel.add(text, "width 500");
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", ""));
        panel.setBorder(new TitledBorder("User Personal Information"));

        txtFullName = new JTextField();
        txtEmail = new JTextField();
        txtContactNo = new JTextField();
        txtCNIC = new JTextField();
        txtAddress = new JTextArea(3, 20);
        JScrollPane scrollAddress = new JScrollPane(txtAddress);

        // Apply FlatLaf style to fields
        txtFullName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter full name (mandatory)");
        txtEmail.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter email address");
        txtContactNo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter contact number");
        txtCNIC.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter CNIC/ID number");
        txtAddress.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter full residential address");
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);

        panel.add(new JLabel("Full Name *:"));
        panel.add(txtFullName);

        panel.add(new JLabel("Email:"));
        panel.add(txtEmail);

        panel.add(new JLabel("Contact No:"));
        panel.add(txtContactNo);

        panel.add(new JLabel("CNIC:"));
        panel.add(txtCNIC);

        panel.add(new JLabel("Address:"));
        panel.add(scrollAddress, "height 60!");

        panel.add(createButtonPanel(), "span 2, align center, gaptop 15");

        return panel;
    }

    private JPanel createButtonPanel() {
        // Only Save and Clear buttons as requested (no Cancel)
        JPanel buttonPanel = new JPanel(new MigLayout("center", "[][]", "10[]10"));

        btnSave = new JButton("Save User");
        btnClear = new JButton("Clear");

        btnSave.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:white; font:bold");
        btnClear.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        buttonPanel.add(btnSave, "gapright 10");
        buttonPanel.add(btnClear);

        // Add button actions
        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveUser());

        return buttonPanel;
    }

    public void clearForm() {
        txtFullName.setText("");
        txtEmail.setText("");
        txtContactNo.setText("");
        txtCNIC.setText("");
        txtAddress.setText("");
    }

    private void saveUser() {
        String name = txtFullName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full Name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UserDAO userDao = new UserDAO();
        User userModel = User.builder()
                .fullName(txtFullName.getText())
                .createdDate(LocalDateTime.now()) // Set by builder/default
                .address(txtAddress.getText())
                .email(txtEmail.getText())
                .contactNo(txtContactNo.getText())
                .cnic(txtCNIC.getText())
                .isActive(true) // Set default true as per TBLUsers
                .build();

        userDao.addUser(userModel);
        clearForm();
    }
}
