package org.example.repository;

import org.example.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s JOIN s.group g JOIN g.direction d JOIN d.institute i WHERE i.university.id = :uniId")
    List<Schedule> findAllByUniversityId(@Param("uniId") Long universityId);

    boolean existsByTeacher_IdAndGroup_Id(Long teacherId, Long groupId);

    List<Schedule> findByGroupId(Long groupId);

    List<Schedule> findByTeacherId(Long teacherId);

    List<Schedule> findByGroupIdAndWeekNumber(Long groupId, Integer weekNumber);

    List<Schedule> findByGroupIdAndDayOfWeek(Long groupId, Integer dayOfWeek);

    List<Schedule> findByGroupIdAndWeekNumberAndDayOfWeek(Long groupId, Integer weekNumber, Integer dayOfWeek);

    List<Schedule> findByTeacherIdAndWeekNumber(Long teacherId, Integer weekNumber);

    List<Schedule> findByTeacherIdAndDayOfWeek(Long teacherId, Integer dayOfWeek);

    List<Schedule> findByTeacherIdAndWeekNumberAndDayOfWeek(Long teacherId, Integer weekNumber, Integer dayOfWeek);

    List<Schedule> findByClassroomId(Long classroomId);

    @Query("SELECT DISTINCT s.group.id FROM Schedule s WHERE s.teacher.id = :teacherUserId")
    List<Long> findDistinctGroupIdsByTeacherUserId(@Param("teacherUserId") Long teacherUserId);

    @Query("SELECT s FROM Schedule s WHERE s.dayOfWeek = :dayOfWeek AND s.weekNumber = :weekNumber " +
            "AND s.startTime < :endTime AND s.endTime > :startTime " +
            "AND s.teacher.id = :teacherId AND (:excludeId IS NULL OR s.id != :excludeId)")
    List<Schedule> findTeacherConflicts(@Param("dayOfWeek") Integer dayOfWeek,
                                       @Param("weekNumber") Integer weekNumber,
                                       @Param("startTime") LocalTime startTime,
                                       @Param("endTime") LocalTime endTime,
                                       @Param("teacherId") Long teacherId,
                                       @Param("excludeId") Long excludeId);

    @Query("SELECT s FROM Schedule s WHERE s.dayOfWeek = :dayOfWeek AND s.weekNumber = :weekNumber " +
            "AND s.startTime < :endTime AND s.endTime > :startTime " +
            "AND s.group.id = :groupId AND (:excludeId IS NULL OR s.id != :excludeId)")
    List<Schedule> findGroupConflicts(@Param("dayOfWeek") Integer dayOfWeek,
                                     @Param("weekNumber") Integer weekNumber,
                                     @Param("startTime") LocalTime startTime,
                                     @Param("endTime") LocalTime endTime,
                                     @Param("groupId") Long groupId,
                                     @Param("excludeId") Long excludeId);

    @Query("SELECT s FROM Schedule s WHERE s.dayOfWeek = :dayOfWeek AND s.weekNumber = :weekNumber " +
            "AND s.startTime < :endTime AND s.endTime > :startTime " +
            "AND s.classroom.id = :classroomId AND (:excludeId IS NULL OR s.id != :excludeId)")
    List<Schedule> findClassroomConflicts(@Param("dayOfWeek") Integer dayOfWeek,
                                         @Param("weekNumber") Integer weekNumber,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("endTime") LocalTime endTime,
                                         @Param("classroomId") Long classroomId,
                                         @Param("excludeId") Long excludeId);
}
