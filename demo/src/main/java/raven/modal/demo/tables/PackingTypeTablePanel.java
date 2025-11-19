package raven.modal.demo.tables;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.utils.Constants;
import raven.modal.demo.dao.PackingTypeDao;
import raven.modal.demo.forms.FormPackingType;
import raven.modal.demo.model.PackingTypeModel;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.MessageUtils;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.combox.JComponentUtils;
import raven.modal.demo.utils.table.TableHeaderAlignment;
import raven.swingpack.JPagination;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.util.List;

@SystemForm(name = "Packing Types", description = "Manage system Packing type definitions", tags = {"Packing", "table"})
public class PackingTypeTablePanel extends Form implements TableActions {

    private JTable table;
    private DefaultTableModel model;
    private PackingTypeDao PackingTypeDao;
    private JPagination pagination;
    private JLabel lbTotal;
    private JButton btnCreate;

    public PackingTypeTablePanel() {
        PackingTypeDao = new PackingTypeDao();
        initUI();
        loadPackingTypes(1);
    }

    private void initUI() {
        setLayout(new MigLayout("fillx,wrap,insets 15 0 10 0", "[fill]", "[][][fill,grow][]"));

        JLabel title = new JLabel("Packing Type List");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");
        add(title, "gapx 20");

        // --- Control Panel (Includes Create Button) ---
        JPanel controlPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill]20[right]", ""));

        btnCreate = new JButton("Create Packing Type");
        btnCreate.putClientProperty(FlatClientProperties.STYLE, "font:bold; background:$Component.accentColor; foreground:white");
        btnCreate.addActionListener(e -> openPackingTypeFormModal(0));

        controlPanel.add(new JPanel(), "growx");
        controlPanel.add(btnCreate, "align right, w 150!, gapleft 10, gapright 10, gaptop 5, gapbottom 5");
        add(controlPanel, "gapx 20, gaptop 10");

        model = new DefaultTableModel(Constants.peckingTypeColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 4;
            }
        };

        table = new JTable(model);

        // Header Alignment
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table) {
            @Override
            protected int getAlignment(int column) {
                if (column == 1 || column == 4) return SwingConstants.CENTER;
                return SwingConstants.LEADING;
            }
        });

        // Action Column Setup
        int actionColumnIndex = (table.getColumnCount() - 1);
        ActionItem[] actions = tableActions();
        table.getColumnModel().getColumn(actionColumnIndex).setCellRenderer(new TableActionCellRenderer(actions));
        table.getColumnModel().getColumn(actionColumnIndex).setCellEditor(new TableActionCellEditor(table, actions));

        // Column Width Settings
        table.getColumnModel().getColumn(0).setPreferredWidth(50); // ID
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Qty
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // Status
        table.getColumnModel().getColumn(actionColumnIndex).setPreferredWidth(150); // Action

        JScrollPane scroll = new JScrollPane(table);
        // Standard styling properties...
        scroll.setBorder(BorderFactory.createEmptyBorder());
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "height:30; hoverBackground:null; pressedBackground:null; separatorColor:$TableHeader.background;");
        table.putClientProperty(FlatClientProperties.STYLE, "rowHeight:30; showHorizontalLines:true; intercellSpacing:0,1; cellFocusColor:$TableHeader.hoverBackground; selectionBackground:$TableHeader.hoverBackground; selectionForeground:$Table.foreground;");

        add(scroll);

        // Pagination Panel
        pagination = new JPagination(11, 1, 1);
        pagination.addChangeListener(e -> loadPackingTypes(pagination.getSelectedPage()));
        JPanel pagePanel = new JPanel(new MigLayout("insets 5 15 5 15", "[][]push[]"));
        lbTotal = new JLabel("0");
        pagePanel.add(new JLabel("Total:"));
        pagePanel.add(lbTotal);
        pagePanel.add(pagination);
        add(pagePanel);
    }

    // --- MODAL DIALOG METHOD ---
    private void openPackingTypeFormModal(int typeId) {
        JComponentUtils.showModal(
                SwingUtilities.getWindowAncestor(this),
                new FormPackingType(typeId),
                typeId > 0 ? "Edit Packing Type" : "Create New Packing Type"
        );
        formRefresh();
    }

    private void loadPackingTypes(int page) {
        model.setRowCount(0);
        int limit = Constants.LIMIT_PER_PAGE;
        int offset = (page - 1) * limit;

        List<PackingTypeModel> types = PackingTypeDao.getAllPackingTypes(offset, limit);
        int totalTypes = PackingTypeDao.getPackingTypeCount();

        for (PackingTypeModel typeModel : types) {
            model.addRow(new Object[]{
                    typeModel.getPackingTypeId(),
                    typeModel.getPackingTypeName(),
                    typeModel.getCartonQty(),
                    typeModel.isActive() ? "Active" : "Inactive"
            });
        }

        lbTotal.setText(DecimalFormat.getInstance().format(totalTypes));
        int totalPages = (int) Math.ceil((double) totalTypes / limit);
        pagination.setSelectedPage(page);
        pagination.getModel().setPageRange(page, totalPages);
    }

    @Override
    public void formInit() {
        loadPackingTypes(1);
    }

    @Override
    public void formRefresh() {
        loadPackingTypes(pagination.getSelectedPage());
    }

    @Override
    public ActionItem[] tableActions() {
        return new ActionItem[]{
                new ActionItem("Edit", (table1, row) -> {
                    int typeId = (int) table1.getValueAt(row, 0);
                    openPackingTypeFormModal(typeId);
                }),
                new ActionItem("Delete", (table1, row) -> {
                    int typeId = (int) table1.getValueAt(row, 0);
                    String typeName = table1.getValueAt(row, 1).toString();
                    int confirm = JOptionPane.showConfirmDialog(table1,
                            "Are you sure you want to delete Packing Type: " + typeName + "?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        int result = PackingTypeDao.deletePackingType(typeId);
                        MessageUtils.showPackingTypeMessage(result);
                        formRefresh();
                    }
                })
        };
    }
}