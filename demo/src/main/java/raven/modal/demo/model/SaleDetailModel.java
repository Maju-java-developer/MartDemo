package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleDetailModel {

    private int saleDetailID;
    private int saleID;
    private int productID;
    private String productName; // For display/joining

    private double quantity;     // Total quantity in base units
    private double rate;         // Unit selling price
    private double lineDiscount; // Discount on this line item (fixed value)
    private double total;        // Net price of line item after lineDiscount

    // Derived fields, useful for display/stock
    private int unitsPerCarton;
}
