package raven.modal.demo.tables;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.dao.SaleDao;
import raven.modal.demo.forms.FormSale;
import raven.modal.demo.model.SaleModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.combox.JComponentUtils;
import raven.modal.demo.utils.table.TableHeaderAlignment;
import raven.swingpack.JPagination;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.util.List;

@SystemForm(name = "Sale History", description = "View and manage all recorded sales", tags = { "sale", "history",
        "table" })
public class SaleTablePanel extends Form implements TableActions {

    private JTable saleTable;
    private DefaultTableModel tableModel;
    private final SaleDao saleDao = new SaleDao();
    private JPagination pagination;
    private JLabel lbTotal;
    private int limit = 15;

    public SaleTablePanel() {
        initUI();
    }

    @Override
    public void formOpen() {
        loadSaleHistory(1);
    }

    private void initUI() {
        setLayout(new MigLayout("fillx,wrap,insets 15 20 10 20", "[fill]", "[][][fill,grow][]"));

        JLabel title = new JLabel("Sale History");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        add(title, "gaptop 5");

        // --- Table Setup ---
        setupTable();
        JScrollPane scrollPane = new JScrollPane(saleTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, "grow, push");

        // --- Pagination ---
        pagination = new JPagination(11, 1, 1);
        pagination.addChangeListener(e -> loadSaleHistory(pagination.getSelectedPage()));
        JPanel pagePanel = new JPanel(new MigLayout("insets 5 15 5 15", "[][]push[]"));
        lbTotal = new JLabel("0");
        pagePanel.add(new JLabel("Total:"));
        pagePanel.add(lbTotal);
        pagePanel.add(pagination);
        add(pagePanel);
    }

    private void setupTable() {
        String[] columns = { "#", "Invoice No", "Customer", "Date", "Total Amount", "Received", "Balance", "Actions" };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == (columns.length - 1); // Actions column
            }
        };
        saleTable = new JTable(tableModel);

        // Setup the Action column
        int actionCol = (columns.length - 1);
        saleTable.getColumnModel().getColumn(actionCol).setCellRenderer(new TableActionCellRenderer(tableActions()));
        saleTable.getColumnModel().getColumn(actionCol)
                .setCellEditor(new TableActionCellEditor(saleTable, tableActions()));

        // Fix column width
        saleTable.getColumnModel().getColumn(0).setMaxWidth(30); // ID
        saleTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Invoice
        saleTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Customer
        saleTable.getColumnModel().getColumn(3).setMaxWidth(100); // Date
        saleTable.getColumnModel().getColumn(4).setPreferredWidth(80); // Total
        saleTable.getColumnModel().getColumn(5).setPreferredWidth(80); // Received
        saleTable.getColumnModel().getColumn(6).setPreferredWidth(80); // Balance
        saleTable.getColumnModel().getColumn(actionCol).setPreferredWidth(200);

        saleTable.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;"
                + "hoverBackground:null;"
                + "pressedBackground:null;"
                + "separatorColor:$TableHeader.background;");
        saleTable.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:30;"
                + "showHorizontalLines:true;"
                + "intercellSpacing:0,1;"
                + "cellFocusColor:$TableHeader.hoverBackground;"
                + "selectionBackground:$TableHeader.hoverBackground;"
                + "selectionForeground:$Table.foreground;");

        // Header Alignment
        saleTable.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(saleTable) {
            @Override
            protected int getAlignment(int column) {
                if (column == 1 || column == (columns.length - 1))
                    return SwingConstants.CENTER;
                if (column >= 4 && column <= 6)
                    return SwingConstants.RIGHT;
                return SwingConstants.LEADING;
            }
        });
    }

    private void loadSaleHistory(int page) {
        tableModel.setRowCount(0);
        int offset = (page - 1) * limit;

        List<SaleModel> sales = saleDao.getSales(offset, limit);
        int totalSales = saleDao.getSaleCount();

        for (SaleModel s : sales) {
            double balance = s.getTotalAmount() - s.getReceivedAmount();

            tableModel.addRow(new Object[] {
                    s.getSaleID(),
                    s.getInvoiceNo(),
                    s.getCustomerName(),
                    s.getSaleDate().toLocalDate(),
                    String.format("%.2f", s.getTotalAmount()),
                    String.format("%.2f", s.getReceivedAmount()),
                    String.format("%.2f", balance),
                    "Actions Placeholder"
            });
        }

        lbTotal.setText(DecimalFormat.getInstance().format(totalSales));
        int totalPages = (int) Math.ceil((double) totalSales / limit);
        pagination.setSelectedPage(page);
        pagination.getModel().setPageRange(page, totalPages);
    }

    // --- Actions Implementation ---

    public void onEdit(int row) {
        int saleId = (int) tableModel.getValueAt(row, 0);
        openSaleFormModal(saleId);
    }

    public void onDelete(int row) {
        int saleId = (int) tableModel.getValueAt(row, 0);
        String invoiceNo = (String) tableModel.getValueAt(row, 1);

        if (JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete Sale #" + invoiceNo + " and all linked details?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

            // Fetch model for deletion (needed for reversal logic in SP)
            SaleModel sale = saleDao.getSaleById(saleId);
            if (sale != null) {
                saleDao.handleSaleCRUD(sale, "Delete");
                JOptionPane.showMessageDialog(this, "Sale deleted successfully.", "Deletion",
                        JOptionPane.INFORMATION_MESSAGE);
                formRefresh();
            } else {
                JOptionPane.showMessageDialog(this, "Error: Sale not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openSaleFormModal(int saleId) {
        JComponentUtils.showModal(
                SwingUtilities.getWindowAncestor(this),
                new FormSale(saleId),
                "Edit Sale #" + saleId
        );
        formRefresh();
    }

    @Override
    public void formInit() {
        loadSaleHistory(1);
    }

    @Override
    public void formRefresh() {
        loadSaleHistory(pagination.getSelectedPage());
    }

    @Override
    public ActionItem[] tableActions() {
        return new ActionItem[] {
                new ActionItem("Edit", (table1, row) -> {
                    onEdit(row);
                }),
                new ActionItem("Delete", (table1, row) -> {
                    onDelete(row);
                }),
        };
    }
}
