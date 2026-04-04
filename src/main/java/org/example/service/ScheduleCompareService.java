package org.example.service;

import org.example.dto.request.ScheduleCompareRequest;
import org.example.dto.response.*;

import java.util.List;

public interface ScheduleCompareService {

    ScheduleCompareResultResponse compare(ScheduleCompareRequest request, String viewerEmail);

    List<ScheduleCompareInstituteOptionResponse> listInstitutes(String viewerEmail);

    List<ScheduleCompareDirectionOptionResponse> listDirections(String viewerEmail, Long instituteId);

    List<ScheduleCompareGroupOptionResponse> listGroups(String viewerEmail, Long instituteId, Long directionId, String q);

    List<ScheduleCompareTeacherOptionResponse> listTeachers(String viewerEmail, String q);

    List<ScheduleCompareClassroomOptionResponse> listClassrooms(String viewerEmail, String q);
}
