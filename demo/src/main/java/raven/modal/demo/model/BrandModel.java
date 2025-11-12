package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandModel {

    private int brandId;
    private String brandTitle;
    private int companyId;
    @Builder.Default
    private boolean isActive = true; // Default to true
    private String companyName;
}