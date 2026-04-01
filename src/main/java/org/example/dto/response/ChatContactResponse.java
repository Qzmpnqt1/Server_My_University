package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatContactResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private String userType;
}
