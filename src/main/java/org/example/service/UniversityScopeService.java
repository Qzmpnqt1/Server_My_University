package org.example.service;

import java.util.Set;

/**
 * Проверка принадлежности сущностей вузу и область видимости администраторов (кампусный / супер).
 */
public interface UniversityScopeService {

    /**
     * Эффективная область данных для админских списков и фильтрации.
     * <ul>
     *     <li>{@code ADMIN}: всегда один вуз (кампусный); переданный {@code requestUniversityId} должен совпадать или быть null.</li>
     *     <li>{@code SUPER_ADMIN}: {@code requestUniversityId == null} → глобальный режим (все вузы);
     *     иначе — только указанный вуз.</li>
     * </ul>
     */
    record AdminQueryScope(boolean globalAllUniversities, Long universityId) {}

    /**
     * @param requestUniversityId для SUPER_ADMIN: null = глобальный режим; для ADMIN: игнорируется при null (берётся кампус).
     */
    AdminQueryScope resolveAdminQueryScope(String viewerEmail, Long requestUniversityId);

    /** Все user id студентов, преподавателей и админов указанного вуза (для аудита и фильтрации). */
    Set<Long> allUserIdsInUniversity(Long universityId);

    boolean isSuperAdmin(String email);

    void requireAdminOrSuperAdmin(String email);

    /**
     * Только кампусный {@link org.example.model.UserType#ADMIN}; для {@code SUPER_ADMIN} — исключение.
     */
    Long requireCampusUniversityId(String adminEmail);

    /**
     * Создание сущности, в запросе которой явно передан {@code universityId}.
     */
    Long resolveMutationTargetUniversity(String actorEmail, Long requestedUniversityId);

    /**
     * Доступ к сущности с известным вузом: кампусный админ — только свой вуз; супер — любой.
     *
     * @return идентификатор вуза сущности (для последующих assert*InUniversity)
     */
    Long enforceAccessToEntityUniversity(String actorEmail, Long entityUniversityId);

    /**
     * Списки для админ-интерфейса: кампусный админ всегда видит только свой вуз;
     * супер — все вузы, если {@code queryUniversityId == null}, иначе фильтр по вузу.
     */
    AdminListScope resolveAdminListScope(String viewerEmail, Long queryUniversityId);

    record AdminListScope(boolean allUniversities, Long universityId) {}

    boolean userBelongsToUniversity(Long userId, Long universityId);

    void assertUserInUniversity(Long userId, Long universityId);

    void assertInstituteInUniversity(Long instituteId, Long universityId);

    void assertStudyDirectionInUniversity(Long directionId, Long universityId);

    void assertAcademicGroupInUniversity(Long groupId, Long universityId);

    void assertClassroomInUniversity(Long classroomId, Long universityId);

    void assertSubjectDirectionInUniversity(Long subjectDirectionId, Long universityId);

    void assertUniversityMatches(Long universityId, Long adminUniversityId);

    void assertRegistrationRequestInUniversity(Long requestId, Long universityId);

    /** Преподаватель относится к вузу по профилю/институту или фигурирует в расписании групп этого вуза. */
    boolean teacherUserInUniversity(Long teacherUserId, Long universityId);
}
