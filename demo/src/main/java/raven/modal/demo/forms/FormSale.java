package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.Constants;
import raven.modal.demo.dao.CustomerDao;
import raven.modal.demo.dao.ProductDao;
import raven.modal.demo.dao.SaleDao;
import raven.modal.demo.model.CustomerModel;
import raven.modal.demo.model.ProductModel;
import raven.modal.demo.model.SaleDetailModel;
import raven.modal.demo.model.SaleModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.tables.ActionItem;
import raven.modal.demo.tables.TableActionCellEditor;
import raven.modal.demo.tables.TableActionCellRenderer;
import raven.modal.demo.tables.TableActions;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.combox.ComboBoxUtils;
import raven.modal.demo.utils.combox.InvoiceUtil;
import raven.modal.demo.utils.combox.JComponentUtils;
import raven.modal.demo.utils.table.TableHeaderAlignment;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SystemForm(name = "New Sale", description = "Record new customer sales and track inventory", tags = {"sale", "form", "customer"})
public class FormSale extends Form implements TableActions {

    // --- Detail Row Input Components ---
    private JComboBox<ProductModel> cmbProductSearch;
    private JTextField txtCartons, txtUnits, txtUnitPrice, txtDiscountLine;
    private JButton btnAdd;

    // --- Table Components ---
    private JTable detailTable;
    private DefaultTableModel detailModel;

    // --- Footer/Header Components ---
    private JComboBox<CustomerModel> cmbCustomer;
    private JComboBox<String> cmbDiscountType;
    private JTextArea txtComment;
    private JTextField txtActualAmount, txtDiscountValue, txtTotalAmount, txtReceivingAmount;
    private JTextField txtGSTRate, txtGSTAmount; // New GST fields
    private JButton btnSave, btnSaveAndPrint, btnClose;

    private int saleId = 0;
    private final ProductDao productDao = new ProductDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final SaleDao saleDao = new SaleDao(); // Need to create this DAO

    private ProductModel selectedProduct;
    private List<ProductModel> allProductsCache;

    public FormSale(int saleId) {
        this.saleId = saleId;
        init();
        loadInitialData();
        if (this.saleId > 0) {
            // loadSaleData(this.saleId); // TODO: Implement Edit Load
        }
    }

    public FormSale() {
        this(0);
    }

    private void init() {
        setLayout(new MigLayout("wrap, fillx, insets 15", "[fill]"));

        JLabel title = new JLabel("New Sale");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        add(title, "gapy 0 10");

        add(createDetailInputPanel(), "gapy 0");
        add(createDetailTablePanel(), "gapy 0, grow, push");
        add(createFooterPanel(), "gapy 10");

        setupDetailTableModel();
        setupInputListeners();
    }

    // --- UI Component Creation ---

    private JPanel createDetailInputPanel() {
        // Layout: [Product][Cartons][Units][Unit Price][Discount][Add Button]
        JPanel panel = new JPanel(new MigLayout("wrap 6, fillx, insets 0", "[grow, 300][80][80][100][80][80]", ""));

        cmbProductSearch = new JComboBox<>();
        txtCartons = new JTextField("0");
        JComponentUtils.setNumberOnly(txtCartons);
        txtUnits = new JTextField("0");
        JComponentUtils.setNumberOnly(txtUnits);
        txtUnitPrice = new JTextField("0.00");
        JComponentUtils.setNumberOnly(txtUnitPrice);
        txtDiscountLine = new JTextField("0"); // New Discount Field
        JComponentUtils.setNumberOnly(txtDiscountLine);
        btnAdd = new JButton("Add");
        btnAdd.setBackground(new Color(50, 150, 250));
        btnAdd.setForeground(Color.WHITE);

        cmbProductSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter product name or code");
        cmbProductSearch.setEditable(true);

        panel.add(new JLabel("Product"));
        panel.add(new JLabel("Cartons"));
        panel.add(new JLabel("Units"));
        panel.add(new JLabel("Unit Price"));
        panel.add(new JLabel("Discount"));
        panel.add(new JLabel(""));

        panel.add(cmbProductSearch, "h 30!, growx");
        panel.add(txtCartons, "h 30!, growx");
        panel.add(txtUnits, "h 30!, growx");
        panel.add(txtUnitPrice, "h 30!, growx");
        panel.add(txtDiscountLine, "h 30!, growx");
        panel.add(btnAdd, "h 30!, growx");

        btnAdd.addActionListener(this::addProductDetailRow);
        setupProductSearchCombo();

        return panel;
    }

    private JPanel createDetailTablePanel() {
        JPanel panel = new JPanel(new MigLayout("wrap, fillx, insets 0", "[fill]"));

        detailTable = new JTable();
        JScrollPane scroll = new JScrollPane(detailTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(new JLabel("Sale Items"), "gapy 10 5");
        panel.add(scroll, "h 200, grow, push");

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap 2, fillx, insets 0", "[grow, fill]15[400, fill]"));

        // Left Column (Customer & Comments)
        JPanel leftPanel = new JPanel(new MigLayout("wrap 1, fillx, insets 0", "[fill]"));
        cmbCustomer = new JComboBox<>();
        txtComment = new JTextArea(3, 20);
        txtComment.setLineWrap(true);
        txtComment.setWrapStyleWord(true);
        JScrollPane scrollComment = new JScrollPane(txtComment);

        leftPanel.add(new JLabel("Customer"));
        leftPanel.add(cmbCustomer, "h 30!");
        leftPanel.add(new JLabel("Comment"));
        leftPanel.add(scrollComment, "h 60!");

        // Right Column (Summary: Actual, GST, Discount, Total, Received)
        JPanel rightPanel = new JPanel(new MigLayout("wrap 6, fillx, insets 0", "[right][60][right][60][right][60]", ""));
        txtActualAmount = new JTextField("0.00");
        txtGSTRate = new JTextField(String.format("%.2f", Constants.DEFAULT_GST_RATE));
        txtGSTAmount = new JTextField("0.00");
        cmbDiscountType = new JComboBox<>();
        txtDiscountValue = new JTextField("0.00");
        JComponentUtils.setNumberOnly(txtDiscountValue);
        txtTotalAmount = new JTextField("0.00");
        txtReceivingAmount = new JTextField("0.00");
        JComponentUtils.setNumberOnly(txtReceivingAmount);

        txtActualAmount.setEditable(false);
        txtGSTAmount.setEditable(false);
        txtTotalAmount.setEditable(false);
        txtGSTRate.setEditable(false); // Typically fixed

        // Layout rows based on the design image
        rightPanel.add(new JLabel("Actual Amount"));
        rightPanel.add(txtActualAmount, "span 5, h 30!, w 120"); // Span to align with GST

        rightPanel.add(new JLabel("GST (%)"));
        rightPanel.add(txtGSTRate, "h 30!, w 120");
        rightPanel.add(new JLabel("GST Amount"));
        rightPanel.add(txtGSTAmount, "span 3, h 30!, w 120");

        rightPanel.add(new JLabel("Discount Type"));
        rightPanel.add(cmbDiscountType, "h 30!");
        rightPanel.add(new JLabel("Discount"));
        rightPanel.add(txtDiscountValue, "span 3, h 30!");

        rightPanel.add(new JLabel("Total Amount"));
        rightPanel.add(txtTotalAmount, "span 5, h 30!, w 120");

        rightPanel.add(new JLabel("Receiving Amount"));
        rightPanel.add(txtReceivingAmount, "span 5, h 30!, w 120");


        // Button Row
        JPanel buttonPanel = new JPanel(new MigLayout("right, insets 10 0 0 0", "[][][]"));
        btnClose = new JButton("Close");
        btnSave = new JButton("Save");
        btnSaveAndPrint = new JButton("Save & Print");

        btnClose.setBackground(new Color(200, 50, 50));
        btnClose.setForeground(Color.WHITE);
        btnSave.setBackground(new Color(50, 150, 250));
        btnSave.setForeground(Color.WHITE);
        btnSaveAndPrint.setBackground(new Color(50, 200, 50));
        btnSaveAndPrint.setForeground(Color.WHITE);

        btnSave.addActionListener(e -> saveSale(false));
        btnSaveAndPrint.addActionListener(e -> saveSale(true));
        btnClose.addActionListener(e -> SwingUtilities.getWindowAncestor(this).dispose());

        buttonPanel.add(btnClose);
        buttonPanel.add(btnSave);
        buttonPanel.add(btnSaveAndPrint);

        // Assemble two main columns
        panel.add(leftPanel);
        panel.add(rightPanel);
        panel.add(buttonPanel, "span 2, align right");

        return panel;
    }

    // --- Data and Table Setup ---

    private void loadInitialData() {
        // Load Customers for Customer dropdown
        List<CustomerModel> customers = customerDao.getActiveCustomersForDropdown();
        cmbCustomer.setModel(new DefaultComboBoxModel<>(customers.toArray(new CustomerModel[0])));
        ComboBoxUtils.setupComboBoxRenderer(cmbCustomer, model -> ((CustomerModel)model).getCustomerName());

        cmbDiscountType.setModel(new DefaultComboBoxModel<>(Constants.DISCOUNT_TYPES));

        // Load all products for the searchable combo box
        allProductsCache = productDao.getAllActiveProducts();
        // The combo box model will be dynamically populated by performSearch/setupProductSearchCombo
    }

    private void setupDetailTableModel() {
        // New columns: Discount is added, Total is calculated differently (after line discount)
        String[] columns = {"Sr#", "Product", "Qty", "Unit Price", "Discount", "Total", "Action", "ProductID", "PeckingTypeID"};

        detailModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 6; // Action column
            }
        };

        detailTable.setModel(detailModel);

        // Setup the Action column (Index 6)
        detailTable.getColumnModel().getColumn(6).setCellRenderer(new TableActionCellRenderer(tableActions()));
        detailTable.getColumnModel().getColumn(6).setCellEditor(new TableActionCellEditor(detailTable, tableActions()));

        // Hide internal columns
        detailTable.getColumnModel().getColumn(7).setMinWidth(0);
        detailTable.getColumnModel().getColumn(7).setMaxWidth(0);
        detailTable.getColumnModel().getColumn(8).setMinWidth(0);
        detailTable.getColumnModel().getColumn(8).setMaxWidth(0);

        // Style and Alignment
        detailTable.getColumnModel().getColumn(0).setMaxWidth(30);
        detailTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        detailTable.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(detailTable) {
            @Override
            protected int getAlignment(int column) {
                if (column >= 2 && column <= 5) return SwingConstants.RIGHT; // Qty, Price, Discount, Total
                if (column == 6) return SwingConstants.CENTER;
                return SwingConstants.LEADING;
            }
        });
        detailTable.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;"
                + "hoverBackground:null;"
                + "pressedBackground:null;"
                + "separatorColor:$TableHeader.background;");
        detailTable.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:30;"
                + "showHorizontalLines:true;"
                + "intercellSpacing:0,1;"
                + "cellFocusColor:$TableHeader.hoverBackground;"
                + "selectionBackground:$TableHeader.hoverBackground;"
                + "selectionForeground:$Table.foreground;");

    }

    // --- Detail Row Logic ---

    private void setupProductSearchCombo() {
        // Setup listener for selection change
        cmbProductSearch.addActionListener(e -> {
            if (cmbProductSearch.getSelectedItem() instanceof ProductModel) {
                selectedProduct = (ProductModel) cmbProductSearch.getSelectedItem();
                // Optional: Pre-fill rate/price if available in ProductModel
            } else {
                selectedProduct = null;
            }
        });

        // Setup listener for search input in the JComboBox editor
        JTextField editor = (JTextField) cmbProductSearch.getEditor().getEditorComponent();
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ENTER && e.getKeyCode() != KeyEvent.VK_DOWN && e.getKeyCode() != KeyEvent.VK_UP) {
                    performSearch(editor.getText());
                }
            }
        });
    }

    private void performSearch(String query) {
        // Filtering against the cached list (local filtering)
        List<ProductModel> results = allProductsCache.stream()
                .filter(p -> p.getProductName().toLowerCase().contains(query.toLowerCase()) ||
                        p.getProductCode().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        JTextField editor = (JTextField) cmbProductSearch.getEditor().getEditorComponent();
        String currentText = editor.getText();

        SwingUtilities.invokeLater(() -> {
            DefaultComboBoxModel<ProductModel> model = new DefaultComboBoxModel<>(results.toArray(new ProductModel[0]));
            cmbProductSearch.setModel(model);
            editor.setText(currentText);
            if (results.size() > 0 && !cmbProductSearch.isPopupVisible()) {
                cmbProductSearch.showPopup();
            }
        });
    }

    private void addProductDetailRow(ActionEvent e) {
        if (selectedProduct == null || selectedProduct.getProductId() == 0) {
            JOptionPane.showMessageDialog(this, "Please select a product.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- Input Validation and Calculation ---
        double cartons = 0, units = 0, unitPrice = 0, lineDiscount = 0;
        try {
            cartons = Double.parseDouble(txtCartons.getText().trim());
            units = Double.parseDouble(txtUnits.getText().trim());
            unitPrice = Double.parseDouble(txtUnitPrice.getText().trim());
            lineDiscount = Double.parseDouble(txtDiscountLine.getText().trim());

            if (unitPrice <= 0 || (cartons <= 0 && units <= 0)) {
                throw new NumberFormatException("Invalid price or quantity.");
            }
            if (lineDiscount < 0 || lineDiscount > (unitPrice * 100)) { // Simple check
                throw new NumberFormatException("Invalid discount value.");
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity, unit price, or discount.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // TODO: CRITICAL STOCK CHECK (Placeholder)
//         double availableStock = saleDao.getAvailableStock(selectedProduct.getProductId());
//         if (totalQuantity > availableStock) {
//             JOptionPane.showMessageDialog(this, "Insufficient stock: Only " + availableStock + " available.", "Stock Error", JOptionPane.ERROR_MESSAGE);
//             return;
//         }

        int unitsPerCarton = selectedProduct.getUnitsPerCarton();
        double totalQuantity = (cartons * unitsPerCarton) + units;

        // Calculate total price after discount
        double grossPrice = totalQuantity * unitPrice;
        double netPrice = grossPrice - lineDiscount;
        if (netPrice < 0) netPrice = 0;

        // --- Add Row ---
        detailModel.addRow(new Object[]{
                detailModel.getRowCount() + 1, // Sr#
                selectedProduct.getProductName(),
                totalQuantity,
                unitPrice,
                String.format("%.2f", lineDiscount),
                String.format("%.2f", netPrice),
                "Action Placeholder",
                selectedProduct.getProductId(),
                selectedProduct.getPeckingTypeId()
        });

        // Clear and update
        clearDetailInput();
        updateActualAmount();
    }

    private void clearDetailInput() {
        JComponentUtils.resetTextField(txtCartons, "0");
        JComponentUtils.resetTextField(txtUnits, "0");
        JComponentUtils.resetTextField(txtUnitPrice, "0.00");
        JComponentUtils.resetTextField(txtDiscountLine, "0");
        ((JTextField) cmbProductSearch.getEditor().getEditorComponent()).setText("");
        selectedProduct = null;
    }

    private void updateActualAmount() {
        // Actual Amount is the sum of 'Total' column (Index 5) in the detail table
        double actualAmount = 0.0;
        for (int i = 0; i < detailModel.getRowCount(); i++) {
            try {
                // Total column is at index 5 (Net Price after line discount)
                actualAmount += Double.parseDouble(detailModel.getValueAt(i, 5).toString());
            } catch (NumberFormatException ignored) {}
        }
        txtActualAmount.setText(String.format("%.2f", actualAmount));
        updateFinalTotals(actualAmount);
    }

    private void updateFinalTotals(double amountBeforeDiscount) {
        // 1. Apply Header Discount
        String type = (String) cmbDiscountType.getSelectedItem();
        double discountValue = 0.0;
        try {
            discountValue = Double.parseDouble(txtDiscountValue.getText().trim());
        } catch (NumberFormatException ignored) {}

        double finalDiscount = 0.0;
        if (Constants.DISCOUNT_TYPE_PERCENTAGE.equals(type)) {
            finalDiscount = amountBeforeDiscount * (discountValue / 100.0);
        } else if (Constants.DISCOUNT_TYPE_FIXED.equals(type)) {
            finalDiscount = discountValue;
        }

        double amountAfterDiscount = amountBeforeDiscount - finalDiscount;
        if (amountAfterDiscount < 0) amountAfterDiscount = 0;

        // 2. Apply GST
        double gstRate = Constants.DEFAULT_GST_RATE;
        // GST is applied to the net amount (Amount After Discount)
        double gstAmount = amountAfterDiscount * (gstRate / 100.0);

        double finalTotal = amountAfterDiscount + gstAmount;

        // 3. Update Fields
        txtGSTAmount.setText(String.format("%.2f", gstAmount));
        txtTotalAmount.setText(String.format("%.2f", finalTotal));

        // Ensure Receiving Amount doesn't exceed the new total if it was set higher before calculation
        validateReceivingAmount();
    }

    private void validateReceivingAmount() {
        try {
            double totalAmount = Double.parseDouble(txtTotalAmount.getText().trim());
            double receivedAmount = Double.parseDouble(txtReceivingAmount.getText().trim());

            if (receivedAmount < 0) {
                txtReceivingAmount.setText("0.00");
            }
            else if (receivedAmount > totalAmount) {
                txtReceivingAmount.setText(String.format("%.2f", totalAmount));
            }
        } catch (NumberFormatException ignored) {}
    }


    // --- Save and Action Logic ---

    private void saveSale(boolean print) {
        // 1. Validation and Data Mapping (similar to FormPurchase)
        CustomerModel selectedCustomer = (CustomerModel) cmbCustomer.getSelectedItem();
        if (selectedCustomer == null || selectedCustomer.getCustomerId() == 0) {
            JOptionPane.showMessageDialog(this, "Please select a Customer.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (detailModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Please add at least one item to the sale.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Safely parse amounts
        double actualAmount, totalAmount, receivedAmount, discountValue;
        try {
            actualAmount = Double.parseDouble(txtActualAmount.getText().trim());
            totalAmount = Double.parseDouble(txtTotalAmount.getText().trim());
            receivedAmount = Double.parseDouble(txtReceivingAmount.getText().trim());
            discountValue = Double.parseDouble(txtDiscountValue.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number format detected in summary fields.", "Data Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Map details from JTable to List
        List<SaleDetailModel> details = new ArrayList<>();
        for (int i = 0; i < detailModel.getRowCount(); i++) {
            try {
                details.add(SaleDetailModel.builder()
                        .productID((int) detailModel.getValueAt(i, 7)) // Index 7: Hidden ProductID
                        .quantity(Double.parseDouble(detailModel.getValueAt(i, 2).toString())) // Index 2: Total Qty
                        .rate(Double.parseDouble(detailModel.getValueAt(i, 3).toString())) // Index 3: Unit Price
                        .lineDiscount(Double.parseDouble(detailModel.getValueAt(i, 4).toString())) // Index 4: Line Discount
                        .total(Double.parseDouble(detailModel.getValueAt(i, 5).toString())) // Index 5: Net Price
                        .build());

            } catch (NumberFormatException | ClassCastException e) {
                JOptionPane.showMessageDialog(this, "Error in detail table data (Row " + (i + 1) + "). Please re-add the item.", "Data Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Create SaleModel
        SaleModel saleModel = SaleModel.builder()
                .saleID(saleId)
                .customerID(selectedCustomer.getCustomerId())
                .saleDate(LocalDateTime.now())
                .invoiceNo(InvoiceUtil.generateInvoiceNumber()) // Use utility or capture from UI
                .actualAmount(actualAmount) // This is the net value before header discount and GST
                .discountType((String) cmbDiscountType.getSelectedItem())
                .discountValue(discountValue)
                .totalAmount(totalAmount) // Final amount including GST
                .receivedAmount(receivedAmount)
                .remarks(Objects.toString(txtComment.getText(), ""))
                .details(details)
                .build();

        if (saleId > 0) {
             saleDao.updateSale(saleModel);
        } else {
            saleDao.saveSale(saleModel);
            JOptionPane.showMessageDialog(this, "New Sale has been done!", "Success Sale", JOptionPane.INFORMATION_MESSAGE);
            clearDetailInput();
        }

    }

    private void setupInputListeners() {
        // Listener for Detail Row Total Calculation
        KeyAdapter detailKeyAdapter = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                // calculateDetailRowTotal(); // Helper method to update the row price fields (if any)
            }
        };

        txtCartons.addKeyListener(detailKeyAdapter);
        txtUnits.addKeyListener(detailKeyAdapter);
        txtUnitPrice.addKeyListener(detailKeyAdapter);
        txtDiscountLine.addKeyListener(detailKeyAdapter); // New!

        // Listeners for Final Summary Calculation (Discount Type, Discount Value)
        cmbDiscountType.addActionListener(e -> updateActualAmount());
        txtDiscountValue.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateActualAmount();
            }
        });

        // Listener for Received Amount Validation
        txtReceivingAmount.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                validateReceivingAmount();
            }
        });
    }

    @Override
    public ActionItem[] tableActions() {
        // The actions are the same as Purchase: Edit and Delete line items
        return new ActionItem[]{
//                new ActionItem("Edit", (table1, row) -> {
//                    // TODO: Implement Edit line item logic (similar to FormPurchase)
//                    // 1. Get ProductID, Qty, Price, Discount
//                    // 2. Calculate Cartons/Units
//                    // 3. Populate top input fields (cmbProductSearch, txtCartons, etc.)
//                    // 4. Delete the original row
//                }),
                new ActionItem("Delete", (table1, row) -> {
                    if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this line item?", "Confirm Deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        onDelete(row);
                    }
                })
        };
    }

    public void onDelete(int row) {
        detailModel.removeRow(row);
        // Re-sequence Sr# column
        for(int i = 0; i < detailModel.getRowCount(); i++) {
            detailModel.setValueAt(i + 1, i, 0);
        }
        updateActualAmount(); // Recalculate totals
    }
}