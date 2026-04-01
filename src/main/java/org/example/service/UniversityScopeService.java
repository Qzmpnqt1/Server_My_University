package org.example.service;

import java.util.Set;

/**
 * Проверка принадлежности сущностей вузу и получение university_id текущего администратора.
 */
public interface UniversityScopeService {

    /** Все user id студентов, преподавателей и админов указанного вуза (для аудита и фильтрации). */
    Set<Long> allUserIdsInUniversity(Long universityId);

    Long requireAdminUniversityId(String adminEmail);

    boolean userBelongsToUniversity(Long userId, Long universityId);

    void assertUserInUniversity(Long userId, Long universityId);

    void assertInstituteInUniversity(Long instituteId, Long universityId);

    void assertStudyDirectionInUniversity(Long directionId, Long universityId);

    void assertAcademicGroupInUniversity(Long groupId, Long universityId);

    void assertClassroomInUniversity(Long classroomId, Long universityId);

    void assertSubjectDirectionInUniversity(Long subjectDirectionId, Long universityId);

    void assertUniversityMatches(Long universityId, Long adminUniversityId);

    void assertRegistrationRequestInUniversity(Long requestId, Long universityId);
}
