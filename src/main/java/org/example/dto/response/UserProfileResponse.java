package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.UserType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private UserType userType;
    private Boolean isActive;
    private LocalDateTime createdAt;

    private StudentProfileInfo studentProfile;
    private TeacherProfileInfo teacherProfile;
    private AdminProfileInfo adminProfile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentProfileInfo {
        private Long groupId;
        private String groupName;
        /** Курс обучения (как у академической группы: 1, 2, 3, …). */
        private Integer course;
        private Long instituteId;
        private String instituteName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherProfileInfo {
        /** id из teacher_profiles — для API назначений и расписания. */
        private Long teacherProfileId;
        /** Вуз из заявки / профиля (без привязки к одному институту). */
        private Long universityId;
        private String universityName;
        /** Устаревшее одиночное поле института в профиле (может быть null). */
        private Long instituteId;
        private String instituteName;
        /** Институты по фактическим назначениям на дисциплины (может быть несколько). */
        private List<String> institutesFromAssignments;
        private String position;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminProfileInfo {
        private Long universityId;
        private String universityName;
        private String role;
    }
}
