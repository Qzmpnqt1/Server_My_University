package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCompareResultResponse {

    private Integer weekNumber;
    private String leftLabel;
    private String rightLabel;

    private int segmentsBothSidesBusy;
    private int segmentsOnlyLeft;
    private int segmentsOnlyRight;

    private List<ScheduleCompareDayResponse> days;
}
