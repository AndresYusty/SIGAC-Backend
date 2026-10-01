package com.universidad.sigac.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.universidad.sigac.config.AppProperties;
import com.universidad.sigac.dto.SolicitudDtos.SolicitudRequest;
import com.universidad.sigac.enums.TipoSolicitud;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.FacultadRepository;
import com.universidad.sigac.repository.HistorialSolicitudRepository;
import com.universidad.sigac.repository.ProgramaRepository;
import com.universidad.sigac.repository.SolicitudRepository;
import com.universidad.sigac.repository.UsuarioRepository;
import com.universidad.sigac.security.UsuarioAutenticado;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Prueba unitaria con Mockito: valida las reglas de los anexos (RN-08) sin tocar la base de datos. */
class SolicitudServiceTest {

    private SolicitudService service;
    private ParametroService parametros;

    @BeforeEach
    void preparar() {
        parametros = mock(ParametroService.class);
        when(parametros.getInt(ParametroService.ARCHIVOS_MAX_MB, 10)).thenReturn(1);
        service = new SolicitudService(mock(SolicitudRepository.class), mock(HistorialSolicitudRepository.class),
                mock(UsuarioRepository.class), mock(FacultadRepository.class), mock(ProgramaRepository.class), parametros,
                mock(StorageService.class), mock(ConsecutivoService.class), mock(AuditService.class),
                mock(EmailService.class), new AppProperties());
        UsuarioAutenticado u = new UsuarioAutenticado(1L, "sec2@demo.edu", "Sec 2", List.of("ROLE_SECRETARIA_2"), 10L, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private SolicitudRequest solicitud() {
        return new SolicitudRequest("Titulo", "Descripcion", "Solicitante", TipoSolicitud.ACADEMICA, null);
    }

    @Test
    void rechazaArchivosQueNoSonPdf() {
        var archivo = new MockMultipartFile("anexos", "nota.txt", "text/plain", "hola".getBytes(StandardCharsets.UTF_8));
        ApiException e = assertThrows(ApiException.class, () -> service.radicar(solicitud(), List.of(archivo)));
        assertEquals(400, e.getStatus().value());
    }

    @Test
    void rechazaPdfFalsoPorContenido() {
        var archivo = new MockMultipartFile("anexos", "falso.pdf", "application/pdf", "no soy pdf".getBytes(StandardCharsets.UTF_8));
        assertThrows(ApiException.class, () -> service.radicar(solicitud(), List.of(archivo)));
    }

    @Test
    void rechazaArchivosMayoresAlLimite() {
        byte[] grande = new byte[2 * 1024 * 1024];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.ISO_8859_1), 0, grande, 0, 5);
        var archivo = new MockMultipartFile("anexos", "grande.pdf", "application/pdf", grande);
        ApiException e = assertThrows(ApiException.class, () -> service.radicar(solicitud(), List.of(archivo)));
        assertEquals(413, e.getStatus().value());
        verify(parametros).getInt(ParametroService.ARCHIVOS_MAX_MB, 10);
    }
}
