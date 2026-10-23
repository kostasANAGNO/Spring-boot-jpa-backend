package com.kostasanagno.his.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kostasanagno.his.dto.PatientRequest;
import com.kostasanagno.his.dto.PatientResponse;
import com.kostasanagno.his.dto.PatientSearchCriteria;
import com.kostasanagno.his.entity.Gender;
import com.kostasanagno.his.entity.Patient;
import com.kostasanagno.his.exception.DuplicateResourceException;
import com.kostasanagno.his.exception.ResourceNotFoundException;
import com.kostasanagno.his.mapper.PatientMapper;
import com.kostasanagno.his.repository.PatientRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Spy
    private PatientMapper patientMapper = new com.kostasanagno.his.mapper.PatientMapperImpl();

    @InjectMocks
    private PatientServiceImpl service;

    private PatientRequest request;

    @BeforeEach
    void setUp() {
        request = new PatientRequest("Maria", "Papadopoulou", LocalDate.of(1990, 5, 17), Gender.FEMALE,
                " 17059012345 ", "+30 210 1234567", "maria@example.com", "Athens", "Nikos", "+30 690 1111111");
    }

    @Test
    void createSavesPatientWithTrimmedNationalId() {
        when(patientRepository.existsByNationalId("17059012345")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PatientResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nationalId()).isEqualTo("17059012345");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createRejectsDuplicateNationalId() {
        when(patientRepository.existsByNationalId("17059012345")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(DuplicateResourceException.class);
        verify(patientRepository, never()).save(any());
    }

    @Test
    void getByIdThrowsWhenMissingOrSoftDeleted() {
        when(patientRepository.findByIdAndActiveTrue(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9");
    }

    @Test
    void updateOverwritesFieldsButKeepsActiveState() {
        Patient existing = Patient.builder().firstName("Old").lastName("Name").nationalId("1")
                .gender(Gender.MALE).dateOfBirth(LocalDate.of(1980, 1, 1)).build();
        existing.setId(5L);
        when(patientRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(existing));
        when(patientRepository.existsByNationalIdAndIdNot("17059012345", 5L)).thenReturn(false);
        when(patientRepository.saveAndFlush(existing)).thenReturn(existing);

        PatientResponse response = service.update(5L, request);

        assertThat(response.firstName()).isEqualTo("Maria");
        assertThat(response.nationalId()).isEqualTo("17059012345");
        assertThat(response.active()).isTrue();
        assertThat(response.id()).isEqualTo(5L);
    }

    @Test
    void updateRejectsNationalIdOwnedByAnotherPatient() {
        Patient existing = Patient.builder().nationalId("1").build();
        when(patientRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(existing));
        when(patientRepository.existsByNationalIdAndIdNot("17059012345", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteIsSoft() {
        Patient existing = Patient.builder().nationalId("1").build();
        when(patientRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(existing));

        service.delete(5L);

        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(patientRepository, never()).delete(any(Patient.class));
        verify(patientRepository, never()).deleteById(any());
    }

    @Test
    void searchMapsPageToResponses() {
        Patient patient = Patient.builder().firstName("A").lastName("B").nationalId("1").build();
        Pageable pageable = PageRequest.of(0, 10);
        when(patientRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(patient), pageable, 1));

        Page<PatientResponse> page = service.search(new PatientSearchCriteria("A", null, null, null, null), pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).firstName()).isEqualTo("A");
    }
}
