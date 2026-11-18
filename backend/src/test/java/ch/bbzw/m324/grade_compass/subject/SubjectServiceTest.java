package ch.bbzw.m324.grade_compass.subject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository repository;

    @InjectMocks
    private SubjectService service;

    @Test
    void create_savesSubjectAndReturnsResponse() {
        when(repository.existsByNameIgnoreCase("Mathe")).thenReturn(false);
        when(repository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = service.create(new CreateSubjectRequest("Mathe"));

        assertThat(response.name()).isEqualTo("Mathe");
        verify(repository).save(any(Subject.class));
    }

    @Test
    void create_trimsNameBeforeCheckingAndSaving() {
        when(repository.existsByNameIgnoreCase("Mathe")).thenReturn(false);
        when(repository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = service.create(new CreateSubjectRequest("  Mathe  "));

        ArgumentCaptor<Subject> captor = ArgumentCaptor.forClass(Subject.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Mathe");
        assertThat(response.name()).isEqualTo("Mathe");
        verify(repository).existsByNameIgnoreCase("Mathe");
    }

    @Test
    void create_duplicateName_throwsConflictAndDoesNotSave() {
        when(repository.existsByNameIgnoreCase("Mathe")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateSubjectRequest("Mathe")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Mathe");

        verify(repository, never()).save(any());
    }

    @Test
    void create_duplicateDetectedAfterTrimming_throwsConflict() {
        when(repository.existsByNameIgnoreCase("Mathe")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateSubjectRequest("  Mathe ")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(repository, never()).save(any());
    }

    @Test
    void findAll_mapsEntitiesToResponses() {
        when(repository.findAll()).thenReturn(List.of(new Subject("Mathe"), new Subject("Deutsch")));

        List<SubjectResponse> result = service.findAll();

        assertThat(result).extracting(SubjectResponse::name).containsExactly("Mathe", "Deutsch");
    }

    @Test
    void findAll_returnsEmptyListWhenNoSubjectsExist() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void delete_existingId_deletesSubject() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_unknownId_throwsNotFoundAndDoesNotDelete() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                .hasMessageContaining("99");

        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void update_changesNameAndTrimsWhitespace() {
        Long id = 1L;
        Subject subject = new Subject("Mathe");

        when(repository.findById(id)).thenReturn(Optional.of(subject));
        when(repository.existsByNameIgnoreCaseAndIdNot("Mathematik", id)).thenReturn(false);
        when(repository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = service.update(id, new CreateSubjectRequest("  Mathematik  "));

        assertThat(response.name()).isEqualTo("Mathematik");
        assertThat(subject.getName()).isEqualTo("Mathematik");

        verify(repository).save(subject);
    }

    @Test
    void update_subjectNotFound_throwsNotFoundAndDoesNotSave() {
        Long id = 99L;

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new CreateSubjectRequest("Mathematik")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                .hasMessageContaining("Subject not found");

        verify(repository, never()).save(any(Subject.class));
    }

    @Test
    void update_duplicateName_throwsConflictAndDoesNotSave() {
        Long id = 1L;
        Subject subject = new Subject("Mathe");

        when(repository.findById(id)).thenReturn(Optional.of(subject));
        when(repository.existsByNameIgnoreCaseAndIdNot("Deutsch", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, new CreateSubjectRequest("Deutsch")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Deutsch");

        verify(repository, never()).save(any(Subject.class));
    }
}
