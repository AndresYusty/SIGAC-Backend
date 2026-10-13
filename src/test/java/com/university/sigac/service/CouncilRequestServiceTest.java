package com.university.sigac.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.university.sigac.config.AppProperties;
import com.university.sigac.dto.CouncilRequestDtos.CouncilRequestForm;
import com.university.sigac.enums.RequestType;
import com.university.sigac.exception.ApiException;
import com.university.sigac.repository.CouncilRequestRepository;
import com.university.sigac.repository.FacultyRepository;
import com.university.sigac.repository.ProgramRepository;
import com.university.sigac.repository.RequestHistoryRepository;
import com.university.sigac.repository.UserRepository;
import com.university.sigac.security.AuthenticatedUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Mockito unit test: validates the attachment rules (RN-08) without touching the database. */
class CouncilRequestServiceTest {

    private CouncilRequestService service;
    private ParameterService parameters;

    @BeforeEach
    void setUp() {
        parameters = mock(ParameterService.class);
        when(parameters.getInt(ParameterService.ATTACHMENTS_MAX_MB, 10)).thenReturn(1);
        service = new CouncilRequestService(mock(CouncilRequestRepository.class), mock(RequestHistoryRepository.class),
                mock(UserRepository.class), mock(FacultyRepository.class), mock(ProgramRepository.class), parameters,
                mock(StorageService.class), mock(SequenceService.class), mock(AuditService.class),
                mock(EmailService.class), new AppProperties());
        AuthenticatedUser u = new AuthenticatedUser(1L, "sec2@demo.edu", "Sec 2", List.of("ROLE_SECRETARY_2"), 10L, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CouncilRequestForm form() {
        return new CouncilRequestForm("Title", "Description", "Requester", RequestType.ACADEMIC, null);
    }

    @Test
    void rejectsNonPdfFiles() {
        var file = new MockMultipartFile("attachments", "note.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        ApiException e = assertThrows(ApiException.class, () -> service.file(form(), List.of(file)));
        assertEquals(400, e.getStatus().value());
    }

    @Test
    void rejectsFakePdfByContent() {
        var file = new MockMultipartFile("attachments", "fake.pdf", "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8));
        assertThrows(ApiException.class, () -> service.file(form(), List.of(file)));
    }

    @Test
    void rejectsFilesLargerThanTheLimit() {
        byte[] large = new byte[2 * 1024 * 1024];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.ISO_8859_1), 0, large, 0, 5);
        var file = new MockMultipartFile("attachments", "large.pdf", "application/pdf", large);
        ApiException e = assertThrows(ApiException.class, () -> service.file(form(), List.of(file)));
        assertEquals(413, e.getStatus().value());
        verify(parameters).getInt(ParameterService.ATTACHMENTS_MAX_MB, 10);
    }
}
