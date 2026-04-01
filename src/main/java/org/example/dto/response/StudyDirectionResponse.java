package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyDirectionResponse {

    private Long id;
    private String name;
    private String code;
    private Long instituteId;
    private String instituteName;
}
