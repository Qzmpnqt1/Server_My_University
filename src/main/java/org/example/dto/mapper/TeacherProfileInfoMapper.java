package org.example.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.UserProfileResponse;
import org.example.model.TeacherProfile;
import org.example.repository.TeacherSubjectRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TeacherProfileInfoMapper {

    private final TeacherSubjectRepository teacherSubjectRepository;

    public UserProfileResponse.TeacherProfileInfo toInfo(TeacherProfile profile) {
        List<String> fromAssignments = teacherSubjectRepository.findDistinctInstitutesForTeacher(profile.getId()).stream()
                .map(i -> i.getName())
                .toList();
        return UserProfileResponse.TeacherProfileInfo.builder()
                .teacherProfileId(profile.getId())
                .universityId(profile.getUniversity() != null ? profile.getUniversity().getId() : null)
                .universityName(profile.getUniversity() != null ? profile.getUniversity().getName() : null)
                .instituteId(profile.getInstitute() != null ? profile.getInstitute().getId() : null)
                .instituteName(profile.getInstitute() != null ? profile.getInstitute().getName() : null)
                .institutesFromAssignments(fromAssignments.isEmpty() ? null : fromAssignments)
                .position(profile.getPosition())
                .build();
    }
}
