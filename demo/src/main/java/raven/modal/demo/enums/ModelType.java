package raven.modal.demo.enums;

public enum ModelType {

    COMPANY("Company"),
    CATEGORY("Category"),
    PACKING_TYPE("PackingType"),
    BRAND("Brand"),
    PRODUCT("Product"),
    VENDOR("Vendor"),
    CUSTOMER("Customer"),
    SALE("Sale"),
    PURCHASE("Purchase");

    private final String base;

    ModelType(String base) {
        this.base = base;
    }

    public String list() {
        return base + "List";
    }

    public String dropdown() {
        return base + "DropDown";
    }

    public String single() {
        return base;
    }
}

