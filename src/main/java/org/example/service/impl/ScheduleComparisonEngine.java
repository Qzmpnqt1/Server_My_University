package org.example.service.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.model.Schedule;
import org.example.model.schedulecompare.ScheduleCompareSegmentType;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Разбиение дня на минимальные сегменты по границам start/end и классификация пересечений.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScheduleComparisonEngine {

    @Getter
    @AllArgsConstructor
    public static final class RawSegment {
        private final LocalTime segmentStart;
        private final LocalTime segmentEnd;
        private final ScheduleCompareSegmentType segmentType;
        private final List<Schedule> left;
        private final List<Schedule> right;
    }

    public static List<RawSegment> buildRawSegmentsForDay(
            List<Schedule> left,
            List<Schedule> right,
            int dayOfWeek) {

        List<Schedule> l = left.stream().filter(s -> Objects.equals(s.getDayOfWeek(), dayOfWeek)).toList();
        List<Schedule> r = right.stream().filter(s -> Objects.equals(s.getDayOfWeek(), dayOfWeek)).toList();

        TreeSet<LocalTime> bounds = new TreeSet<>(Comparator.naturalOrder());
        for (Schedule s : l) {
            bounds.add(s.getStartTime());
            bounds.add(s.getEndTime());
        }
        for (Schedule s : r) {
            bounds.add(s.getStartTime());
            bounds.add(s.getEndTime());
        }
        if (bounds.size() < 2) {
            return List.of();
        }

        List<LocalTime> ordered = new ArrayList<>(bounds);
        List<RawSegment> raw = new ArrayList<>();
        for (int i = 0; i < ordered.size() - 1; i++) {
            LocalTime t1 = ordered.get(i);
            LocalTime t2 = ordered.get(i + 1);
            if (!t1.isBefore(t2)) {
                continue;
            }
            List<Schedule> activeL = overlapping(l, t1, t2);
            List<Schedule> activeR = overlapping(r, t1, t2);
            if (activeL.isEmpty() && activeR.isEmpty()) {
                continue;
            }
            ScheduleCompareSegmentType type;
            if (!activeL.isEmpty() && !activeR.isEmpty()) {
                type = ScheduleCompareSegmentType.BOTH;
            } else if (!activeL.isEmpty()) {
                type = ScheduleCompareSegmentType.ONLY_LEFT;
            } else {
                type = ScheduleCompareSegmentType.ONLY_RIGHT;
            }
            raw.add(new RawSegment(t1, t2, type, activeL, activeR));
        }
        return mergeAdjacent(raw);
    }

    private static List<Schedule> overlapping(List<Schedule> list, LocalTime segStart, LocalTime segEnd) {
        return list.stream()
                .filter(s -> s.getStartTime().isBefore(segEnd) && s.getEndTime().isAfter(segStart))
                .sorted(Comparator.comparing(Schedule::getStartTime))
                .toList();
    }

    private static List<RawSegment> mergeAdjacent(List<RawSegment> raw) {
        if (raw.isEmpty()) {
            return raw;
        }
        List<RawSegment> out = new ArrayList<>();
        RawSegment cur = raw.get(0);
        for (int i = 1; i < raw.size(); i++) {
            RawSegment next = raw.get(i);
            if (canMerge(cur, next)) {
                cur = new RawSegment(
                        cur.getSegmentStart(),
                        next.getSegmentEnd(),
                        cur.getSegmentType(),
                        cur.getLeft(),
                        cur.getRight()
                );
            } else {
                out.add(cur);
                cur = next;
            }
        }
        out.add(cur);
        return out;
    }

    private static boolean canMerge(RawSegment a, RawSegment b) {
        if (a.getSegmentType() != b.getSegmentType()) {
            return false;
        }
        if (!Objects.equals(a.getSegmentEnd(), b.getSegmentStart())) {
            return false;
        }
        return idSet(a.getLeft()).equals(idSet(b.getLeft()))
                && idSet(a.getRight()).equals(idSet(b.getRight()));
    }

    private static Set<Long> idSet(List<Schedule> entries) {
        return entries.stream().map(Schedule::getId).collect(Collectors.toSet());
    }
}
