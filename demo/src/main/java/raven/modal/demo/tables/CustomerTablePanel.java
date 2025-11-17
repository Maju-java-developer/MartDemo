package raven.modal.demo.tables;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.utils.Constants;
import raven.modal.demo.dao.CustomerDao;
import raven.modal.demo.dao.UtilsDao;
import raven.modal.demo.forms.FormCustomer;
import raven.modal.demo.model.CustomerModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.table.TableHeaderAlignment;
import raven.swingpack.JPagination;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;


@SystemForm(name = "Customers", description = "Manage customer records", tags = {"customer", "table"})
public class CustomerTablePanel extends Form implements TableActions{

    private JTable table;
    private DefaultTableModel model;
    private CustomerDao customerDao = new CustomerDao();
    private JPagination pagination;
    private JLabel lbTotal;
    private int limit = Constants.LIMIT_PER_PAGE;

    public CustomerTablePanel() {
        initUI();
    }

    @Override
    public void formOpen() {
        loadCustomers(1);
    }

    private void initUI() {
        setLayout(new MigLayout("fillx,wrap,insets 15 0 10 0", "[fill]", "[][fill,grow][]"));

        JLabel title = new JLabel("Customer List");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");
        add(title, "gapx 20");

        model = new DefaultTableModel(Constants.customerColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Only the Action column is editable (for button clicks)
                return col == (Constants.customerColumns.length - 1);
            }
        };


        // Table
        table = new JTable(model);

        // alignment table header
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table) {
            @Override
            protected int getAlignment(int column) {
                if (column == 1 || column == (Constants.customerColumns.length - 1)) {
                    return SwingConstants.CENTER;
                }
                return SwingConstants.LEADING;
            }
        });

        // Set renderer & editor for Action column
        int getLastOneColumn = (table.getColumnCount() -1);
        table.getColumnModel().getColumn(0).setMaxWidth(30);
        table.getColumnModel().getColumn(getLastOneColumn).setCellRenderer(new TableActionCellRenderer(tableActions()));
        table.getColumnModel().getColumn(getLastOneColumn).setCellEditor(new TableActionCellEditor(table, tableActions()));

        // Optional: set fixed width for Action column
        table.getColumnModel().getColumn(getLastOneColumn).setPreferredWidth(200);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;"
                + "hoverBackground:null;"
                + "pressedBackground:null;"
                + "separatorColor:$TableHeader.background;");
        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:30;"
                + "showHorizontalLines:true;"
                + "intercellSpacing:0,1;"
                + "cellFocusColor:$TableHeader.hoverBackground;"
                + "selectionBackground:$TableHeader.hoverBackground;"
                + "selectionForeground:$Table.foreground;");

        add(scroll);

        // Pagination
        pagination = new JPagination(11, 1, 1);
        pagination.addChangeListener(e -> loadCustomers(pagination.getSelectedPage()));
        JPanel pagePanel = new JPanel(new MigLayout("insets 5 15 5 15", "[][]push[]"));
        pagePanel.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        lbTotal = new JLabel("0");
        pagePanel.add(new JLabel("Total:"));
        pagePanel.add(lbTotal);
        pagePanel.add(pagination);
        add(pagePanel);
    }

    private void loadCustomers(int page) {
        model.setRowCount(0);
        int offset = (page - 1) * limit;

        List<CustomerModel> suppliers = customerDao.getAllCustomers(offset, limit);
        int totalCustomers = UtilsDao.getCount("tblcustomers");

        int count = offset + 1;
        for (CustomerModel customerModel : suppliers) {
            model.addRow(new Object[]{
                    customerModel.getCustomerId(),
                    customerModel.getCustomerName(),
                    customerModel.getContactNo(),
                    customerModel.getEmail(),
                    customerModel.getAddress(),
                    customerModel.getOpeningBalance(),
                    customerModel.getTaxPer()
            });
        }

        lbTotal.setText(DecimalFormat.getInstance().format(totalCustomers));
        int totalPages = (int) Math.ceil((double) totalCustomers / limit);
        pagination.setSelectedPage(page);
        pagination.getModel().setPageRange(page, totalPages);
    }

    @Override
    public void formInit() {
        loadCustomers(1);
    }

    @Override
    public void formRefresh() {
        loadCustomers(pagination.getSelectedPage());
    }

    @Override
    public ActionItem[] tableActions() {
        // 1. Define the action logic using ActionItem
        return new ActionItem[]{
                new ActionItem("Payment", (table1, row) -> {
                    String customerName = table1.getValueAt(row, 1).toString();
                    JOptionPane.showMessageDialog(table1, "Add Payment for " + customerName);
                }),
                new ActionItem("Edit", (table1, row) -> {
                    int customerId = (int) table1.getValueAt(row, 0);
                    editFormPurchaseModal(customerId); // Open form in EDIT mode
                }),
                new ActionItem("Delete", (table1, row) -> {
                    int customerId = (int) table1.getValueAt(row, 0);
                    String customerName = table1.getValueAt(row, 1).toString();

                    int confirm = JOptionPane.showConfirmDialog(table1,
                            "Are you sure you want to delete customer: " + customerName + "?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        if (customerDao.deleteCustomer(customerId)) { // Call DAO delete
                            JOptionPane.showMessageDialog(table1, "Customer deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                            formRefresh(); // Refresh table after successful deletion
                        } else {
                            JOptionPane.showMessageDialog(table1, "Database error!", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                })
        };
    }

    private void editFormPurchaseModal(int id) {
        FormCustomer formPanel = new FormCustomer(id);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                id > 0 ? "Edit Customer" : "Create New Customer",
                Dialog.ModalityType.APPLICATION_MODAL);

        dialog.setContentPane(formPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        formRefresh(); // Refresh table after form closes
    }
}
