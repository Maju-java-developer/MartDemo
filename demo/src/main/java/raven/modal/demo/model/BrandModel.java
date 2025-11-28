package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.EqualsAndHashCode;
import raven.modal.demo.annotations.DropdownField;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandModel extends AbstractModel {

    @DropdownField(isId = true)
    private int brandId;
    @DropdownField(isText = true)
    private String brandTitle;
    private int companyId;
    @Builder.Default
    private boolean isActive = true; // Default to true
    private String companyName;

    public BrandModel(int brandId, String brandTitle, String companyName, boolean isActive) {
        this.brandId = brandId;
        this.brandTitle = brandTitle;
        this.isActive = isActive;
        this.companyName = companyName;
    }
}