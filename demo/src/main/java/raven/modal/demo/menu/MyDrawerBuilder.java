/*
 * MyDrawerBuilder.java
 * 
 * A singleton builder for creating and customizing drawer menus in the Swing application.
 * Provides capabilities for setting user details, customizing header/footer, and building menu options dynamically.
 * 
 * @author Majid.Hussain
 * @date 13-11-2025
 */

package raven.modal.demo.menu;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import raven.extras.AvatarIcon;
import raven.modal.demo.Demo;
import raven.modal.demo.forms.FormCustomer;
import raven.modal.demo.forms.FormProducts;
import raven.modal.demo.forms.FormPurchase;
import raven.modal.demo.forms.FormSale;
import raven.modal.demo.forms.FormSupplier;
import raven.modal.demo.model.ModelUser;
import raven.modal.demo.system.AllForms;
import raven.modal.demo.system.Form;
import raven.modal.demo.system.FormManager;
import raven.modal.demo.tables.BrandTablePanel;
import raven.modal.demo.tables.CategoryTablePanel;
import raven.modal.demo.tables.CompanyTablePanel;
import raven.modal.demo.tables.CustomerTablePanel;
import raven.modal.demo.tables.PeckingTypeTablePanel;
import raven.modal.demo.tables.ProductTablePanel;
import raven.modal.demo.tables.PurchaseTablePanel;
import raven.modal.demo.tables.SupplierTablePanel;
import raven.modal.drawer.DrawerPanel;
import raven.modal.drawer.item.Item;
import raven.modal.drawer.item.MenuItem;
import raven.modal.drawer.menu.MenuAction;
import raven.modal.drawer.menu.MenuEvent;
import raven.modal.drawer.menu.MenuOption;
import raven.modal.drawer.menu.MenuStyle;
import raven.modal.drawer.renderer.DrawerStraightDotLineStyle;
import raven.modal.drawer.simple.SimpleDrawerBuilder;
import raven.modal.drawer.simple.footer.LightDarkButtonFooter;
import raven.modal.drawer.simple.footer.SimpleFooterData;
import raven.modal.drawer.simple.header.SimpleHeader;
import raven.modal.drawer.simple.header.SimpleHeaderData;
import raven.modal.option.Option;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Singleton builder for the application drawer menu,
 * providing a central place for menu construction, header/footer customization,
 * user info integration, and menu validation.
 * 
 * @author Majid.Hussain
 * @date 13-11-2025
 */
public class MyDrawerBuilder extends SimpleDrawerBuilder {

    private static MyDrawerBuilder instance;
    private ModelUser user;

    /**
     * Get the singleton instance of MyDrawerBuilder.
     * 
     * @return MyDrawerBuilder instance
     */
    public static MyDrawerBuilder getInstance() {
        if (instance == null) {
            instance = new MyDrawerBuilder();
        }
        return instance;
    }

    /**
     * Get current ModelUser.
     *
     * @return the current user
     */
    public ModelUser getUser() {
        return user;
    }

    /**
     * Set the current user and update menu header and items if necessary.
     *
     * @param user ModelUser to set
     */
    public void setUser(ModelUser user) {
        boolean updateMenuItem = this.user == null || this.user.getRole() != user.getRole();

        this.user = user;

        // set user to menu validation
        MyMenuValidation.setUser(user);

        // setup drawer header
        SimpleHeader header = (SimpleHeader) getHeader();
        SimpleHeaderData data = header.getSimpleHeaderData();
        AvatarIcon icon = (AvatarIcon) data.getIcon();
        String iconName = user.getRole() == ModelUser.Role.ADMIN ? "avatar_me.svg" : "avatar_female.svg";

        icon.setIcon(new FlatSVGIcon("raven/modal/demo/drawer/image/" + iconName, 100, 100));
        data.setTitle(user.getUserName());
        data.setDescription(user.getEmail());
        header.setSimpleHeaderData(data);

        if (updateMenuItem) {
            rebuildMenu();
        }
    }

    private final int SHADOW_SIZE = 12;

    /**
     * Private constructor for singleton pattern.
     * Initializes drawer builder with customized menu options and footer.
     */
    private MyDrawerBuilder() {
        super(createSimpleMenuOption());
        LightDarkButtonFooter lightDarkButtonFooter = (LightDarkButtonFooter) getFooter();
        lightDarkButtonFooter.addModeChangeListener(isDarkMode -> {
            // event for light dark mode changed
        });
    }

    /**
     * Get data for the header, including avatar, title, and description.
     *
     * @return SimpleHeaderData for drawer header
     */
    @Override
    public SimpleHeaderData getSimpleHeaderData() {
        AvatarIcon icon = new AvatarIcon(new FlatSVGIcon("raven/modal/demo/drawer/image/avatar_me.svg", 100, 100), 50, 50, 3.5f);
        icon.setType(AvatarIcon.Type.MASK_SQUIRCLE);
        icon.setBorder(2, 2);

        changeAvatarIconBorderColor(icon);

        UIManager.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals("lookAndFeel")) {
                changeAvatarIconBorderColor(icon);
            }
        });

        return new SimpleHeaderData()
                .setIcon(icon)
                .setTitle(user != null ? user.getUserName() : "")
                .setDescription(user != null ? user.getEmail(): "");
    }

    /**
     * Change the border color for avatar icon depending on current look and feel.
     * 
     * @param icon AvatarIcon to update
     */
    private void changeAvatarIconBorderColor(AvatarIcon icon) {
        icon.setBorderColor(new AvatarIcon.BorderColor(UIManager.getColor("Component.accentColor"), 0.7f));
    }

    /**
     * Get data for the drawer footer (app version info).
     *
     * @return SimpleFooterData for drawer footer
     */
    @Override
    public SimpleFooterData getSimpleFooterData() {
        return new SimpleFooterData()
                .setTitle("Swing Modal Dialog")
                .setDescription("Version " + Demo.DEMO_VERSION);
    }

    /**
     * Create and customize Option for the drawer.
     * 
     * @return customized Option instance
     */
    @Override
    public Option createOption() {
        Option option = super.createOption();
        option.setOpacity(0.3f);
        option.getBorderOption()
                .setShadowSize(new Insets(0, 0, 0, SHADOW_SIZE));
        return option;
    }

    /**
     * Build a MenuOption with customized MenuItem structure and menu events.
     * 
     * @return MenuOption for the drawer's menu
     */
    public static MenuOption createSimpleMenuOption() {

        // create simple menu option
        MenuOption simpleMenuOption = new MenuOption();

        MenuItem items[] = new MenuItem[]{
                new Item.Label("MAIN"),
//                new Item("Dashboard", "dashboard.svg", FormDashboard.class),
                new Item("Purchase", "purchases.svg")
                        .subMenu("Add Purchase", FormPurchase.class)
                        .subMenu("Purchase History", PurchaseTablePanel.class),
                new Item("Sale", "sale.svg")
                        .subMenu("Add Sale", FormSale.class)
                        .subMenu("Sale History", PurchaseTablePanel.class),
                new Item("Suppliers", "chat.svg")
                        .subMenu("Add Supplier", FormSupplier.class)
                        .subMenu("View All Suppliers", SupplierTablePanel.class),
                new Item("Customers", "chat.svg")
                        .subMenu("Add Customer", FormCustomer.class)
                        .subMenu("View All Customers", CustomerTablePanel.class),
                new Item("Setting", "setting.svg")
                        .subMenu("Company", CompanyTablePanel.class)
                        .subMenu("Category", CategoryTablePanel.class)
                        .subMenu("Pecking Type", PeckingTypeTablePanel.class)
                        .subMenu("Brand", BrandTablePanel.class)
                        .subMenu( new Item("Product")
                                .subMenu("Add Product", FormProducts.class)
                                .subMenu("View All Products", ProductTablePanel.class)),
                new Item("Logout", "logout.svg")
        };

        simpleMenuOption.setMenuStyle(new MenuStyle() {

            /**
             * Style each menu item according to its level.
             *
             * @param menu the menu button
             * @param index item index path
             * @param isMainItem if this is a main item
             */
            @Override
            public void styleMenuItem(JButton menu, int[] index, boolean isMainItem) {
                boolean isTopLevel = index.length == 1;
                if (isTopLevel) {
                    // adjust item menu at the top level because it's contain icon
                    menu.putClientProperty(FlatClientProperties.STYLE, "" +
                            "margin:-1,0,-1,0;");
                }
            }

            /**
             * Style the main menu component.
             * 
             * @param component the menu component
             */
            @Override
            public void styleMenu(JComponent component) {
                component.putClientProperty(FlatClientProperties.STYLE, getDrawerBackgroundStyle());
            }
        });

        simpleMenuOption.getMenuStyle().setDrawerLineStyleRenderer(new DrawerStraightDotLineStyle());
        simpleMenuOption.setMenuValidation(new MyMenuValidation());

        simpleMenuOption.addMenuEvent(new MenuEvent() {
            /**
             * Handle menu selection events and perform required actions based on menu index.
             *
             * @param action MenuAction object
             * @param index Index path of selected menu
             */
            @Override
            public void selected(MenuAction action, int[] index) {
                System.out.println("Drawer menu selected " + Arrays.toString(index));
                Class<?> itemClass = action.getItem().getItemClass();
                int i = index[0];
                if (i == 9) {
                    action.consume();
                    FormManager.showAbout();
                    return;
                } else if (i == 5) {
                    action.consume();
                    FormManager.logout();
                    return;
                }
                if (itemClass == null || !Form.class.isAssignableFrom(itemClass)) {
                    action.consume();
                    return;
                }
                Class<? extends Form> formClass = (Class<? extends Form>) itemClass;
                FormManager.showForm(AllForms.getForm(formClass));
            }
        });

        simpleMenuOption.setMenus(items)
                .setBaseIconPath("raven/modal/demo/drawer/icon")
                .setIconScale(0.45f);

        return simpleMenuOption;
    }

    /**
     * Get the drawer width with shadow.
     * 
     * @return drawer width in pixels
     */
    @Override
    public int getDrawerWidth() {
        return 270 + SHADOW_SIZE;
    }

    /**
     * Get the compact drawer width with shadow.
     * 
     * @return compact drawer width in pixels
     */
    @Override
    public int getDrawerCompactWidth() {
        return 80 + SHADOW_SIZE;
    }

    /**
     * Get value for open drawer location.
     * 
     * @return open drawer location
     */
    @Override
    public int getOpenDrawerAt() {
        return 1000;
    }

    /**
     * Check if drawer should open at scale.
     * 
     * @return true if drawer opens at scale, false otherwise
     */
    @Override
    public boolean openDrawerAtScale() {
        return false;
    }

    /**
     * Apply drawer panel styles.
     *
     * @param drawerPanel DrawerPanel instance to style
     */
    @Override
    public void build(DrawerPanel drawerPanel) {
        drawerPanel.putClientProperty(FlatClientProperties.STYLE, getDrawerBackgroundStyle());
    }

    /**
     * Get drawer background styling string for look and feel adaptation.
     * 
     * @return style string for drawer background
     */
    private static String getDrawerBackgroundStyle() {
        return "" +
                "[light]background:tint($Panel.background,20%);" +
                "[dark]background:tint($Panel.background,5%);";
    }
}
