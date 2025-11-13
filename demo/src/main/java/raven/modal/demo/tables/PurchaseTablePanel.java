package raven.modal.demo.tables;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.forms.FormViewPurchase;
import raven.modal.demo.utils.Constants;
import raven.modal.demo.dao.PurchaseDao;
import raven.modal.demo.forms.FormPurchase;
import raven.modal.demo.model.PurchaseModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.combox.JComponentUtils;
import raven.modal.demo.utils.table.TableHeaderAlignment;
import raven.swingpack.JPagination;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

@SystemForm(name = "Purchase History", description = "View and manage all recorded purchases", tags = {"purchase", "history", "table"})
public class PurchaseTablePanel extends Form implements TableActions {

    private JTable purchaseTable;
    private DefaultTableModel tableModel;
    private final PurchaseDao purchaseDao = new PurchaseDao();
    private JPagination pagination;
    private JLabel lbTotal;
    private int limit = 15; // More rows for history table

    public PurchaseTablePanel() {
        initUI();
        loadPurchaseHistory(1);
    }

    private void initUI() {
        setLayout(new MigLayout("fillx,wrap,insets 15 20 10 20", "[fill]", "[][][fill,grow][]"));

        JLabel title = new JLabel("Purchase History");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        add(title, "gaptop 5");

        // --- Table Setup ---
        setupTable();
        JScrollPane scrollPane = new JScrollPane(purchaseTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, "grow, push");

        // --- Pagination ---
        pagination = new JPagination(11, 1, 1);
        pagination.addChangeListener(e -> loadPurchaseHistory(pagination.getSelectedPage()));
        JPanel pagePanel = new JPanel(new MigLayout("insets 5 15 5 15", "[][]push[]"));
        lbTotal = new JLabel("0");
        pagePanel.add(new JLabel("Total:"));
        pagePanel.add(lbTotal);
        pagePanel.add(pagination);
        add(pagePanel);
    }

    private void setupTable() {
        // Columns for detailed history view
//        String[] columns = {"ID", "Vendor", "Date", "Actual Amt", "Discount", "Total", "Paid", "Balance", "Actions"};
        String[] columns = {"#", "Vendor", "Date", "Total Amount", "Paid Amount", "Balance", "Actions"};

        tableModel = new DefaultTableModel(Constants.purchaseColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == (Constants.purchaseColumns.length - 1); // Actions column
            }
            // Use String for numeric columns to prevent default sorting issues
        };
        purchaseTable = new JTable(tableModel);

        // Setup the Action column
        int actionCol = (Constants.purchaseColumns.length - 1);
        purchaseTable.getColumnModel().getColumn(actionCol).setCellRenderer(new TableActionCellRenderer(tableActions()));
        purchaseTable.getColumnModel().getColumn(actionCol).setCellEditor(new TableActionCellEditor(purchaseTable, tableActions()));

        // Fix column width
        purchaseTable.getColumnModel().getColumn(0).setMaxWidth(30); // ID
        purchaseTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Vendor Name
        purchaseTable.getColumnModel().getColumn(2).setMaxWidth(80); // Date
        purchaseTable.getColumnModel().getColumn(3).setPreferredWidth(75); // Total Amount
        purchaseTable.getColumnModel().getColumn(4).setPreferredWidth(75); // Paid Amount
        purchaseTable.getColumnModel().getColumn(5).setPreferredWidth(75); // Remaining Amount
        purchaseTable.getColumnModel().getColumn(actionCol).setPreferredWidth(300);

        purchaseTable.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;"
                + "hoverBackground:null;"
                + "pressedBackground:null;"
                + "separatorColor:$TableHeader.background;");
        purchaseTable.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:30;"
                + "showHorizontalLines:true;"
                + "intercellSpacing:0,1;"
                + "cellFocusColor:$TableHeader.hoverBackground;"
                + "selectionBackground:$TableHeader.hoverBackground;"
                + "selectionForeground:$Table.foreground;");

        // Header Alignment (Align all monetary values to the center/right)
        purchaseTable.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(purchaseTable) {
            @Override
            protected int getAlignment(int column) {
                if (column == 1 || column  == (Constants.purchaseColumns.length - 1)) return SwingConstants.CENTER;
                return SwingConstants.LEADING;
            }
        });
    }

    private void loadPurchaseHistory(int page) {
        tableModel.setRowCount(0);
        int offset = (page - 1) * limit;

        List<PurchaseModel> purchases = purchaseDao.getPurchases(offset, limit);
        int totalPurchases = purchaseDao.getPurchaseCount();

        for (PurchaseModel p : purchases) {
            double balance = p.getTotalAmount() - p.getPaidAmount();

            tableModel.addRow(new Object[]{
                    p.getPurchaseID(),
                    p.getSupplierName(),
                    p.getPurchaseDate().toLocalDate(),
//                    String.format("%.2f", p.getActualAmount()),
//                    p.getDiscountType().startsWith("P")
//                            ? String.format("%.2f%%", p.getDiscountValue())
//                            : String.format("%.2f", p.getDiscountValue()),
                    String.format("%.2f", p.getTotalAmount()),
                    String.format("%.2f", p.getPaidAmount()),
                    String.format("%.2f", balance),
                    "Actions Placeholder" // Action Panel will render here
            });
        }

        lbTotal.setText(DecimalFormat.getInstance().format(totalPurchases));
        int totalPages = (int) Math.ceil((double) totalPurchases / limit);
        pagination.setSelectedPage(page);
        pagination.getModel().setPageRange(page, totalPages);
    }

    // --- Actions Implementation ---

    public void onEdit(int row) {
        int purchaseId = (int) tableModel.getValueAt(row, 0);
        openPurchaseFormModal(purchaseId);
    }

    public void onDelete(int row) {
        // Secondary action button (Used for simple view or delete confirmation)
        int purchaseId = (int) tableModel.getValueAt(row, 0);

        if (JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete Purchase ID " + purchaseId + " and all linked details?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

             purchaseDao.deletePurchase(purchaseId);
            // Placeholder:
            JOptionPane.showMessageDialog(this, "Purchase deletion logic initiated for ID: " + purchaseId, "Deletion", JOptionPane.INFORMATION_MESSAGE);
            formRefresh();
        }
    }

    private void openPurchaseFormModal(int purchaseId) {
        // Reuses the FormPurchase for editing
        FormPurchase editForm = new FormPurchase(purchaseId);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Edit Purchase #" + purchaseId,
                Dialog.ModalityType.APPLICATION_MODAL);

        dialog.setContentPane(editForm);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        formRefresh();
    }

    private void showPaymentDialog(int purchaseId, double outstandingBalance, String vendorName) {
        JPanel panel = new JPanel(new MigLayout("wrap 2, insets 15", "[right]20[grow, fill]"));
        JTextField txtPayment = new JTextField();
        txtPayment.setText(String.format("%.2f", outstandingBalance)); // Suggest full payment

        panel.add(new JLabel("Purchase ID:"));
        panel.add(new JLabel(String.valueOf(purchaseId)));
        panel.add(new JLabel("Vendor Name:"));
        panel.add(new JLabel(vendorName));
        panel.add(new JLabel("Outstanding Balance:"));
        panel.add(new JLabel(String.format("%.2f", outstandingBalance)));
        panel.add(new JLabel("Payment Amount:"));
        panel.add(txtPayment, "w 150");

        int result = JOptionPane.showConfirmDialog(this, panel, "Record Payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                double payment = Double.parseDouble(txtPayment.getText().trim());
                if (payment <= 0 || payment > outstandingBalance) {
                    JOptionPane.showMessageDialog(this, "Payment must be between 0 and the outstanding balance.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Call DAO to update purchase paid amount and supplier balance
                boolean success = purchaseDao.updateSupplierPayment(purchaseId, payment); // Assumed DAO method
                if (success) {
                    JOptionPane.showMessageDialog(this, "Payment recorded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    formRefresh(); // Refresh table
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid numeric amount.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void formInit() {
        loadPurchaseHistory(1);
    }

    @Override
    public void formRefresh() {
        loadPurchaseHistory(pagination.getSelectedPage());
    }

    @Override
    public ActionItem[] tableActions() {
        return new ActionItem[] {
                new ActionItem("View", this::onView),
                new ActionItem("Payment", (table1, row) -> {
                    int purchaseId = (int) tableModel.getValueAt(row, 0);
                    String vendorName = tableModel.getValueAt(row, 1).toString();
                    double balance = Double.parseDouble(tableModel.getValueAt(row, 5).toString());
                    showPaymentDialog(purchaseId, balance, vendorName);
                }), new ActionItem("Edit", (table1, row) -> {
                    onEdit(row);
                }), new ActionItem("Delete", (table1, row) -> {
                    onDelete(row);
                }),
        };
    }

    private void onView(JTable table, int row) {
        // Assuming PurchaseID is stored in a hidden column (e.g., column index 7)
        int purchaseId = (int) table.getValueAt(row, 0);

        // Open the FormViewPurchase in a modal dialog
        JComponentUtils.showModal(
                SwingUtilities.getWindowAncestor(this),
                new FormViewPurchase(purchaseId),
                "View Purchase Details"
        );
    }
}