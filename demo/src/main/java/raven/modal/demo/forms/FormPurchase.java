package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.utils.Constants;
import raven.modal.demo.dao.ProductDao;
import raven.modal.demo.dao.PurchaseDao;
import raven.modal.demo.dao.SupplierDao;
import raven.modal.demo.model.ProductModel;
import raven.modal.demo.model.PurchaseDetailModel;
import raven.modal.demo.model.PurchaseModel;
import raven.modal.demo.model.SupplierModel;
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

@SystemForm(name = "Purchase Form", description = "Record and edit new inventory purchases", tags = {"purchase", "form", "inventory"})
public class FormPurchase extends Form implements TableActions {

    // --- Detail Row Input Components ---
    private JComboBox<ProductModel> cmbProductSearch; // Searchable Product Field
    private JTextField txtCartons, txtUnits, txtUnitPrice;
    private JButton btnAdd;

    // --- Table Components ---
    private JTable detailTable;
    private DefaultTableModel detailModel;

    // --- Footer/Header Components ---
    private JComboBox<SupplierModel> cmbVendor;
    private JComboBox<String> cmbDiscountType;
    private JTextArea txtComment;
    private JTextField txtActualAmount, txtDiscountValue, txtTotalAmount, txtPayingAmount;
    private JButton btnSave, btnSaveAndPrint, btnClose;

    private int purchaseId = 0;
    private final ProductDao productDao = new ProductDao();
    private final SupplierDao supplierDao = new SupplierDao();
    private final PurchaseDao purchaseDao = new PurchaseDao();

    // Current product selected in the detail input bar
    private ProductModel selectedProduct;

    public FormPurchase(int purchaseId) {
        this.purchaseId = purchaseId;
        init();
        loadInitialData();
        if (this.purchaseId > 0) {
            loadPurchaseData(this.purchaseId);
        }
    }

    public FormPurchase() {
        this(0);
    }

    private void init() {
        setLayout(new MigLayout("wrap, fillx, insets 15", "[fill]"));

        // Use a simple title matching the design image
        JLabel title = new JLabel("New Purchase");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        add(title, "gapy 0 10");

        add(createDetailInputPanel(), "gapy 0");
        add(createDetailTablePanel(), "gapy 0, grow, push");
        add(createFooterPanel(), "gapy 10");

        // Setup table model
        detailModel = new DefaultTableModel(new Object[]{"Sr#", "Product", "Qty", "Unit Price", "Total", "Action", "ProductID", "PeckingTypeID"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Only Action column is editable
                return col == 5;
            }
        };

        // alignment table header
        detailTable.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(detailTable) {
            @Override
            protected int getAlignment(int column) {
                if (column == 1 || column == 5) {
                    return SwingConstants.CENTER;
                }
                return SwingConstants.LEADING;
            }
        });

        detailTable.setModel(detailModel);

        // Set renderer & editor for Action column
        detailTable.getColumnModel().getColumn(0).setMaxWidth(30);
        detailTable.getColumnModel().getColumn(5).setCellRenderer(new TableActionCellRenderer(tableActions()));
        detailTable.getColumnModel().getColumn(5).setCellEditor(new TableActionCellEditor(detailTable, tableActions()));

        // Optional: set fixed width for Action column
        detailTable.getColumnModel().getColumn(5).setPreferredWidth(200);

        // Hide internal columns
        detailTable.getColumnModel().getColumn(6).setMinWidth(0);
        detailTable.getColumnModel().getColumn(6).setMaxWidth(0);
        detailTable.getColumnModel().getColumn(7).setMinWidth(0);
        detailTable.getColumnModel().getColumn(7).setMaxWidth(0);

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

        setupInputListeners();
    }

    // --- Initial Data Loading ---

    private void loadInitialData() {
        // Load Suppliers for Vendor dropdown
        List<SupplierModel> suppliers = supplierDao.getActiveSuppliersForDropdown();
        cmbVendor.setModel(new DefaultComboBoxModel<>(suppliers.toArray(new SupplierModel[0])));
        // Utility to render model objects by name
        ComboBoxUtils.setupComboBoxRenderer(cmbVendor, model -> ((SupplierModel)model).getSupplierName());

        cmbDiscountType.setModel(new DefaultComboBoxModel<>(Constants.DISCOUNT_TYPES));
    }

    // --- UI Panel Creation Methods ---

    private JPanel createDetailInputPanel() {
        // Layout: [Product][Cartons][Units][Unit Price][Add Button]
        JPanel panel = new JPanel(new MigLayout("wrap 5, fillx, insets 0", "[grow, 400][100][100][100][grow, 80]", ""));

        cmbProductSearch = new JComboBox<>();
        txtCartons = new JTextField("0");
        JComponentUtils.setNumberOnly(txtCartons);
        txtUnits = new JTextField("0");
        JComponentUtils.setNumberOnly(txtUnits);
        txtUnitPrice = new JTextField("0.00");
        JComponentUtils.setNumberOnly(txtUnitPrice);
        btnAdd = new JButton("Add");
        btnAdd.setBackground(new Color(50, 150, 250));
        btnAdd.setForeground(Color.WHITE);

        cmbProductSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter product name or code");
        cmbProductSearch.setEditable(true);

        panel.add(new JLabel("Product"));
        panel.add(new JLabel("Cartons"));
        panel.add(new JLabel("Units"));
        panel.add(new JLabel("Unit Price"));
        panel.add(new JLabel("")); // Placeholder for Add button label

        panel.add(cmbProductSearch, "h 30!, wmin 150, growx");
        panel.add(txtCartons, "h 30!, wmin 100");
        panel.add(txtUnits, "h 30!, wmin 100");
        panel.add(txtUnitPrice, "h 30!, wmin 100");
        panel.add(btnAdd, "h 30!, wmin 100");

        btnAdd.addActionListener(this::addProductDetailRow);
        setupProductSearchCombo(); // Setup searching capability

        return panel;
    }

    private JPanel createDetailTablePanel() {
        JPanel panel = new JPanel(new MigLayout("wrap, fillx, insets 0", "[fill]"));

        detailTable = new JTable();
        JScrollPane scroll = new JScrollPane(detailTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(new JLabel("Purchase Items"), "gapy 10 5");
        panel.add(scroll, "h 200, grow, push");

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new MigLayout("wrap 2, fillx, insets 0", "[grow, fill]15[400, fill]"));

        // Left Column (Vendor & Comments)
        JPanel leftPanel = new JPanel(new MigLayout("wrap 1, fillx, insets 0", "[fill]"));
        cmbVendor = new JComboBox<>();
        txtComment = new JTextArea(3, 20);
        txtComment.setLineWrap(true);
        txtComment.setWrapStyleWord(true);
        JScrollPane scrollComment = new JScrollPane(txtComment);

        leftPanel.add(new JLabel("Vendor"));
        leftPanel.add(cmbVendor, "h 30!");
        leftPanel.add(new JLabel("Comment"));
        leftPanel.add(scrollComment, "h 60!");

        // Right Column (Summary & Discount)
        JPanel rightPanel = new JPanel(new MigLayout("wrap 4, fillx, insets 0", "[right][80][right][80]", ""));
        txtActualAmount = new JTextField("0");
        cmbDiscountType = new JComboBox<>();
        txtDiscountValue = new JTextField("0");
        JComponentUtils.setNumberOnly(txtDiscountValue);
        txtTotalAmount = new JTextField("0");
        txtPayingAmount = new JTextField("0");
        JComponentUtils.setNumberOnly(txtPayingAmount);

        txtActualAmount.setEditable(false);
        txtTotalAmount.setEditable(false);

        rightPanel.add(new JLabel("Actual Amount"));
        rightPanel.add(txtActualAmount, "span 3, w 120"); // Span for visual alignment

        rightPanel.add(new JLabel("Discount Type"));
        rightPanel.add(cmbDiscountType, "h 30!");
        rightPanel.add(new JLabel("Discount"));
        rightPanel.add(txtDiscountValue, "h 30!");

        rightPanel.add(new JLabel("Total Amount"));
        rightPanel.add(txtTotalAmount, "span 3, w 120");

        rightPanel.add(new JLabel("Paying Amount"));
        rightPanel.add(txtPayingAmount, "span 3, w 120");

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

        btnSave.addActionListener(e -> savePurchase(false));
        btnSaveAndPrint.addActionListener(e -> savePurchase(true));

        buttonPanel.add(btnClose);
        buttonPanel.add(btnSave);
        buttonPanel.add(btnSaveAndPrint);

        // Assemble two main columns
        panel.add(leftPanel);
        panel.add(rightPanel);
        panel.add(buttonPanel, "span 2, align right");

        return panel;
    }

    private void setupProductSearchCombo() {
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

        // Listener for selection change
        cmbProductSearch.addActionListener(e -> {
            if (cmbProductSearch.getSelectedItem() instanceof ProductModel) {
                selectedProduct = (ProductModel) cmbProductSearch.getSelectedItem();
                // Optional: Pre-fill rate/price if available in ProductModel
            } else {
                selectedProduct = null;
            }
        });
    }

    private void performSearch(String query) {
        if (query.length() < 2) return;

        List<ProductModel> results = productDao.searchActiveProducts(query);

        // Store the text currently in the editor
        JTextField editor = (JTextField) cmbProductSearch.getEditor().getEditorComponent();
        String currentText = editor.getText(); // Preserve the user's input

        SwingUtilities.invokeLater(() -> {
            // Ensure the JComboBox doesn't try to auto-select the first item right away
            cmbProductSearch.setSelectedItem(null);
            DefaultComboBoxModel<ProductModel> model = new DefaultComboBoxModel<>(results.toArray(new ProductModel[0]));
            cmbProductSearch.setModel(model);
            // Restore the user's typed text to the editor AFTER setting the model
            editor.setText(currentText);
            // Re-open the popup list
            if (results.size() > 0 && !cmbProductSearch.isPopupVisible()) {
                cmbProductSearch.showPopup();
            }
        });
    }

    private void updateActualAmount() {
        double actualAmount = 0.0;
        for (int i = 0; i < detailModel.getRowCount(); i++) {
            try {
                // Total column is at index 4 in the detailModel
                actualAmount += Double.parseDouble(detailModel.getValueAt(i, 4).toString());
            } catch (NumberFormatException ignored) {}
        }
        txtActualAmount.setText(String.format("%.2f", actualAmount));
        updateFinalTotals(actualAmount);
    }

    private void updateFinalTotals(double actualAmount) {
        String type = (String) cmbDiscountType.getSelectedItem();
        double discountValue = 0.0;
        try {
            discountValue = Double.parseDouble(txtDiscountValue.getText().trim());
        } catch (NumberFormatException ignored) {}

        double finalDiscount = 0.0;
        if (Constants.DISCOUNT_TYPE_PERCENTAGE.equals(type)) {
            finalDiscount = actualAmount * (discountValue / 100.0);
        } else if (Constants.DISCOUNT_TYPE_FIXED.equals(type)) {
            finalDiscount = discountValue;
        }

        double totalAmount = actualAmount - finalDiscount;
        if (totalAmount < 0) totalAmount = 0;

        txtTotalAmount.setText(String.format("%.2f", totalAmount));
    }

    private void addProductDetailRow(ActionEvent e) {
        if (selectedProduct == null || selectedProduct.getProductId() == 0) {
            JOptionPane.showMessageDialog(this, "Please select a product.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- 1. CHECK FOR DUPLICATES ---
        int newProductId = selectedProduct.getProductId();
        for (int i = 0; i < detailModel.getRowCount(); i++) {
            // We compare the hidden ProductID (column index 6)
            int existingProductId = (int) detailModel.getValueAt(i, 6);
            if (existingProductId == newProductId) {
                JOptionPane.showMessageDialog(this,
                        "Product '" + selectedProduct.getProductName() + "' is already added in line " + (i + 1) + ". Please use the Action column to edit it.",
                        "Duplicate Item",
                        JOptionPane.WARNING_MESSAGE);
                return; // Exit the method, preventing addition
            }
        }

        // --- END DUPLICATE CHECK ---
        double cartons = 0, units = 0, unitPrice = 0;
        try {
            cartons = Double.parseDouble(txtCartons.getText().trim());
            units = Double.parseDouble(txtUnits.getText().trim());
            unitPrice = Double.parseDouble(txtUnitPrice.getText().trim());
            if (unitPrice <= 0 || (cartons <= 0 && units <= 0)) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity or unit price.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Calculate total quantity and total price
        int unitsPerCarton = selectedProduct.getUnitsPerCarton(); // Assumed to be loaded in searchActiveProducts
        double totalQuantity = (cartons * unitsPerCarton) + units;
        double totalRowPrice = totalQuantity * unitPrice;

        detailModel.addRow(new Object[]{
                detailModel.getRowCount() + 1, // Sr#
                selectedProduct.getProductName(),
                totalQuantity,
                unitPrice,
                String.format("%.2f", totalRowPrice),
                "Action Placeholder", // Action column
                selectedProduct.getProductId(), // Hidden ID
                selectedProduct.getPackingTypeId() // Hidden ID
        });

        // Clear row input fields
        JComponentUtils.resetTextField(txtCartons,"0");
        JComponentUtils.resetTextField(txtUnits,"0");
        JComponentUtils.resetTextField(txtUnitPrice, "0.00");
        ((JTextField) cmbProductSearch.getEditor().getEditorComponent()).setText("");
        selectedProduct = null;

        updateActualAmount();
    }


    private void savePurchase(boolean print) {
        // 1. Validation
        SupplierModel selectedSupplier = (SupplierModel) cmbVendor.getSelectedItem();
        if (selectedSupplier == null || selectedSupplier.getSupplierID() == 0) {
            JOptionPane.showMessageDialog(this, "Please select a Vendor.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (detailModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Please add at least one item to the purchase.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Safely parse amounts
        double actualAmount = 0.0;
        double totalAmount = 0.0;
        double paidAmount = 0.0;
        double discountValue = 0.0;
        try {
            actualAmount = Double.parseDouble(txtActualAmount.getText().trim());
            totalAmount = Double.parseDouble(txtTotalAmount.getText().trim());
            paidAmount = Double.parseDouble(txtPayingAmount.getText().trim());
            discountValue = Double.parseDouble(txtDiscountValue.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number format detected in summary fields.", "Data Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String discountType = (String) cmbDiscountType.getSelectedItem();

        // --- 2. CRITICAL STEP: Map details from JTable to List ---
        List<PurchaseDetailModel> details = new ArrayList<>();
        for (int i = 0; i < detailModel.getRowCount(); i++) {
            try {
                // The 'Unit Price' column (index 3) acts as the Rate in TBLPurchaseDetail
                details.add(PurchaseDetailModel.builder()
                        .productID((int) detailModel.getValueAt(i, 6)) // Index 6: Hidden ProductID
                        .quantity(Double.parseDouble(detailModel.getValueAt(i, 2).toString())) // Index 2: Total Qty
                        .rate(Double.parseDouble(detailModel.getValueAt(i, 3).toString())) // Index 3: Unit Price (Rate)
                        .total(Double.parseDouble(detailModel.getValueAt(i, 4).toString())) // Index 4: Total Row Price
                        .build());

            } catch (NumberFormatException | ClassCastException e) {
                JOptionPane.showMessageDialog(this, "Error in detail table data (Row " + (i + 1) + "). Please re-add the item.", "Data Error", JOptionPane.ERROR_MESSAGE);
                return; // Stop save process
            }
        }

        // 3. Create PurchaseModel
        PurchaseModel purchaseModel = PurchaseModel.builder()
                .purchaseID(purchaseId)
                .supplierID(selectedSupplier.getSupplierID())
                .purchaseDate(LocalDateTime.now())
                .invoiceNo(InvoiceUtil.generateInvoiceNumber()) // Assuming InvoiceNo is not captured, or se ua default
                .actualAmount(actualAmount)
                .discountType(discountType)
                .discountValue(discountValue)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .remarks(Objects.toString(txtComment.getText(), ""))
                .details(details) // NOW THIS IS POPULATED!
                .build();
        if (purchaseId > 0) {
             purchaseDao.updatePurchase(purchaseModel); // TODO: Implement Update
             JOptionPane.showMessageDialog(this, "Purchase Updated successfully!.", "Info", JOptionPane.INFORMATION_MESSAGE);
            // Close the form/dialog
            SwingUtilities.getWindowAncestor(this).dispose();
        } else {
            purchaseDao.savePurchase(purchaseModel, 1); // todo set user id here after ending
            clearForm();
        }

    }

    private void loadPurchaseData(int id) {
        PurchaseModel purchase = purchaseDao.getPurchaseForEdit(id);

        if (purchase == null) {
            JOptionPane.showMessageDialog(this, "Purchase ID " + id + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Set Vendor/Supplier (must match the model structure in cmbVendor)
        SupplierModel selectedSupplier = new SupplierModel(purchase.getSupplierID(), purchase.getSupplierName());
        cmbVendor.setSelectedItem(selectedSupplier);

        // Set Summary Fields
        txtActualAmount.setText(String.format("%.2f", purchase.getActualAmount()));
        cmbDiscountType.setSelectedItem(purchase.getDiscountType());
        txtDiscountValue.setText(String.format("%.2f", purchase.getDiscountValue()));
        txtTotalAmount.setText(String.format("%.2f", purchase.getTotalAmount()));
        txtPayingAmount.setText(String.format("%.2f", purchase.getPaidAmount()));
        txtComment.setText(purchase.getRemarks());

        // --- 2. Load Detail Data (Table Rows) ---

        detailModel.setRowCount(0); // Clear table before loading
        int sr = 1;
        for (PurchaseDetailModel detail : purchase.getDetails()) {

            double totalQuantity = detail.getQuantity();
            int unitsPerCarton = detail.getUnitsPerCarton();

            // Split Total Quantity into Cartons and Units for display purposes
            // (Even though the table only shows total Qty, this data is often used for other displays)
            double cartons = 0;
            // double units = totalQuantity; // This is the total quantity in base units

            if (unitsPerCarton > 0) {
                cartons = Math.floor(totalQuantity / unitsPerCarton);
                // units = totalQuantity % unitsPerCarton; // The remainder in base units
            }

            detailModel.addRow(new Object[]{
                    detailModel.getRowCount() + 1, // Sr#
                    detail.getProductName(),
                    totalQuantity, // Display Total Qty
                    detail.getRate(),
                    String.format("%.2f", detail.getTotal()),
                    "Action Placeholder",
                    detail.getProductID(), // Hidden ID
                    // NOTE: PeckingTypeId is not available in PurchaseDetailModel directly,
                    // but we can infer it or rely on the product being fully fetched during edit action.
                    // For now, let's leave it as 0 or fetch it if needed for the edit action.
                    0 // Index 7 (PeckingTypeID) is not strictly needed for display/save update
            });
        }

        // Recalculate totals after loading the table (although the saved totals should match)
        updateActualAmount();

        // Inform the user they are in edit mode
        JLabel title = (JLabel) getComponent(0); // Assuming the title is the first component
        title.setText("Edit Purchase #" + id + " (Vendor: " + purchase.getSupplierName() + ")");
    }

    public void clearForm() {
        cmbVendor.setSelectedIndex(0);
        txtComment.setText("");
        txtActualAmount.setText("0");
        cmbDiscountType.setSelectedIndex(0);
        txtDiscountValue.setText("0");
        txtTotalAmount.setText("0");
        txtPayingAmount.setText("0");
        txtUnitPrice.setText("0.00");
        detailModel.setRowCount(0);
    }

    /**
     * Calculates the total price for the current detail input row
     * based on Cartons, Units, Unit Price, and the selected product's units per carton.
     * This method is primarily used to validate and compute the values required by the
     * addDetailRow() method and to ensure immediate calculation feedback
     * (if a txtRowTotal field were present, it would update it here).
     */
    private void calculateDetailRowTotal() {
        // 1. Check for valid product selection
        if (selectedProduct == null || selectedProduct.getUnitsPerCarton() <= 0) {
            // Since there is no dedicated 'Row Total' field to update, we reset the Unit Price
            // and rely on the validation in addDetailRow().
            return;
        }

        double cartons = 0, units = 0, unitPrice = 0;

        // 2. Safely parse input values
        try {
            cartons = Double.parseDouble(txtCartons.getText().trim());
            units = Double.parseDouble(txtUnits.getText().trim());
            unitPrice = Double.parseDouble(txtUnitPrice.getText().trim());
        } catch (NumberFormatException e) {
            // If any field is invalid during typing, suppress the error and stop calculation.
            return;
        }

        int unitsPerCarton = selectedProduct.getUnitsPerCarton();

        // 3. Calculation
        // Total Quantity = (Cartons * Units Per Carton) + Loose Units
        double totalQuantity = (cartons * unitsPerCarton) + units;

        // Total Row Price = Total Quantity * Unit Price
        double totalRowPrice = totalQuantity * unitPrice;

        // --- Display Feedback (Crucial for UX) ---
        // Since the UI design does not show a 'Row Total' field in the input bar:
        //
        // If you add a JTextField named 'txtRowTotal' to the input panel:
        //
        // if (txtRowTotal != null) {
        //     txtRowTotal.setText(String.format("%.2f", totalRowPrice));
        // }

        // For now, this method simply performs the necessary calculation that will be
        // used and finalized when the 'Add' button is clicked.
    }

    private void setupInputListeners() {
        // 1. Listeners for Detail Row Total Calculation (Cartons, Units, Unit Price)

        // KeyListener to update total whenever text changes
        KeyAdapter detailKeyAdapter = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                calculateDetailRowTotal();
            }
        };

        txtCartons.addKeyListener(detailKeyAdapter);
        txtUnits.addKeyListener(detailKeyAdapter);
        txtUnitPrice.addKeyListener(detailKeyAdapter);

        // 2. Listeners for Final Summary Calculation (Discount Type, Discount Value)

        // ActionListener for JComboBox (Discount Type) changes
        cmbDiscountType.addActionListener(e -> {
            // We pass the current Actual Amount to recalculate the final total
            double actualAmount = 0.0;
            try {
                actualAmount = Double.parseDouble(txtActualAmount.getText());
            } catch (NumberFormatException ignored) {}
            updateFinalTotals(actualAmount);
        });

        // KeyListener for Discount Value changes
        KeyAdapter discountKeyAdapter = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                double actualAmount = 0.0;
                try {
                    actualAmount = Double.parseDouble(txtActualAmount.getText());
                } catch (NumberFormatException ignored) {}
                updateFinalTotals(actualAmount);
            }
        };

        txtDiscountValue.addKeyListener(discountKeyAdapter);

        // --- NEW: Listener for Paid Amount Validation ---
        KeyAdapter paidAmountKeyAdapter = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                validatePaidAmount();
            }
        };

        txtPayingAmount.addKeyListener(paidAmountKeyAdapter);
        // -----------------------------------------------

        // Ensure initial values are numeric before adding listeners
        if (txtPayingAmount.getText().isEmpty()) {
            txtPayingAmount.setText("0.00");
        }
    }

    @Override
    public ActionItem[] tableActions() {
        // 1. Define the action logic using ActionItem
        return new ActionItem[]{
//                new ActionItem("Edit", (table1, row) -> {
//                    // 1. Fetch data from the row
//                    int productId = (int) detailModel.getValueAt(row, 6);
//                    double totalQuantity = Double.parseDouble(detailModel.getValueAt(row, 2).toString());
//                    double unitPrice = Double.parseDouble(detailModel.getValueAt(row, 3).toString());
//                    String productName = detailModel.getValueAt(row, 1).toString();
//                    int peckingTypeId = (int) detailModel.getValueAt(row, 7); // Hidden PeckingTypeID
//
//                    ProductModel productToEdit = productDao.getProductById(productId);
//
//                    if (productToEdit == null) {
//                        JOptionPane.showMessageDialog(this, "Error: Could not retrieve product details for editing.", "Error", JOptionPane.ERROR_MESSAGE);
//                        return;
//                    }
//
//                    // 3. Calculate Cartons and Units for display
//                    int unitsPerCarton = productToEdit.getUnitsPerCarton();
//                    double cartons = 0;
//                    double units = totalQuantity;
//
//                    if (unitsPerCarton > 0) {
//                        // Split total quantity back into cartons and remaining units
//                        cartons = Math.floor(totalQuantity / unitsPerCarton);
//                        units = totalQuantity % unitsPerCarton;
//                    }
//
//                    DefaultComboBoxModel<ProductModel> productModel = new DefaultComboBoxModel<>(new ProductModel[]{productToEdit});
//                    cmbProductSearch.setModel(productModel);
//                    cmbProductSearch.setSelectedItem(productToEdit);
//
//                    // Set the internal tracking variable
//                    selectedProduct = productToEdit;
//
//                    // B. Set quantity and price fields
//                    JComponentUtils.resetTextField(txtCartons, String.valueOf((int)cartons));
//                    JComponentUtils.resetTextField(txtUnits, String.format("%.2f", units));
//                    JComponentUtils.resetTextField(txtUnitPrice, String.format("%.2f", unitPrice));
//
//                    // 5. Delete the original row (The user will re-add the edited data)
//                    onDelete(row); // Use the existing delete method, but suppress confirmation if possible.
//                    JOptionPane.showMessageDialog(this,
//                            "Line item '" + productName + "' loaded for editing. Adjust values and click 'Add'.",
//                            "Edit Mode",
//                            JOptionPane.INFORMATION_MESSAGE);
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

    /**
     * Ensures the amount entered in txtPayingAmount does not exceed txtTotalAmount.
     */
    private void validatePaidAmount() {
        try {
            double totalAmount = Double.parseDouble(txtTotalAmount.getText().trim());
            double paidAmount = Double.parseDouble(txtPayingAmount.getText().trim());

            if (paidAmount > totalAmount) {
                // Option 1: Limit the input to the total amount
                txtPayingAmount.setText(String.format("%.2f", totalAmount));
                // Option 2 (Less disruptive): Allow the user to type, but flag it later during save validation.
                // We'll stick to Option 1 for immediate user correction.
            } else if (paidAmount < 0) {
                // Prevent negative payments
                txtPayingAmount.setText("0.00");
            }
        } catch (NumberFormatException ignored) {
            // Ignore if the user is in the middle of typing an invalid number
        }
    }
}