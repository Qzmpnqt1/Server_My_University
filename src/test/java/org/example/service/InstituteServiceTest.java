package org.example.service;

import org.example.dto.request.InstituteRequest;
import org.example.dto.response.InstituteResponse;
import org.example.exception.ResourceNotFoundException;
import org.example.model.Institute;
import org.example.model.University;
import org.example.repository.InstituteRepository;
import org.example.repository.UniversityRepository;
import org.example.repository.UsersRepository;
import org.example.service.UniversityScopeService;
import org.example.service.impl.InstituteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class InstituteServiceTest {

    @Mock
    private InstituteRepository instituteRepository;

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private UniversityScopeService universityScopeService;

    @InjectMocks
    private InstituteServiceImpl instituteService;

    @BeforeEach
    void scopeStubs() {
        lenient().when(universityScopeService.requireAdminUniversityId(anyString())).thenReturn(1L);
        lenient().doNothing().when(universityScopeService).assertUniversityMatches(anyLong(), anyLong());
        lenient().doNothing().when(universityScopeService).assertInstituteInUniversity(anyLong(), anyLong());
    }

    private static University university(Long id, String name) {
        return University.builder()
                .id(id)
                .name(name)
                .shortName("U" + id)
                .city("City")
                .build();
    }

    @Test
    void getAll_WhenUniversityIdNull_ReturnsAll() {
        Institute i1 = Institute.builder()
                .id(1L)
                .name("Inst A")
                .shortName("IA")
                .university(university(10L, "Uni Ten"))
                .build();
        when(instituteRepository.findAll()).thenReturn(List.of(i1));

        List<InstituteResponse> result = instituteService.getAll(null, null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(10L, result.get(0).getUniversityId());
        assertEquals("Uni Ten", result.get(0).getUniversityName());
        verify(instituteRepository).findAll();
        verify(instituteRepository, never()).findByUniversityId(anyLong());
    }

    @Test
    void getAll_WhenUniversityIdProvided_ReturnsFiltered() {
        Institute i = Institute.builder()
                .id(2L)
                .name("Filtered")
                .shortName("F")
                .university(university(20L, "Target Uni"))
                .build();
        when(instituteRepository.findByUniversityId(20L)).thenReturn(List.of(i));

        List<InstituteResponse> result = instituteService.getAll(20L, null);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals(20L, result.get(0).getUniversityId());
        assertEquals("Target Uni", result.get(0).getUniversityName());
        verify(instituteRepository).findByUniversityId(20L);
        verify(instituteRepository, never()).findAll();
    }

    @Test
    void getById_WhenFound_ReturnsMappedResponse() {
        Institute entity = Institute.builder()
                .id(30L)
                .name("Physics")
                .shortName("PHY")
                .university(university(100L, "Big University"))
                .build();
        when(instituteRepository.findById(30L)).thenReturn(Optional.of(entity));

        InstituteResponse response = instituteService.getById(30L, null);

        assertEquals(30L, response.getId());
        assertEquals("Physics", response.getName());
        assertEquals("PHY", response.getShortName());
        assertEquals(100L, response.getUniversityId());
        assertEquals("Big University", response.getUniversityName());
    }

    @Test
    void getById_WhenNotFound_ThrowsResourceNotFoundException() {
        when(instituteRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> instituteService.getById(999L, null));

        assertEquals("Institute not found with id: 999", ex.getMessage());
    }

    @Test
    void create_WhenUniversityExists_SavesAndReturnsMappedResponse() {
        University uni = university(5L, "Parent Uni");
        when(universityRepository.findById(5L)).thenReturn(Optional.of(uni));
        InstituteRequest request = InstituteRequest.builder()
                .name("Math Institute")
                .shortName("MATH")
                .universityId(5L)
                .build();
        Institute saved = Institute.builder()
                .id(40L)
                .name("Math Institute")
                .shortName("MATH")
                .university(uni)
                .build();
        when(instituteRepository.save(any(Institute.class))).thenReturn(saved);

        InstituteResponse response = instituteService.create(request, "admin@test.ru");

        ArgumentCaptor<Institute> captor = ArgumentCaptor.forClass(Institute.class);
        verify(instituteRepository).save(captor.capture());
        Institute passed = captor.getValue();
        assertEquals("Math Institute", passed.getName());
        assertEquals("MATH", passed.getShortName());
        assertSame(uni, passed.getUniversity());

        assertEquals(40L, response.getId());
        assertEquals("Math Institute", response.getName());
        assertEquals(5L, response.getUniversityId());
        assertEquals("Parent Uni", response.getUniversityName());
    }

    @Test
    void create_WhenUniversityMissing_ThrowsResourceNotFoundException() {
        when(universityRepository.findById(77L)).thenReturn(Optional.empty());
        InstituteRequest request = InstituteRequest.builder()
                .name("Orphan")
                .shortName("O")
                .universityId(77L)
                .build();

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> instituteService.create(request, "admin@test.ru"));

        assertEquals("University not found with id: 77", ex.getMessage());
        verify(instituteRepository, never()).save(any());
    }

    @Test
    void update_WhenFound_UpdatesAllFieldsAndReturnsMappedResponse() {
        University oldUni = university(1L, "Old Uni");
        University newUni = university(2L, "New Uni");
        Institute existing = Institute.builder()
                .id(50L)
                .name("Old Institute")
                .shortName("OI")
                .university(oldUni)
                .build();
        InstituteRequest request = InstituteRequest.builder()
                .name("New Institute")
                .shortName("NI")
                .universityId(2L)
                .build();
        when(instituteRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(universityRepository.findById(2L)).thenReturn(Optional.of(newUni));
        when(instituteRepository.save(existing)).thenReturn(existing);

        InstituteResponse response = instituteService.update(50L, request, "admin@test.ru");

        assertEquals("New Institute", existing.getName());
        assertEquals("NI", existing.getShortName());
        assertSame(newUni, existing.getUniversity());
        verify(instituteRepository).save(existing);
        assertEquals(50L, response.getId());
        assertEquals("New Institute", response.getName());
        assertEquals("NI", response.getShortName());
        assertEquals(2L, response.getUniversityId());
        assertEquals("New Uni", response.getUniversityName());
    }

    @Test
    void delete_WhenExists_DeletesById() {
        when(instituteRepository.existsById(60L)).thenReturn(true);

        instituteService.delete(60L, "admin@test.ru");

        verify(instituteRepository).existsById(60L);
        verify(instituteRepository).deleteById(60L);
    }

    @Test
    void delete_WhenNotFound_ThrowsResourceNotFoundException() {
        when(instituteRepository.existsById(61L)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> instituteService.delete(61L, "admin@test.ru"));

        assertEquals("Institute not found with id: 61", ex.getMessage());
        verify(instituteRepository, never()).deleteById(anyLong());
    }
}
