package org.example.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.schedulecompare.ScheduleCompareSegmentType;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCompareSegmentResponse {

    @JsonFormat(pattern = "HH:mm")
    private LocalTime segmentStart;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime segmentEnd;

    private ScheduleCompareSegmentType segmentType;

    /** Записи левой стороны, активные на этом интервале (может быть несколько при коллизиях в данных). */
    private List<ScheduleResponse> leftEntries;

    /** Записи правой стороны. */
    private List<ScheduleResponse> rightEntries;
}
