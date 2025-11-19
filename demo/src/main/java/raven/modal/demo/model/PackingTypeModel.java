package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PackingTypeModel extends AbstractModel{
    private int packingTypeId;
    private String packingTypeName;
    private int cartonQty;
    private boolean isActive;
}
