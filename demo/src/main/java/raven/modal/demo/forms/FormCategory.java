package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.CategoryDao;
import raven.modal.demo.model.CategoryModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Objects;

@SystemForm(name = "Category Form", description = "Add or edit product category information", tags = {"category", "form"})
public class FormCategory extends Form {

    private JTextField txtCategoryName;
    private JComboBox<String> cmbIsActive;
    private JButton btnSave, btnClear;

    private int categoryId = 0;
    private JLabel titleLabel;
    private CategoryDao categoryDao = new CategoryDao();

    public FormCategory(int categoryId) {
        this.categoryId = categoryId;
        init();
        if (this.categoryId > 0) {
            loadCategoryData(this.categoryId);
        }
    }

    public FormCategory() {
        this(0); // Default to ADD mode
    }

    private void init() {
        setLayout(new MigLayout("wrap, fillx", "[fill]"));
        add(createHeader());
        add(createFormPanel(), "gapy 10");
    }

    private Component createHeader() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap", "[fill]"));

        titleLabel = new JLabel(categoryId > 0 ? "Edit Category" : "Add New Category");
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");

        JTextPane text = new JTextPane();
        text.setText("Enter category name and set status. Category Name is mandatory.");
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder());

        panel.add(titleLabel);
        panel.add(text, "width 500");
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", ""));
        panel.setBorder(new TitledBorder("Category Details"));

        txtCategoryName = new JTextField();
        cmbIsActive = new JComboBox<>(new String[]{"Active", "Inactive"});
        cmbIsActive.setSelectedItem("Active");

        txtCategoryName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter category name (e.g., Electronics, Food)");

        panel.add(new JLabel("Category Name *:"));
        panel.add(txtCategoryName);

        panel.add(new JLabel("Status:"));
        panel.add(cmbIsActive);

        panel.add(createButtonPanel(), "span 2, align center, gaptop 15");

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new MigLayout("center", "[][][]", "10[]10"));

        btnSave = new JButton(categoryId > 0 ? "Update" : "Save");
        btnClear = new JButton("Clear");

        btnSave.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:white; font:bold");
        btnClear.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        buttonPanel.add(btnSave, "gapright 10");
        buttonPanel.add(btnClear, "gapright 10");

        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveCategory());

        return buttonPanel;
    }

    private void loadCategoryData(int id) {
        CategoryModel category = categoryDao.getCategoryById(id);

        if (category != null) {
            txtCategoryName.setText(category.getCategoryName());
            cmbIsActive.setSelectedItem(category.isActive() ? "Active" : "Inactive");
            titleLabel.setText("Edit Category: " + category.getCategoryName());
        } else {
            JOptionPane.showMessageDialog(this, "Category ID " + id + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            this.categoryId = 0;
        }
    }

    private void clearForm() {
        txtCategoryName.setText("");
        cmbIsActive.setSelectedItem("Active");
    }

    private void saveCategory() {
        String name = txtCategoryName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Category name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean isActive = Objects.equals(cmbIsActive.getSelectedItem(), "Active");
        CategoryModel categoryModel = CategoryModel.builder()
                .categoryName(txtCategoryName.getText())
                .isActive(isActive)
                .build();

        if (categoryId > 0) {
            categoryModel.setCategoryId(categoryId);
            categoryDao.updateCategory(categoryModel);
        } else {
            categoryDao.addCategory(categoryModel);
        }

        SwingUtilities.getWindowAncestor(this).dispose();
    }
}
