package org.example.model.schedulecompare;

/**
 * ONLY_LEFT — занятость только у левой стороны сравнения.
 * ONLY_RIGHT — только у правой.
 * BOTH — в интервале есть занятия с обеих сторон (в т.ч. частичное пересечение по времени внутри сегмента).
 */
public enum ScheduleCompareSegmentType {
    ONLY_LEFT,
    ONLY_RIGHT,
    BOTH
}
