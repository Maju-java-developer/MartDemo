package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PackingTypeModel {
    private int packingTypeId;
    private String packingTypeName;
    private int quarterQty;
    private boolean isActive;
}
