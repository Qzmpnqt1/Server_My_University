package org.example.service.impl;

import org.example.model.Schedule;
import org.example.model.schedulecompare.ScheduleCompareSegmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleComparisonEngineTest {

    private static Schedule slot(long id, int dow, String start, String end) {
        return Schedule.builder()
                .id(id)
                .dayOfWeek(dow)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build();
    }

    @Test
    @DisplayName("Пустые обе стороны — пустой результат")
    void emptyBoth() {
        var raw = ScheduleComparisonEngine.buildRawSegmentsForDay(List.of(), List.of(), 1);
        assertThat(raw).isEmpty();
    }

    @Test
    @DisplayName("Только левая сторона — один сегмент ONLY_LEFT")
    void onlyLeft() {
        List<Schedule> left = List.of(slot(1, 1, "09:00", "10:30"));
        var raw = ScheduleComparisonEngine.buildRawSegmentsForDay(left, List.of(), 1);
        assertThat(raw).hasSize(1);
        assertThat(raw.get(0).getSegmentType()).isEqualTo(ScheduleCompareSegmentType.ONLY_LEFT);
        assertThat(raw.get(0).getSegmentStart()).isEqualTo(LocalTime.of(9, 0));
        assertThat(raw.get(0).getSegmentEnd()).isEqualTo(LocalTime.of(10, 30));
        assertThat(raw.get(0).getLeft()).hasSize(1);
        assertThat(raw.get(0).getRight()).isEmpty();
    }

    @Test
    @DisplayName("Частичное пересечение — три сегмента")
    void partialOverlap() {
        List<Schedule> left = List.of(slot(1, 1, "09:00", "11:00"));
        List<Schedule> right = List.of(slot(2, 1, "10:00", "12:00"));
        var raw = ScheduleComparisonEngine.buildRawSegmentsForDay(left, right, 1);
        assertThat(raw).hasSize(3);
        assertThat(raw.get(0).getSegmentType()).isEqualTo(ScheduleCompareSegmentType.ONLY_LEFT);
        assertThat(raw.get(0).getSegmentStart()).isEqualTo(LocalTime.of(9, 0));
        assertThat(raw.get(0).getSegmentEnd()).isEqualTo(LocalTime.of(10, 0));
        assertThat(raw.get(1).getSegmentType()).isEqualTo(ScheduleCompareSegmentType.BOTH);
        assertThat(raw.get(1).getSegmentStart()).isEqualTo(LocalTime.of(10, 0));
        assertThat(raw.get(1).getSegmentEnd()).isEqualTo(LocalTime.of(11, 0));
        assertThat(raw.get(2).getSegmentType()).isEqualTo(ScheduleCompareSegmentType.ONLY_RIGHT);
        assertThat(raw.get(2).getSegmentStart()).isEqualTo(LocalTime.of(11, 0));
        assertThat(raw.get(2).getSegmentEnd()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("Соседние сегменты с одинаковым набором id занятий объединяются")
    void mergeAdjacentSameState() {
        List<Schedule> left = List.of(
                slot(1, 1, "09:00", "10:00"),
                slot(1, 1, "10:00", "11:00"));
        var raw = ScheduleComparisonEngine.buildRawSegmentsForDay(left, List.of(), 1);
        assertThat(raw).hasSize(1);
        assertThat(raw.get(0).getSegmentStart()).isEqualTo(LocalTime.of(9, 0));
        assertThat(raw.get(0).getSegmentEnd()).isEqualTo(LocalTime.of(11, 0));
        assertThat(raw.get(0).getLeft()).hasSize(1);
    }

    @Test
    @DisplayName("Другой день недели не попадает в выборку")
    void wrongDayFiltered() {
        List<Schedule> left = List.of(slot(1, 2, "09:00", "10:00"));
        var raw = ScheduleComparisonEngine.buildRawSegmentsForDay(left, List.of(), 1);
        assertThat(raw).isEmpty();
    }
}
