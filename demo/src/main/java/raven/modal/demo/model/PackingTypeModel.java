package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import raven.modal.demo.annotations.DropdownField;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PackingTypeModel extends AbstractModel{
    @DropdownField(isId = true)
    private int packingTypeId;
    @DropdownField(isText = true)
    private String packingTypeName;
    private int cartonQty;
    private boolean isActive;
}
