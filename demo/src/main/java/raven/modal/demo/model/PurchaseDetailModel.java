package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseDetailModel {

    private int purchaseDetailID;
    private int purchaseID;
    private int productID;
    private double quantity;
    private double rate;
    private double total;

    // Helper fields for display/form
    private String productName;
    private int unitsPerCarton;
    private String unitName; // Needed if TBLProducts is linked to TBLUnits
}