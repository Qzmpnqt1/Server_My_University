package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Элемент каскадного выбора (институт, направление, группа, студент). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherGradingPickResponse {

    private Long id;
    private String name;
    private String subtitle;
}
