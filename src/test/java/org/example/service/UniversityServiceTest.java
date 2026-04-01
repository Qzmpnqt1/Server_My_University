package org.example.service;

import org.example.dto.request.UniversityRequest;
import org.example.dto.response.UniversityResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.University;
import org.example.repository.UniversityRepository;
import org.example.repository.UsersRepository;
import org.example.service.UniversityScopeService;
import org.example.service.impl.UniversityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UniversityServiceTest {

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private UniversityScopeService universityScopeService;

    @InjectMocks
    private UniversityServiceImpl universityService;

    @BeforeEach
    void scopeStubs() {
        lenient().when(universityScopeService.requireAdminUniversityId(anyString())).thenReturn(7L);
        lenient().doNothing().when(universityScopeService).assertUniversityMatches(anyLong(), anyLong());
    }

    @Test
    void getAll_ReturnsMappedList() {
        University u1 = University.builder()
                .id(1L)
                .name("Moscow State")
                .shortName("MSU")
                .city("Moscow")
                .build();
        University u2 = University.builder()
                .id(2L)
                .name("SPbU")
                .shortName("SPbU")
                .city("Saint Petersburg")
                .build();
        when(universityRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UniversityResponse> result = universityService.getAll(null);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Moscow State", result.get(0).getName());
        assertEquals("MSU", result.get(0).getShortName());
        assertEquals("Moscow", result.get(0).getCity());
        assertEquals(2L, result.get(1).getId());
        assertEquals("SPbU", result.get(1).getName());
        verify(universityRepository).findAll();
    }

    @Test
    void getById_WhenFound_ReturnsMappedResponse() {
        University entity = University.builder()
                .id(10L)
                .name("Test Uni")
                .shortName("TU")
                .city("Test City")
                .build();
        when(universityRepository.findById(10L)).thenReturn(Optional.of(entity));

        UniversityResponse response = universityService.getById(10L, null);

        assertEquals(10L, response.getId());
        assertEquals("Test Uni", response.getName());
        assertEquals("TU", response.getShortName());
        assertEquals("Test City", response.getCity());
        verify(universityRepository).findById(10L);
    }

    @Test
    void getById_WhenNotFound_ThrowsResourceNotFoundException() {
        when(universityRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> universityService.getById(99L, null));

        assertEquals("University not found with id: 99", ex.getMessage());
        verify(universityRepository).findById(99L);
    }

    @Test
    void create_ByUniversityAdmin_ThrowsAccessDenied() {
        UniversityRequest request = UniversityRequest.builder()
                .name("New University")
                .shortName("NU")
                .city("Novgorod")
                .build();

        assertThrows(AccessDeniedException.class, () -> universityService.create(request, "admin@test.ru"));
        verify(universityRepository, never()).save(any());
    }

    @Test
    void update_WhenFound_UpdatesAllFieldsAndReturnsMappedResponse() {
        University existing = University.builder()
                .id(7L)
                .name("Old Name")
                .shortName("ON")
                .city("Old City")
                .build();
        UniversityRequest request = UniversityRequest.builder()
                .name("Updated Name")
                .shortName("UN")
                .city("Updated City")
                .build();
        when(universityRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(universityRepository.save(existing)).thenReturn(existing);

        UniversityResponse response = universityService.update(7L, request, "admin@test.ru");

        assertEquals("Updated Name", existing.getName());
        assertEquals("UN", existing.getShortName());
        assertEquals("Updated City", existing.getCity());
        verify(universityRepository).save(existing);
        assertEquals(7L, response.getId());
        assertEquals("Updated Name", response.getName());
        assertEquals("UN", response.getShortName());
        assertEquals("Updated City", response.getCity());
    }

    @Test
    void update_WhenNotFound_ThrowsResourceNotFoundException() {
        when(universityRepository.findById(404L)).thenReturn(Optional.empty());
        UniversityRequest request = UniversityRequest.builder()
                .name("X")
                .shortName("Y")
                .city("Z")
                .build();

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> universityService.update(404L, request, "admin@test.ru"));

        assertEquals("University not found with id: 404", ex.getMessage());
        verify(universityRepository, never()).save(any());
    }

    @Test
    void delete_WhenExists_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> universityService.delete(3L, "admin@test.ru"));
        verify(universityRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_WhenNotFound_StillThrowsAccessDeniedForAdmin() {
        assertThrows(AccessDeniedException.class, () -> universityService.delete(88L, "admin@test.ru"));
        verify(universityRepository, never()).deleteById(anyLong());
    }
}
