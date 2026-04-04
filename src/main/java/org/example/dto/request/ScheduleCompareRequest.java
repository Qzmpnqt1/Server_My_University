package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.schedulecompare.ScheduleEntityKind;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCompareRequest {

    /**
     * MY_WITH_OTHER — левая сторона берётся из текущего пользователя (студент: группа, преподаватель: self).
     * FULL — обе стороны задаются явно (только ADMIN).
     */
    @NotNull
    private ScheduleCompareMode mode;

    /** Для FULL: тип и id левой сущности. */
    private ScheduleEntityKind leftKind;
    private Long leftId;

    @NotNull
    private ScheduleEntityKind rightKind;
    @NotNull
    private Long rightId;

    @NotNull
    private Integer weekNumber;

    /** null — все дни недели 1–7. */
    private Integer dayOfWeek;

    public enum ScheduleCompareMode {
        MY_WITH_OTHER,
        FULL
    }
}
