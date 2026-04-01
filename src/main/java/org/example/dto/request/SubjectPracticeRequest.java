package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectPracticeRequest {

    @NotNull(message = "ID предмета в направлении обязателен")
    private Long subjectDirectionId;

    @NotNull(message = "Номер практики обязателен")
    private Integer practiceNumber;

    @NotBlank(message = "Название практики обязательно")
    private String practiceTitle;

    private Integer maxGrade;

    private Boolean isCredit;
}
