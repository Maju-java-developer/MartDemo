package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private int userId;
    private String fullName;
    private String email;
    private String contactNo;
    private String address;
    private String cnic;
    private LocalDateTime createdDate = LocalDateTime.now(); // Default value
    private boolean isActive = true; // Default value
}
