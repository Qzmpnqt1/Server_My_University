package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.RegistrationStatus;
import org.example.model.UserType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private UserType userType;
    private RegistrationStatus status;
    private String rejectionReason;
    private Long universityId;
    private String universityName;
    private Long groupId;
    private String groupName;
    private Long instituteId;
    private String instituteName;
    private LocalDateTime createdAt;
}
