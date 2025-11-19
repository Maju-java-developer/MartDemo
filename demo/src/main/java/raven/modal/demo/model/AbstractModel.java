package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import raven.modal.demo.utils.Constants;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbstractModel {
    private LocalDateTime createdDate;
    private static Integer createdBy = Constants.currentUser.getUserId();
    private LocalDateTime updatedDate;
    private static Integer updatedBy = Constants.currentUser.getUserId();
}
