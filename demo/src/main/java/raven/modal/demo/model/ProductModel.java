package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ProductModel {
    private int productId;
    private String productCode;
    private String productName;
    private int brandId;         // New field
    private int categoryId;      // New field
    private int peckingTypeId;   // New field (PeekingTypeId in DAO)
    private int companyId;       // New field
    private boolean isActive = true;
    private String brandName;
    private String categoryName;
    private String peckingTypeName;
    private String companyName;
    private int unitsPerCarton;

    @Override
    public String toString() {
        // This tells the JComboBox editor (and the ComboBox itself) how to display the object.
        return productName;
    }
}
