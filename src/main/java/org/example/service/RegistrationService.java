package org.example.service;

import org.example.dto.request.ApproveRejectRequest;
import org.example.dto.request.GuestRegistrationLookupRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.request.UpdatePendingRegistrationRequest;
import org.example.dto.response.GuestRegistrationStatusResponse;
import org.example.dto.response.RegistrationRequestResponse;
import org.example.model.RegistrationStatus;
import org.example.model.UserType;

import java.util.List;

public interface RegistrationService {
    void submitRegistration(RegisterRequest request);

    GuestRegistrationStatusResponse lookupRegistrationStatus(GuestRegistrationLookupRequest request);

    void updatePendingRegistration(UpdatePendingRegistrationRequest request);

    List<RegistrationRequestResponse> getAllRequests(RegistrationStatus status, UserType userType,
                                                     Long instituteId, String adminEmail);

    RegistrationRequestResponse getRequestById(Long id, String adminEmail);

    void approveRequest(Long id, String adminEmail);

    void rejectRequest(Long id, ApproveRejectRequest request, String adminEmail);
}
