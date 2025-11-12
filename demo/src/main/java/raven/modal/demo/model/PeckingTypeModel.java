package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PeckingTypeModel {
    private int peckingTypeId;
    private String peckingTypeName;
    private int quarterQty;
    private boolean isActive;
}
