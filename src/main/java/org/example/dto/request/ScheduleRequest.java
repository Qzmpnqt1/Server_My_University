package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

    @NotNull(message = "ID типа занятия обязателен")
    private Long subjectTypeId;

    @NotNull(message = "ID преподавателя обязателен")
    private Long teacherId;

    @NotNull(message = "ID группы обязателен")
    private Long groupId;

    @NotNull(message = "ID аудитории обязателен")
    private Long classroomId;

    @NotNull(message = "День недели обязателен")
    private Integer dayOfWeek;

    @NotNull(message = "Время начала обязательно")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Время окончания обязательно")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @NotNull(message = "Номер недели обязателен")
    private Integer weekNumber;
}
